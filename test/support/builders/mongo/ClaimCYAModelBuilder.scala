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

package support.builders.mongo

import models.mongo.ClaimCYAModel
import play.api.libs.json.{JsObject, Json}
import support.utils.TaxYearUtils.taxYearEOY

import java.time.{Instant, LocalDate}
import java.util.UUID

object ClaimCYAModelBuilder {

  val aClaimCYAModel: ClaimCYAModel = ClaimCYAModel(
    benefitId = Some(UUID.fromString("e80d4871-ede8-4b81-93b1-b73ad4fbfd42")),
    startDate = LocalDate.parse(s"${taxYearEOY - 1}-04-23"),
    endDateQuestion = Some(true),
    endDate = Some(LocalDate.parse(s"$taxYearEOY-08-13")),
    dateIgnored = Some(Instant.parse(s"${taxYearEOY - 1}-07-08T05:23:00Z")),
    submittedOn = Some(Instant.parse(s"$taxYearEOY-03-13T19:23:00Z")),
    amount = Some(300.00),
    taxPaidQuestion = Some(true),
    taxPaid = Some(400.00),
    isHmrcData = true
  )

  val aClaimCYAModelJson: JsObject = Json.obj(
    "benefitId" -> aClaimCYAModel.benefitId,
    "startDate" -> aClaimCYAModel.startDate,
    "endDateQuestion" -> aClaimCYAModel.endDateQuestion,
    "endDate" -> aClaimCYAModel.endDate.get,
    "dateIgnored" -> aClaimCYAModel.dateIgnored,
    "submittedOn" -> aClaimCYAModel.submittedOn,
    "amount" -> aClaimCYAModel.amount,
    "taxPaidQuestion" -> aClaimCYAModel.taxPaidQuestion,
    "taxPaid" -> aClaimCYAModel.taxPaid,
    "isHmrcData" -> aClaimCYAModel.isHmrcData
  )
}
