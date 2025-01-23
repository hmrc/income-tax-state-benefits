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

package services

import models.api.{AllStateBenefitsData, CustomerAddedStateBenefitsData, StateBenefit, StateBenefitsData}
import models.errors.{ApiServiceError, ServiceError}
import models.prePopulation.PrePopulationResponse
import support.UnitTest
import support.builders.api.CustomerAddedStateBenefitsDataBuilder.aCustomerAddedStateBenefitsData
import support.builders.api.StateBenefitsDataBuilder.aStateBenefitsData
import support.mocks.MockStateBenefitsService
import uk.gov.hmrc.http.HeaderCarrier

import java.time.{Instant, LocalDate}
import java.util.UUID
import scala.concurrent.ExecutionContext

class PrePopulationServiceSpec extends UnitTest
  with MockStateBenefitsService {

  trait Test {
    val taxYear: Int = 2024
    val nino: String = "AA111111A"
    val service: PrePopulationService = new PrePopulationService(
      service = mockStateBenefitsService
    )

    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global

    val dummyData: PrePopulationResponse = PrePopulationResponse(hasEsaPrePop = true, hasJsaPrePop = true)
  }

  "get" when {
    "call to retrieve state benefits data fails with a non-404 status code" should {
      "return an error" in new Test {
        mockGetAllStateBenefitsData(taxYear = taxYear, nino = nino, result = Left(ApiServiceError("500")))
        val result: Either[ServiceError, PrePopulationResponse] = await(service.get(taxYear, nino).value)

        result shouldBe a[Left[_, _]]
        result.swap.getOrElse(ApiServiceError("")).message should include("500")
      }
    }

    "call to retrieve state benefits data fails with a 404 status" should {
      "return a 'no pre-pop' response" in new Test {
        mockGetAllStateBenefitsData(taxYear = taxYear, nino = nino, result = Left(ApiServiceError("404")))
        val result: Either[ServiceError, PrePopulationResponse] = await(service.get(taxYear, nino).value)

        result shouldBe a[Right[_, _]]
        result.getOrElse(dummyData) shouldBe PrePopulationResponse.noPrePop
      }
    }

    "call to retrieve state benefits data succeeds, but the response contains no benefits data" should {
      "return a 'no pre-pop' response" in new Test {
        mockGetAllStateBenefitsData(taxYear = taxYear, nino = nino, result = Right(None))
        val result: Either[ServiceError, PrePopulationResponse] = await(service.get(taxYear, nino).value)

        result shouldBe a[Right[_, _]]
        result.getOrElse(dummyData) shouldBe PrePopulationResponse.noPrePop
      }
    }

    "call to retrieve state benefits data succeeds, but the response contains no ESA or JSA data" should {
      "return a 'no pre-pop' response" in new Test {
        val emptyIfResponse: AllStateBenefitsData = AllStateBenefitsData(
          stateBenefitsData = Some(StateBenefitsData()),
          customerAddedStateBenefitsData = Some(CustomerAddedStateBenefitsData())
        )

        mockGetAllStateBenefitsData(taxYear = taxYear, nino = nino, result = Right(Some(emptyIfResponse)))
        val result: Either[ServiceError, PrePopulationResponse] = await(service.get(taxYear, nino).value)

        result shouldBe a[Right[_, _]]
        result.getOrElse(dummyData) shouldBe PrePopulationResponse.noPrePop
      }
    }

    "call to retrieve state benefits data succeeds, and the response contains ESA or JSA data" should {
      "return pre-pop flags as 'true' when customer data exists" in new Test {
        val customerOnlyIfResponse: AllStateBenefitsData = AllStateBenefitsData(
          stateBenefitsData = None,
          customerAddedStateBenefitsData = Some(aCustomerAddedStateBenefitsData)
        )

        mockGetAllStateBenefitsData(taxYear = taxYear, nino = nino, result = Right(Some(customerOnlyIfResponse)))
        val result: Either[ServiceError, PrePopulationResponse] = await(service.get(taxYear, nino).value)

        result shouldBe a[Right[_, _]]
        result.getOrElse(PrePopulationResponse.noPrePop) shouldBe PrePopulationResponse(hasEsaPrePop = true, hasJsaPrePop = true)
      }

      "return pre-pop flags as 'true' when non-ignored HMRC-Held data exists" in new Test {
        val aStateBenefit: StateBenefit = StateBenefit(
          benefitId = UUID.fromString("a1e8057e-fbbc-47a8-a8b4-78d9f015c936"),
          startDate = LocalDate.parse(s"${taxYear - 1}-04-23"),
          endDate = Some(LocalDate.parse(s"$taxYear-08-13")),
          dateIgnored = None,
          submittedOn = Some(Instant.parse(s"$taxYear-03-13T19:23:00Z")),
          amount = Some(300.00),
          taxPaid = Some(400.00)
        )

        val aStateBenefitsData: StateBenefitsData = StateBenefitsData(
          employmentSupportAllowances = Some(Set(aStateBenefit)),
          jobSeekersAllowances = Some(Set(aStateBenefit))
        )

        val hmrcHeldOnlyIfResponse: AllStateBenefitsData = AllStateBenefitsData(
          stateBenefitsData = Some(aStateBenefitsData),
          customerAddedStateBenefitsData = None
        )

        mockGetAllStateBenefitsData(taxYear = taxYear, nino = nino, result = Right(Some(hmrcHeldOnlyIfResponse)))
        val result: Either[ServiceError, PrePopulationResponse] = await(service.get(taxYear, nino).value)

        result shouldBe a[Right[_, _]]
        result.getOrElse(PrePopulationResponse.noPrePop) shouldBe PrePopulationResponse(hasEsaPrePop = true, hasJsaPrePop = true)
      }

      "return pre-pop flags as 'false' when only ignored HMRC-held data exists" in new Test {
        val hmrcHeldOnlyIfResponse: AllStateBenefitsData = AllStateBenefitsData(
          stateBenefitsData = Some(aStateBenefitsData),
          customerAddedStateBenefitsData = None
        )

        mockGetAllStateBenefitsData(taxYear = taxYear, nino = nino, result = Right(Some(hmrcHeldOnlyIfResponse)))
        val result: Either[ServiceError, PrePopulationResponse] = await(service.get(taxYear, nino).value)

        result shouldBe a[Right[_, _]]
        result.getOrElse(dummyData) shouldBe PrePopulationResponse.noPrePop
      }
    }
  }

}
