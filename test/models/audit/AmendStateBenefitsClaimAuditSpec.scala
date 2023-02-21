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

package models.audit

import play.api.libs.json.Json
import support.UnitTest
import support.utils.TaxYearUtils.taxYearEOY

import java.time.{Instant, LocalDate}

class AmendStateBenefitsClaimAuditSpec extends UnitTest {

  ".writes" should {
    "render correct Json string" in {
      val auditEvent = AmendStateBenefitsClaimAudit(
        taxYear = 2022,
        userType = "Individual",
        nino = "AA123456A",
        mtdItId = "1234567890",
        benefitType = "jobSeekersAllowance",
        originalClaimDetails = ClaimDetails(
          startDate = LocalDate.of(2021, 1, 1),
          endDate = Some(LocalDate.of(2022, 4, 1)),
          submittedOn = Some(Instant.parse(s"$taxYearEOY-03-13T19:23:00Z")),
          amount = Some(BigDecimal(123.00)),
          taxPaid = Some(BigDecimal(100.00))
        ),
        updatedClaimDetails = ClaimDetails(
          startDate = LocalDate.of(2021, 1, 1),
          endDate = Some(LocalDate.of(2022, 4, 1)),
          submittedOn = Some(Instant.parse(s"$taxYearEOY-03-13T19:23:00Z")),
          amount = Some(BigDecimal(150.00)),
          taxPaid = Some(BigDecimal(23.00))
        )
      )

      val value1 = Json.toJson(auditEvent)

      println(s"value1 = $value1")
    }
  }

}
