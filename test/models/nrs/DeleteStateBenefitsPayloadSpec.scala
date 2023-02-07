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

package models.nrs

import play.api.libs.json.Json
import support.UnitTest

import java.time.LocalDate

class DeleteStateBenefitsPayloadSpec extends UnitTest {

  ".writes" should {
    "render correct Json string" in {
      val payload = DeleteStateBenefitsPayload(
        benefitType = "jobSeekersAllowance",
        stateBenefitData = StateBenefitData(
          LocalDate.of(2021, 1, 1),
          Some(LocalDate.of(2022, 3, 1)),
          Some(BigDecimal(123.00)),
          Some(BigDecimal(23.00))
        )
      )

      val value1 = Json.toJson(payload)

      println(s"value1 = $value1")
    }
  }

}
