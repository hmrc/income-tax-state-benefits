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

import com.google.inject.ImplementedBy
import com.mongodb.client.model.ReturnDocument
import com.mongodb.client.model.Updates.set
import config.AppConfig
import models.encryption.TextAndKey
import models.errors.{DataNotFoundError, DataNotUpdatedError, MongoError, ServiceError}
import models.mongo._
import org.mongodb.scala.MongoException
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.{FindOneAndReplaceOptions, FindOneAndUpdateOptions}
import play.api.Logging
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.Codecs.toBson
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats
import uk.gov.hmrc.play.http.logging.Mdc
import utils.PagerDutyHelper.PagerDutyKeys._
import utils.PagerDutyHelper.{PagerDutyKeys, pagerDutyLog}
import utils.SecureGCMCipher

import java.time.{LocalDateTime, ZoneOffset}
import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

@Singleton
class StateBenefitsUserDataRepositoryImpl @Inject()(mongo: MongoComponent, appConfig: AppConfig)
                                                   (implicit secureGCMCipher: SecureGCMCipher, ec: ExecutionContext)
  extends PlayMongoRepository[EncryptedStateBenefitsUserData](
    mongoComponent = mongo,
    collectionName = "stateBenefitsUserData",
    domainFormat = EncryptedStateBenefitsUserData.format,
    indexes = StateBenefitsUserDataIndexes.indexes(appConfig),
    replaceIndexes = true
  ) with Repository with StateBenefitsUserDataRepository with Logging {

  private lazy val findMessageStart = "[StateBenefitsUserDataRepositoryImpl][find]"
  private lazy val createOrUpdateMessageStart = "[StateBenefitsUserDataRepositoryImpl][create/update]"

  def logOutIndexes(): Unit = {
    Mdc.preservingMdc(collection.listIndexes().toFuture())
      .map(_.foreach(eachIndex => logger.info(s"INDEX_IN_STATE_BENEFITS_USER_DATA $eachIndex")))
  }

  override def createOrUpdate(stateBenefitsUserData: StateBenefitsUserData): Future[Either[ServiceError, UUID]] = {
    val userData = stateBenefitsUserData.sessionDataId.fold(stateBenefitsUserData.copy(sessionDataId = Some(UUID.randomUUID())))(_ => stateBenefitsUserData)
      .copy(lastUpdated = LocalDateTime.now(ZoneOffset.UTC))

    encryptedFrom(userData) match {
      case Left(error: ServiceError) => Future.successful(Left(error))
      case Right(encryptedData) => createOrUpdateFrom(encryptedData)
    }
  }

  override def find(nino: String, sessionDataId: UUID): Future[Either[ServiceError, StateBenefitsUserData]] = {
    findBy(sessionDataId, nino).map {
      case Left(error) => Left(error)
      case Right(encryptedData) => decryptedFrom(encryptedData)
    }
  }

  private def createOrUpdateFrom(encryptedData: EncryptedStateBenefitsUserData): Future[Either[ServiceError, UUID]] = {
    val queryFilter: Bson = filter(encryptedData.nino, encryptedData.sessionDataId.get)
    val options = FindOneAndReplaceOptions().upsert(true).returnDocument(ReturnDocument.AFTER)
    collection.findOneAndReplace(queryFilter, encryptedData, options).toFutureOption().map {
      case Some(data) => Right(data.sessionDataId.get)
      case None =>
        pagerDutyLog(FAILED_TO_CREATE_UPDATE_STATE_BENEFITS_DATA, s"$createOrUpdateMessageStart Failed to update user data.")
        Left(DataNotUpdatedError)
    }.recover {
      case throwable: Throwable =>
        pagerDutyLog(FAILED_TO_CREATE_UPDATE_STATE_BENEFITS_DATA, s"$createOrUpdateMessageStart Failed to update user data. Exception: ${throwable.getMessage}")
        Left(MongoError(throwable.getMessage))
    }
  }

  private def findBy(sessionDataId: UUID, nino: String): Future[Either[ServiceError, EncryptedStateBenefitsUserData]] = {
    val update = set("lastUpdated", toBson(LocalDateTime.now(ZoneOffset.UTC))(MongoJavatimeFormats.localDateTimeFormat))
    val options = FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
    val eventualResult = collection.findOneAndUpdate(filter(nino, sessionDataId), update, options).toFutureOption().map {
      case Some(data) => Right(data)
      case None => Left(DataNotFoundError)
    }

    eventualResult.recover {
      case exception: Exception =>
        pagerDutyLog(FAILED_TO_FIND_STATE_BENEFITS_DATA, s"$findMessageStart Failed to find user data. Exception: ${exception.getMessage}")
        Left(MongoError(exception.getMessage))
    }
  }

  private def decryptedFrom(encryptedData: EncryptedStateBenefitsUserData): Either[ServiceError, StateBenefitsUserData] = {
    implicit lazy val textAndKey: TextAndKey = TextAndKey(encryptedData.mtdItId, appConfig.encryptionKey)
    Try(encryptedData.decrypted).toEither match {
      case Left(throwable: Throwable) => handleEncryptionDecryptionException(throwable.asInstanceOf[Exception], findMessageStart)
      case Right(decryptedData) => Right(decryptedData)
    }
  }

  private def encryptedFrom(userData: StateBenefitsUserData): Either[ServiceError, EncryptedStateBenefitsUserData] = {
    implicit val textAndKey: TextAndKey = TextAndKey(userData.mtdItId, appConfig.encryptionKey)
    Try(userData.encrypted).toEither match {
      case Left(throwable: Throwable) => handleEncryptionDecryptionException(throwable.asInstanceOf[Exception], createOrUpdateMessageStart)
      case Right(encryptedData) => Right(encryptedData)
    }
  }

  override def clear(sessionId: String): Future[Either[ServiceError, Unit]] = {
    val eventualResponse = collection.deleteMany(sessionIdFilter(sessionId))
      .toFutureOption()
      .recover(mongoRecover("Clear", FAILED_TO_CLEAR_STATE_BENEFITS_DATA, sessionId))
      .map(_.exists(_.wasAcknowledged()))

    eventualResponse.map {
      case false => Left(MongoError("FAILED_TO_CLEAR_STATE_BENEFITS_DATA"))
      case true => Right(())
    }
  }

  def mongoRecover[T](operation: String,
                      pagerDutyKey: PagerDutyKeys.Value,
                      sessionId: String): PartialFunction[Throwable, Option[T]] = new PartialFunction[Throwable, Option[T]] {

    override def isDefinedAt(x: Throwable): Boolean = x.isInstanceOf[MongoException]

    override def apply(e: Throwable): Option[T] = {
      pagerDutyLog(
        pagerDutyKey,
        s"[StateBenefitsUserDataRepositoryImpl][$operation] Failed to clear state benefits user data. Error:${e.getMessage}. SessionId: $sessionId"
      )
      None
    }
  }
}

@ImplementedBy(classOf[StateBenefitsUserDataRepositoryImpl])
trait StateBenefitsUserDataRepository {
  def createOrUpdate(userData: StateBenefitsUserData): Future[Either[ServiceError, UUID]]

  def find(nino: String, sessionDataId: UUID): Future[Either[ServiceError, StateBenefitsUserData]]

  def logOutIndexes(): Unit

  def clear(sessionId: String): Future[Either[ServiceError, Unit]]
}
