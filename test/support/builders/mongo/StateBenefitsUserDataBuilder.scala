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

package support.builders.mongo

import models.api.BenefitType
import models.mongo.StateBenefitsUserData
import play.api.libs.json.{Format, JsObject, Json}
import support.builders.UserBuilder.aUser
import support.builders.mongo.ClaimCYAModelBuilder.{aClaimCYAModel, aClaimCYAModelJson}
import support.utils.TaxYearUtils.taxYear
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.LocalDateTime
import java.util.UUID

object StateBenefitsUserDataBuilder {

  val aStateBenefitsUserData: StateBenefitsUserData = StateBenefitsUserData(
    benefitType = BenefitType.JobSeekersAllowance.typeName,
    sessionDataId = Some(UUID.fromString("558238ef-d2ff-4839-bd6d-307324d6fe37")),
    sessionId = "sessionId-eb3158c2-0aff-4ce8-8d1b-f2208ace52fe",
    mtdItId = aUser.mtditid,
    nino = "AA123456A",
    taxYear = taxYear,
    isPriorSubmission = false,
    claim = Some(aClaimCYAModel)
  )

  implicit val mongoLocalDateTimeFormats: Format[LocalDateTime] = MongoJavatimeFormats.localDateTimeFormat

  val aStateBenefitsUserDataJson: JsObject = Json.obj(
    "benefitType" -> aStateBenefitsUserData.benefitType,
    "sessionDataId" -> aStateBenefitsUserData.sessionDataId,
    "sessionId" -> aStateBenefitsUserData.sessionId,
    "mtdItId" -> aStateBenefitsUserData.mtdItId,
    "nino" -> aStateBenefitsUserData.nino,
    "taxYear" -> aStateBenefitsUserData.taxYear,
    "isPriorSubmission" -> aStateBenefitsUserData.isPriorSubmission,
    "claim" -> aClaimCYAModelJson,
    "lastUpdated" -> Json.toJson(aStateBenefitsUserData.lastUpdated)
  )
}
