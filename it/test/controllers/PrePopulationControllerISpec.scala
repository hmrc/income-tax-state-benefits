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

package controllers

import models.api.{AllStateBenefitsData, CustomerAddedStateBenefitsData, StateBenefit, StateBenefitsData}
import models.prePopulation.PrePopulationResponse
import play.api.http.Status.{IM_A_TEAPOT, INTERNAL_SERVER_ERROR, NOT_FOUND, OK}
import play.api.libs.json.Json
import play.api.libs.ws.{WSRequest, WSResponse}
import play.api.test.Helpers.AUTHORIZATION
import stubs.AuthStub
import support.builders.api.AllStateBenefitsDataBuilder.anAllStateBenefitsData
import support.builders.api.CustomerAddedStateBenefitsDataBuilder.aCustomerAddedStateBenefitsData
import support.builders.api.StateBenefitsDataBuilder.aStateBenefitsData
import support.stubs.{ControllerIntegrationTest, WireMockStubs}
import uk.gov.hmrc.http.HttpResponse

import java.time.{Instant, LocalDate}
import java.util.UUID

class PrePopulationControllerISpec extends ControllerIntegrationTest
  with WireMockStubs {

  trait Test {
    val nino: String = "AA111111A"
    val taxYear: Int = 2024
    val mtdItId: String = "12345"
    val ifTaxYearParam = s"${(taxYear - 1).toString.takeRight(2)}-${taxYear.toString.takeRight(2)}"

    def request(): WSRequest = {
      AuthStub.authorised(nino, mtdItId)
      buildRequest(s"/income-tax-state-benefits/pre-population/$nino/$taxYear")
        .withFollowRedirects(false)
        .withHttpHeaders(
          (AUTHORIZATION, "Bearer 123"),
          ("mtditid", mtdItId)
        )
    }
  }

  "/pre-population/:nino/:taxYear" when {
    "IF returns a non-404 error when retrieving a user's state benefits" should {
      "return an INTERNAL SERVER ERROR response" in new Test {
        val httpResponse: HttpResponse = HttpResponse(IM_A_TEAPOT, "teapot time")
        stubGetHttpClientCall(s"/if/income-tax/income/state-benefits/$ifTaxYearParam/$nino", httpResponse)

        val result: WSResponse = await(request().get())
        result.status shouldBe INTERNAL_SERVER_ERROR
      }
    }

    "IF returns a 404 error when retrieving a user's state benefits" should {
      "return an empty pre-pop response" in new Test {
        val httpResponse: HttpResponse = HttpResponse(NOT_FOUND, "no teapot found")
        stubGetHttpClientCall(s"/if/income-tax/income/state-benefits/$ifTaxYearParam/$nino", httpResponse)

        val result: WSResponse = await(request().get())
        result.status shouldBe OK
        result.json shouldBe Json.toJson(PrePopulationResponse.noPrePop)
      }
    }

    "IF returns no data when retrieving a user's state benefits" should {
      "return an empty pre-pop response" in new Test {
        val httpResponse: HttpResponse = HttpResponse(OK, "{}")
        stubGetHttpClientCall(s"/if/income-tax/income/state-benefits/$ifTaxYearParam/$nino", httpResponse)

        val result: WSResponse = await(request().get())
        result.status shouldBe OK
        result.json shouldBe Json.toJson(PrePopulationResponse.noPrePop)
      }
    }

    "IF returns no relevant data when retrieving a user's state benefits" should {
      "return an empty pre-pop response" in new Test {
        val data: AllStateBenefitsData = AllStateBenefitsData(
          stateBenefitsData = Some(aStateBenefitsData.copy(
            employmentSupportAllowances = None,
            jobSeekersAllowances = None
          )),
          customerAddedStateBenefitsData = Some(aCustomerAddedStateBenefitsData.copy(
            employmentSupportAllowances = None,
            jobSeekersAllowances = None
          ))
        )

        val httpResponse: HttpResponse = HttpResponse(OK, Json.toJson(data).toString())
        stubGetHttpClientCall(s"/if/income-tax/income/state-benefits/$ifTaxYearParam/$nino", httpResponse)

        val result: WSResponse = await(request().get())
        result.status shouldBe OK
        result.json shouldBe Json.toJson(PrePopulationResponse.noPrePop)
      }
    }

    "IF returns relevant data when retrieving a user's state benefits" should {
      "return the appropriate pre-population response when only customer data exists" in new Test {
        val customerOnlyIfResponse: AllStateBenefitsData = AllStateBenefitsData(
          stateBenefitsData = None,
          customerAddedStateBenefitsData = Some(aCustomerAddedStateBenefitsData)
        )

        val httpResponse: HttpResponse = HttpResponse(OK, Json.toJson(customerOnlyIfResponse).toString())
        stubGetHttpClientCall(s"/if/income-tax/income/state-benefits/$ifTaxYearParam/$nino", httpResponse)

        val result: WSResponse = await(request().get())
        result.status shouldBe OK
        result.json shouldBe Json.toJson(PrePopulationResponse(
          hasEsaPrePop = true, hasJsaPrePop = true, hasPensionsPrePop = true, hasPensionLumpSumsPrePop = true
        ))
      }

      "return the appropriate pre-population response when only non-ignored HMRC-held data exists" in new Test {
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
          jobSeekersAllowances = Some(Set(aStateBenefit)),
          statePension = Some(aStateBenefit),
          statePensionLumpSum = Some(aStateBenefit),

        )

        val hmrcHeldOnlyIfResponse: AllStateBenefitsData = AllStateBenefitsData(
          stateBenefitsData = Some(aStateBenefitsData),
          customerAddedStateBenefitsData = None
        )

        val httpResponse: HttpResponse = HttpResponse(OK, Json.toJson(hmrcHeldOnlyIfResponse).toString())
        stubGetHttpClientCall(s"/if/income-tax/income/state-benefits/$ifTaxYearParam/$nino", httpResponse)

        val result: WSResponse = await(request().get())
        result.status shouldBe OK
        result.json shouldBe Json.toJson(PrePopulationResponse(
          hasEsaPrePop = true, hasJsaPrePop = true, hasPensionsPrePop = true, hasPensionLumpSumsPrePop = true
        ))
      }

      "return an empty pre-population response when only ignored HMRC-held data exists" in new Test {
        val hmrcHeldOnlyIfResponse: AllStateBenefitsData = AllStateBenefitsData(
          stateBenefitsData = Some(aStateBenefitsData),
          customerAddedStateBenefitsData = None
        )

        val httpResponse: HttpResponse = HttpResponse(OK, Json.toJson(hmrcHeldOnlyIfResponse).toString())
        stubGetHttpClientCall(s"/if/income-tax/income/state-benefits/$ifTaxYearParam/$nino", httpResponse)

        val result: WSResponse = await(request().get())
        result.status shouldBe OK
        result.json shouldBe Json.toJson(PrePopulationResponse.noPrePop)
      }

      "return the appropriate pre-population response for a mixed scenario" in new Test {
        val customerData: Option[CustomerAddedStateBenefitsData] = Some(
          aCustomerAddedStateBenefitsData.copy(jobSeekersAllowances = None)
        )

        val mixedResponse: AllStateBenefitsData = anAllStateBenefitsData.copy(
          customerAddedStateBenefitsData = customerData
        )

        val httpResponse: HttpResponse = HttpResponse(OK, Json.toJson(mixedResponse).toString())
        stubGetHttpClientCall(s"/if/income-tax/income/state-benefits/$ifTaxYearParam/$nino", httpResponse)

        val result: WSResponse = await(request().get())
        result.status shouldBe OK
        result.json shouldBe Json.toJson(PrePopulationResponse(
          hasEsaPrePop = true, hasJsaPrePop = false, hasPensionsPrePop = true, hasPensionLumpSumsPrePop = true
        ))
      }
    }
  }

}
