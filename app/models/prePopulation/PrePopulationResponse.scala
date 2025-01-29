/*
 * Copyright 2025 HM Revenue & Customs
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

package models.prePopulation

import play.api.libs.json.{Json, Writes}

case class PrePopulationResponse(hasEsaPrePop: Boolean,
                                 hasJsaPrePop: Boolean,
                                 hasPensionsPrePop: Boolean,
                                 hasPensionLumpSumsPrePop: Boolean)

object PrePopulationResponse {
  implicit val writes: Writes[PrePopulationResponse] = Json.writes[PrePopulationResponse]

  def fromData(customerData: Option[CustomerPrePopulationDataWrapper],
               hmrcData: Option[HmrcHeldPrePopulationDataWrapper]): PrePopulationResponse = {
    val (esaCustomer, jsaCustomer, penCustomer, lumCustomer) = PrePopulationDataWrapper.isPrePopulated(customerData)
    val (esaHmrc, jsaHmrc, penHmrc, lumHmrc) = PrePopulationDataWrapper.isPrePopulated(hmrcData)

    PrePopulationResponse(
      hasEsaPrePop = esaCustomer || esaHmrc,
      hasJsaPrePop = jsaCustomer || jsaHmrc,
      hasPensionsPrePop = penCustomer || penHmrc,
      hasPensionLumpSumsPrePop = lumCustomer || lumHmrc
    )
  }

  val noPrePop: PrePopulationResponse = PrePopulationResponse(
    hasEsaPrePop = false,
    hasJsaPrePop = false,
    hasPensionsPrePop = false,
    hasPensionLumpSumsPrePop = false
  )
}
