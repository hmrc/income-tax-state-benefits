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

package models.mongo

import models.encryption.TextAndKey
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json._
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats
import utils.JsonUtils.jsonObjNoNulls
import utils.SecureGCMCipher

import java.time.{LocalDateTime, ZoneOffset}
import java.util.UUID

case class StateBenefitsUserData(sessionDataId: Option[UUID] = None,
                                 sessionId: String,
                                 mtdItId: String,
                                 nino: String,
                                 taxYear: Int,
                                 isPriorSubmission: Boolean,
                                 claim: Option[ClaimCYAModel],
                                 lastUpdated: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC)) {

  lazy val isHmrcData: Boolean = claim.exists(_.isHmrcData)

  def encrypted()(implicit secureGCMCipher: SecureGCMCipher, textAndKey: TextAndKey): EncryptedStateBenefitsUserData = EncryptedStateBenefitsUserData(
    sessionDataId = sessionDataId,
    sessionId = sessionId,
    mtdItId = mtdItId,
    nino = nino,
    taxYear = taxYear,
    isPriorSubmission = isPriorSubmission,
    claim = claim.map(_.encrypted),
    lastUpdated = lastUpdated
  )
}

object StateBenefitsUserData {
  implicit val mongoLocalDateTimeFormats: Format[LocalDateTime] = MongoJavatimeFormats.localDateTimeFormat

  implicit val stateBenefitsUserDataWrites: OWrites[StateBenefitsUserData] = (data: StateBenefitsUserData) => {
    jsonObjNoNulls(
      "sessionDataId" -> data.sessionDataId,
      "sessionId" -> data.sessionId,
      "mtdItId" -> data.mtdItId,
      "nino" -> data.nino,
      "taxYear" -> data.taxYear,
      "isPriorSubmission" -> data.isPriorSubmission,
      "claim" -> data.claim,
      "lastUpdated" -> data.lastUpdated
    )
  }

  implicit val stateBenefitsUserDataReads: Reads[StateBenefitsUserData] = (
    (JsPath \ "sessionDataId").readNullable[UUID] and
      (JsPath \ "sessionId").read[String] and
      (JsPath \ "mtdItId").read[String] and
      (JsPath \ "nino").read[String] and
      (JsPath \ "taxYear").read[Int] and
      (JsPath \ "isPriorSubmission").read[Boolean] and
      (JsPath \ "claim").readNullable[ClaimCYAModel] and
      (JsPath \ "lastUpdated").readWithDefault[LocalDateTime](LocalDateTime.now(ZoneOffset.UTC))
    ) (StateBenefitsUserData.apply _)
}

case class EncryptedStateBenefitsUserData(sessionDataId: Option[UUID] = None,
                                          sessionId: String,
                                          mtdItId: String,
                                          nino: String,
                                          taxYear: Int,
                                          isPriorSubmission: Boolean,
                                          claim: Option[EncryptedClaimCYAModel],
                                          lastUpdated: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC)) {

  def decrypted()(implicit secureGCMCipher: SecureGCMCipher, textAndKey: TextAndKey): StateBenefitsUserData = StateBenefitsUserData(
    sessionDataId = sessionDataId,
    sessionId = sessionId,
    mtdItId = mtdItId,
    nino = nino,
    taxYear = taxYear,
    isPriorSubmission = isPriorSubmission,
    claim = claim.map(_.decrypted),
    lastUpdated = lastUpdated
  )
}

object EncryptedStateBenefitsUserData {
  implicit val mongoLocalDateTimeFormats: Format[LocalDateTime] = MongoJavatimeFormats.localDateTimeFormat
  implicit val format: OFormat[EncryptedStateBenefitsUserData] = Json.format[EncryptedStateBenefitsUserData]
}
