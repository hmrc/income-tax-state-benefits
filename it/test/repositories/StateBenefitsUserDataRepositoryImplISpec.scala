/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package repositories

import com.mongodb.MongoTimeoutException
import models.errors.{DataNotFoundError, EncryptionDecryptionError}
import models.mongo.{EncryptedStateBenefitsUserData, StateBenefitsUserData}
import org.mongodb.scala.model.Indexes.ascending
import org.mongodb.scala.model.{IndexModel, IndexOptions}
import org.mongodb.scala.{MongoException, MongoInternalException, MongoWriteException}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.inject.guice.GuiceApplicationBuilder
import support.IntegrationTest
import support.builders.mongo.ClaimCYAModelBuilder.aClaimCYAModel
import support.builders.mongo.StateBenefitsUserDataBuilder.aStateBenefitsUserData
import uk.gov.hmrc.mongo.MongoUtils
import utils.AesGcmAdCrypto
import utils.PagerDutyHelper.PagerDutyKeys.FAILED_TO_CREATE_UPDATE_STATE_BENEFITS_DATA

import java.time.Instant
import java.util.UUID
import scala.concurrent.Future

class StateBenefitsUserDataRepositoryImplISpec extends IntegrationTest {

  protected implicit val aesGcmAdCrypto: AesGcmAdCrypto = app.injector.instanceOf[AesGcmAdCrypto]

  private val nino = "AA123456A"

  private val repoWithInvalidEncryption = GuiceApplicationBuilder().configure(config + ("mongodb.encryption.key" -> "key")).build()
    .injector.instanceOf[StateBenefitsUserDataRepositoryImpl]

  private val underTest: StateBenefitsUserDataRepositoryImpl = app.injector.instanceOf[StateBenefitsUserDataRepositoryImpl]

  class EmptyDatabase {
    await(underTest.collection.drop().toFuture())
    await(underTest.ensureIndexes())
    await(underTest.collection.countDocuments().toFuture()) shouldBe 0
  }

  "the set indexes" should {
    "enforce uniqueness" in new EmptyDatabase {
      private val data_1: StateBenefitsUserData = aStateBenefitsUserData.copy(sessionId = "session-1")
      private val data_2: StateBenefitsUserData = aStateBenefitsUserData.copy(sessionId = "session-2")

      implicit val associatedText: String = data_1.mtdItId
      await(underTest.createOrUpdate(data_1))
      await(underTest.collection.countDocuments().toFuture()) shouldBe 1

      private val encryptedUserData: EncryptedStateBenefitsUserData = data_2.encrypted

      private val caught = intercept[MongoWriteException](await(underTest.collection.insertOne(encryptedUserData).toFuture()))

      caught.getMessage must
        include("E11000 duplicate key error collection: income-tax-state-benefits.stateBenefitsUserData index: UserDataLookupIndex dup key:")
    }

    "enforce uniqueness for sessionId" in new EmptyDatabase {
      private val data_1: StateBenefitsUserData = aStateBenefitsUserData.copy(sessionId = "session-test")
      private val data_2: StateBenefitsUserData = aStateBenefitsUserData.copy(sessionId = "session-test", nino = "some-nino")

      implicit val associatedText: String = data_1.mtdItId
      await(underTest.createOrUpdate(data_1))
      await(underTest.collection.countDocuments().toFuture()) shouldBe 1

      private val encryptedUserData: EncryptedStateBenefitsUserData = data_2.encrypted

      private val caught = intercept[MongoWriteException](await(underTest.collection.insertOne(encryptedUserData).toFuture()))

      caught.getMessage must
        include("E11000 duplicate key error collection: income-tax-state-benefits.stateBenefitsUserData index: SessionIdIndex dup key:")
    }
  }

  "createOrUpdate with invalid encryption" should {
    "fail to add data" in new EmptyDatabase {
      await(repoWithInvalidEncryption.createOrUpdate(aStateBenefitsUserData)) shouldBe
        Left(EncryptionDecryptionError("Failed encrypting data"))
    }
  }

  "find with invalid encryption" should {
    "fail to find data" in new EmptyDatabase {
      implicit val associatedText: String = aStateBenefitsUserData.mtdItId
      await(underTest.collection.countDocuments().toFuture()) shouldBe 0
      await(repoWithInvalidEncryption.collection.insertOne(aStateBenefitsUserData.encrypted).toFuture())
      await(underTest.collection.countDocuments().toFuture()) shouldBe 1
      await(repoWithInvalidEncryption.find(nino, aStateBenefitsUserData.sessionDataId.get)) shouldBe
        Left(EncryptionDecryptionError("Failed encrypting data"))
    }
  }

