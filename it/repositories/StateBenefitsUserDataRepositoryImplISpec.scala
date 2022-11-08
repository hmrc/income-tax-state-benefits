/*
 * Copyright 2022 HM Revenue & Customs
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

import models.encryption.TextAndKey
import models.errors.{DataNotFoundError, EncryptionDecryptionError}
import models.mongo.{EncryptedStateBenefitsUserData, StateBenefitsUserData}
import org.mongodb.scala.MongoWriteException
import org.mongodb.scala.model.Indexes.ascending
import org.mongodb.scala.model.{IndexModel, IndexOptions}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.inject.guice.GuiceApplicationBuilder
import support.IntegrationTest
import support.builders.mongo.ClaimCYAModelBuilder.aClaimCYAModel
import support.builders.mongo.StateBenefitsUserDataBuilder.aStateBenefitsUserData
import uk.gov.hmrc.mongo.MongoUtils
import utils.SecureGCMCipher

import java.time.{LocalDateTime, ZoneOffset}
import java.util.UUID
import scala.concurrent.Future

class StateBenefitsUserDataRepositoryImplISpec extends IntegrationTest {

  protected implicit val secureGCMCipher: SecureGCMCipher = app.injector.instanceOf[SecureGCMCipher]

  private val nino = "AA123456A"

  private val repoWithInvalidEncryption = GuiceApplicationBuilder().configure(config + ("mongodb.encryption.key" -> "key")).build()
    .injector.instanceOf[StateBenefitsUserDataRepositoryImpl]

  private def count(): Long = await(underTest.collection.countDocuments().toFuture())

  private val underTest: StateBenefitsUserDataRepositoryImpl = app.injector.instanceOf[StateBenefitsUserDataRepositoryImpl]

  class EmptyDatabase {
    await(underTest.collection.drop().toFuture())
    await(underTest.ensureIndexes)
    count() shouldBe 0
  }

  "createOrUpdate with invalid encryption" should {
    "fail to add data" in new EmptyDatabase {
      await(repoWithInvalidEncryption.createOrUpdate(aStateBenefitsUserData)) shouldBe Left(EncryptionDecryptionError(
        "Key being used is not valid. It could be due to invalid encoding, wrong length or uninitialized for encrypt Invalid AES key length: 2 bytes"
      ))
    }
  }

  "find with invalid encryption" should {
    "fail to find data" in new EmptyDatabase {
      implicit val textAndKey: TextAndKey = TextAndKey(aStateBenefitsUserData.mtdItId, appConfig.encryptionKey)
      count() shouldBe 0
      await(repoWithInvalidEncryption.collection.insertOne(aStateBenefitsUserData.encrypted).toFuture())
      count() shouldBe 1
      await(repoWithInvalidEncryption.find(nino, aStateBenefitsUserData.sessionDataId.get)) shouldBe Left(EncryptionDecryptionError(
        "Key being used is not valid. It could be due to invalid encoding, wrong length or uninitialized for decrypt Invalid AES key length: 2 bytes"
      ))
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
      count shouldBe 0

      await(underTest.createOrUpdate(aStateBenefitsUserData)) shouldBe Right(aStateBenefitsUserData.sessionDataId.get)
      count shouldBe 1

      private val errorOrUuid = await(underTest.createOrUpdate(aStateBenefitsUserData.copy(sessionDataId = Some(UUID.randomUUID()))))

      errorOrUuid.left.get.message must include("Command failed with error 11000 (DuplicateKey)")
      count shouldBe 1
    }

    "create a document in collection when one does not exist" in new EmptyDatabase {
      count shouldBe 0
      await(underTest.createOrUpdate(aStateBenefitsUserData.copy(sessionDataId = None)))
      count shouldBe 1
    }

    "update a document in collection when one already exists" in new EmptyDatabase {
      await(underTest.createOrUpdate(aStateBenefitsUserData.copy(claim = Some(aClaimCYAModel)))) shouldBe Right(aStateBenefitsUserData.sessionDataId.get)
      count shouldBe 1

      private val updatedCisUserData = aStateBenefitsUserData.copy(claim = None)

      await(underTest.createOrUpdate(updatedCisUserData)) shouldBe Right(aStateBenefitsUserData.sessionDataId.get)
      count shouldBe 1
      await(underTest.find(nino, updatedCisUserData.sessionDataId.get)).right.get.claim shouldBe None
    }
  }

  "find" should {
    "get a document and update the TTL" in new EmptyDatabase {
      private val now = LocalDateTime.now(ZoneOffset.UTC)
      private val data = aStateBenefitsUserData.copy(lastUpdated = now)

      await(underTest.createOrUpdate(data))
      count shouldBe 1

      private val result = await(underTest.find(nino, data.sessionDataId.get))

      result.right.map(_.copy(lastUpdated = data.lastUpdated)) shouldBe Right(data)
      result.right.get.lastUpdated.isAfter(data.lastUpdated) shouldBe true
    }

    "return DataNotFoundError when find operation did not find data for the given inputs" in new EmptyDatabase {
      await(underTest.find(nino, UUID.randomUUID())) shouldBe Left(DataNotFoundError)
    }
  }

  "the set indexes" should {
    "enforce uniqueness" in new EmptyDatabase {
      private val data: StateBenefitsUserData = aStateBenefitsUserData

      implicit val textAndKey: TextAndKey = TextAndKey(data.mtdItId, appConfig.encryptionKey)
      await(underTest.createOrUpdate(data))
      count shouldBe 1

      private val encryptedUserData: EncryptedStateBenefitsUserData = data.encrypted

      private val caught = intercept[MongoWriteException](await(underTest.collection.insertOne(encryptedUserData).toFuture()))

      caught.getMessage must
        include("E11000 duplicate key error collection: income-tax-state-benefits.stateBenefitsUserData index: UserDataLookupIndex dup key:")
    }
  }
}
