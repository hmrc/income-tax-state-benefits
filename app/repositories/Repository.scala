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

import models.errors.{EncryptionDecryptionError, ServiceError}
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Filters.{and, equal}
import uk.gov.hmrc.mongo.play.json.Codecs.toBson
import utils.PagerDutyHelper.PagerDutyKeys.ENCRYPTION_DECRYPTION_ERROR
import utils.PagerDutyHelper.pagerDutyLog

import java.util.UUID

trait Repository {

  def filter(nino: String, sessionDataId: UUID): Bson = and(
    equal("sessionDataId", toBson(Some(sessionDataId))),
    equal("nino", toBson(nino))
  )

  def sessionIdFilter(sessionId: String): Bson = and(
    equal("sessionId", toBson(sessionId))
  )

  def handleEncryptionDecryptionException[T](exception: Exception, startOfMessage: String): Left[ServiceError, T] = {
    pagerDutyLog(ENCRYPTION_DECRYPTION_ERROR, s"$startOfMessage ${exception.getMessage}")
    Left(EncryptionDecryptionError(exception.getMessage))
  }
}

object Repository extends Repository