  "handleEncryptionDecryptionException" should {
    "handle an exception" in {
      repoWithInvalidEncryption.handleEncryptionDecryptionException(new Exception("fail"), "") shouldBe Left(EncryptionDecryptionError("fail"))
    }
  }

  "createOrUpdate" should {
    "fail to add a document to the collection when a mongo error occurs" in new EmptyDatabase {
      def ensureIndexes: Future[Seq[String]] = {
        val indexes = Seq(IndexModel(ascending("sessionId"), IndexOptions().unique(true).name("fakeIndex")))
        MongoUtils.ensureIndexes(underTest.collection, indexes, replaceIndexes = true)
      }

      await(ensureIndexes)
      await(underTest.collection.countDocuments().toFuture()) shouldBe 0

      await(underTest.createOrUpdate(aStateBenefitsUserData)) shouldBe Right(aStateBenefitsUserData.sessionDataId.get)
      await(underTest.collection.countDocuments().toFuture()) shouldBe 1

      private val errorOrUuid = await(underTest.createOrUpdate(aStateBenefitsUserData.copy(sessionDataId = Some(UUID.randomUUID()))))

      errorOrUuid.swap.map(_.message).getOrElse("") must include("Command failed with error 11000 (DuplicateKey)")
      await(underTest.collection.countDocuments().toFuture()) shouldBe 1
    }

    "create a document in collection when one does not exist" in new EmptyDatabase {
      await(underTest.collection.countDocuments().toFuture()) shouldBe 0
      await(underTest.createOrUpdate(aStateBenefitsUserData.copy(sessionDataId = None)))
      await(underTest.collection.countDocuments().toFuture()) shouldBe 1
    }

    "update a document in collection when one already exists" in new EmptyDatabase {
      await(underTest.createOrUpdate(aStateBenefitsUserData.copy(claim = Some(aClaimCYAModel)))) shouldBe Right(aStateBenefitsUserData.sessionDataId.get)
      await(underTest.collection.countDocuments().toFuture()) shouldBe 1

      private val updatedCisUserData = aStateBenefitsUserData.copy(claim = None)

      await(underTest.createOrUpdate(updatedCisUserData)) shouldBe Right(aStateBenefitsUserData.sessionDataId.get)
      await(underTest.collection.countDocuments().toFuture()) shouldBe 1
      await(underTest.find(nino, updatedCisUserData.sessionDataId.get)).map(_.claim) shouldBe Right(None)
    }
  }

  "find" should {
    "get a document and update the TTL" in new EmptyDatabase {
      private val now = Instant.now()
      private val data = aStateBenefitsUserData.copy(lastUpdated = now)

      await(underTest.createOrUpdate(data))
      await(underTest.collection.countDocuments().toFuture()) shouldBe 1

      private val result = await(underTest.find(nino, data.sessionDataId.get))

      result.map(_.copy(lastUpdated = data.lastUpdated)) shouldBe Right(data)
      result.map(_.lastUpdated.isAfter(data.lastUpdated)) shouldBe Right(true)
    }

    "return DataNotFoundError when find operation did not find data for the given inputs" in new EmptyDatabase {
      await(underTest.find(nino, UUID.randomUUID())) shouldBe Left(DataNotFoundError)
    }
  }

  "mongoRecover" should {
    Seq(new MongoTimeoutException(""), new MongoInternalException(""), new MongoException("")).foreach { exception =>
      s"recover when the exception is a MongoException or a subclass of MongoException - ${exception.getClass.getSimpleName}" in {
        val result = Future.failed(exception)
          .recover(underTest.mongoRecover[Int]("CreateOrUpdate", FAILED_TO_CREATE_UPDATE_STATE_BENEFITS_DATA, "some-session-id"))

        await(result) mustBe None
      }
    }

    Seq(new NullPointerException(""), new RuntimeException("")).foreach { exception =>
      s"not recover when the exception is not a subclass of MongoException - ${exception.getClass.getSimpleName}" in {
        val result = Future.failed(exception)
          .recover(underTest.mongoRecover[Int]("CreateOrUpdate", FAILED_TO_CREATE_UPDATE_STATE_BENEFITS_DATA, "some-session-id"))

        assertThrows[RuntimeException] {
          await(result)
        }
      }
    }
  }

  "clear" should {
    "remove a record" in new EmptyDatabase {
      await(underTest.collection.countDocuments().toFuture()) mustBe 0
      await(underTest.createOrUpdate(aStateBenefitsUserData.copy(sessionId = "some-session-id"))) mustBe Right(aStateBenefitsUserData.sessionDataId.get)
      await(underTest.collection.countDocuments().toFuture()) mustBe 1

      await(underTest.clear("some-session-id")) mustBe Right(())
      await(underTest.collection.countDocuments().toFuture()) mustBe 0
    }
  }
}
