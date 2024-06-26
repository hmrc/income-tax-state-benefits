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

package connectors

import connectors.errors.{ApiError, SingleErrorBody}
import org.scalamock.scalatest.MockFactory
import play.api.http.Status.{CREATED, INTERNAL_SERVER_ERROR, NO_CONTENT, OK}
import play.api.libs.json.Json
import services.PagerDutyLoggerService
import support.ConnectorIntegrationTest
import support.builders.api.AddStateBenefitBuilder.anAddStateBenefit
import support.builders.api.AllStateBenefitsDataBuilder.anAllStateBenefitsData
import support.builders.api.StateBenefitDetailOverrideBuilder.aStateBenefitDetailOverride
import support.builders.api.UpdateStateBenefitBuilder.anUpdateStateBenefit
import support.providers.TaxYearProvider
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, SessionId}

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global

class IntegrationFrameworkConnectorISpec extends ConnectorIntegrationTest
  with MockFactory
  with TaxYearProvider {

  private val benefitId = UUID.randomUUID()
  private val nino = "some-nino"
  private val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue")))

  private val pagerDutyLoggerService = mock[PagerDutyLoggerService]
  private val underTest = new IntegrationFrameworkConnector(httpClient, pagerDutyLoggerService, appConfigStub)

  private def toTaxYearParameter(taxYear: Int): String = {
    s"${taxYear - 1}-${taxYear.toString takeRight 2}"
  }

  private def asTys(taxYear: Int): String = {
    val end = taxYear - 2000
    val start = end - 1
    s"$start-$end"
  }

  ".getAllStateBenefitsData" when {
    "when tax year not 23-24" should {
      "return correct IF data when correct parameters are passed" in {
        val httpResponse = HttpResponse(OK, Json.toJson(anAllStateBenefitsData).toString())

        stubGetHttpClientCall(s"/if/income-tax/income/state-benefits/$nino/${toTaxYearParameter(2023)}", httpResponse)

        await(underTest.getAllStateBenefitsData(2023, nino)(hc)) shouldBe Right(Some(anAllStateBenefitsData))
      }

      "return IF error and perform a pagerDutyLog when Left is returned" in {
        val httpResponse = HttpResponse(INTERNAL_SERVER_ERROR, Json.toJson(SingleErrorBody("some-code", "some-reason")).toString())

        (pagerDutyLoggerService.pagerDutyLog _).expects(*, "GetStateBenefitsResponse")

        stubGetHttpClientCall(s"/if/income-tax/income/state-benefits/$nino/${toTaxYearParameter(2023)}", httpResponse)

        await(underTest.getAllStateBenefitsData(2023, nino)(hc)) shouldBe
          Left(ApiError(INTERNAL_SERVER_ERROR, SingleErrorBody("some-code", "some-reason")))
      }
    }

    "when tax year 23-24" should {
      "return correct IF data when correct parameters are passed" in {
        val httpResponse = HttpResponse(OK, Json.toJson(anAllStateBenefitsData).toString())

        stubGetHttpClientCall(s"/if/income-tax/income/state-benefits/23-24/$nino", httpResponse)

        await(underTest.getAllStateBenefitsData(2024, nino)(hc)) shouldBe Right(Some(anAllStateBenefitsData))
      }

      "return IF error and perform a pagerDutyLog when Left is returned" in {
        val httpResponse = HttpResponse(INTERNAL_SERVER_ERROR, Json.toJson(SingleErrorBody("some-code", "some-reason")).toString())

        (pagerDutyLoggerService.pagerDutyLog _).expects(*, "GetStateBenefitsResponse")

        stubGetHttpClientCall(s"/if/income-tax/income/state-benefits/23-24/$nino", httpResponse)

        await(underTest.getAllStateBenefitsData(2024, nino)(hc)) shouldBe
          Left(ApiError(INTERNAL_SERVER_ERROR, SingleErrorBody("some-code", "some-reason")))
      }
    }
  }

  ".addCustomerStateBenefit" should {
    "return correct IF data when correct parameters are passed" in {
      val httpResponse = HttpResponse(OK, s"""{"benefitId": "$benefitId"}""")

      val url = s"/if/income-tax/income/state-benefits/$nino/${toTaxYearParameter(taxYear)}/custom"
      stubPostHttpClientCall(url, Json.toJson(anAddStateBenefit).toString(), httpResponse)

      await(underTest.addCustomerStateBenefit(taxYear, nino, anAddStateBenefit)(hc)) shouldBe Right(benefitId)
    }

    "return IF error and perform a pagerDutyLog when Left is returned" in {
      val httpResponse = HttpResponse(INTERNAL_SERVER_ERROR, Json.toJson(SingleErrorBody("some-code", "some-reason")).toString())

      (pagerDutyLoggerService.pagerDutyLog _).expects(*, "AddStateBenefitResponse")

      val url = s"/if/income-tax/income/state-benefits/$nino/${toTaxYearParameter(taxYear)}/custom"
      stubPostHttpClientCall(url, Json.toJson(anAddStateBenefit).toString(), httpResponse)

      await(underTest.addCustomerStateBenefit(taxYear, nino, anAddStateBenefit)(hc)) shouldBe
        Left(ApiError(INTERNAL_SERVER_ERROR, SingleErrorBody("some-code", "some-reason")))
    }
  }

  ".updateCustomerStateBenefit" should {
    val url = s"/if/income-tax/income/state-benefits/$nino/${toTaxYearParameter(taxYear)}/custom/$benefitId"
    "return correct IF data when correct parameters are passed" in {
      val httpResponse = HttpResponse(CREATED, "")

      stubPutHttpClientCall(url, Json.toJson(anUpdateStateBenefit).toString(), httpResponse)

      await(underTest.updateCustomerStateBenefit(taxYear, nino, benefitId, anUpdateStateBenefit)(hc)) shouldBe Right(())
    }

    "return IF error and perform a pagerDutyLog when Left is returned" in {
      val httpResponse = HttpResponse(INTERNAL_SERVER_ERROR, Json.toJson(SingleErrorBody("some-code", "some-reason")).toString())

      (pagerDutyLoggerService.pagerDutyLog _).expects(*, "UpdateStateBenefitResponse")

      stubPutHttpClientCall(url, Json.toJson(anUpdateStateBenefit).toString(), httpResponse)

      await(underTest.updateCustomerStateBenefit(taxYear, nino, benefitId, anUpdateStateBenefit)(hc)) shouldBe
        Left(ApiError(INTERNAL_SERVER_ERROR, SingleErrorBody("some-code", "some-reason")))
    }
  }

  ".createOrUpdateStateBenefitDetailOverride" should {
    val taxYearBefore24 = 2023

    "return correct IF data when correct parameters are passed for tax year before 23" in {
      val jsValue = Json.toJson(aStateBenefitDetailOverride)
      val httpResponse = HttpResponse(NO_CONTENT, "")

      stubPutHttpClientCall(s"/if/income-tax/income/state-benefits/$nino/${toTaxYearParameter(taxYearBefore24)}/$benefitId", jsValue.toString(), httpResponse)

      await(underTest.createOrUpdateStateBenefitDetailOverride(taxYearBefore24, nino, benefitId, aStateBenefitDetailOverride)(hc)) shouldBe Right(())
    }

    "return IF error and perform a pagerDutyLog when Left is returned for tax year before 23" in {
      val jsValue = Json.toJson(aStateBenefitDetailOverride)
      val httpResponse = HttpResponse(INTERNAL_SERVER_ERROR, Json.toJson(SingleErrorBody("some-code", "some-reason")).toString())

      (pagerDutyLoggerService.pagerDutyLog _).expects(*, "CreateOrUpdateStateBenefitResponse")

      stubPutHttpClientCall(s"/if/income-tax/income/state-benefits/$nino/${toTaxYearParameter(taxYearBefore24)}/$benefitId", jsValue.toString(), httpResponse)

      await(underTest.createOrUpdateStateBenefitDetailOverride(taxYearBefore24, nino, benefitId, aStateBenefitDetailOverride)(hc)) shouldBe
        Left(ApiError(INTERNAL_SERVER_ERROR, SingleErrorBody("some-code", "some-reason")))
    }

    "return correct IF data when correct parameters are passed for tax year after 23" in {
      val jsValue = Json.toJson(aStateBenefitDetailOverride)
      val httpResponse = HttpResponse(NO_CONTENT, "")

      stubPutHttpClientCall(s"/if/income-tax/${asTys(taxYear)}/income/state-benefits/$nino/$benefitId", jsValue.toString(), httpResponse)

      await(underTest.createOrUpdateStateBenefitDetailOverride(taxYear, nino, benefitId, aStateBenefitDetailOverride)(hc)) shouldBe Right(())
    }

    "return IF error and perform a pagerDutyLog when Left is returned for tax year after 23" in {
      val jsValue = Json.toJson(aStateBenefitDetailOverride)
      val httpResponse = HttpResponse(INTERNAL_SERVER_ERROR, Json.toJson(SingleErrorBody("some-code", "some-reason")).toString())

      (pagerDutyLoggerService.pagerDutyLog _).expects(*, "CreateOrUpdateStateBenefitResponse")

      stubPutHttpClientCall(s"/if/income-tax/${asTys(taxYear)}/income/state-benefits/$nino/$benefitId", jsValue.toString(), httpResponse)

      await(underTest.createOrUpdateStateBenefitDetailOverride(taxYear, nino, benefitId, aStateBenefitDetailOverride)(hc)) shouldBe
        Left(ApiError(INTERNAL_SERVER_ERROR, SingleErrorBody("some-code", "some-reason")))
    }

  }

  ".deleteStateBenefitDetailOverride" should {
    val taxYearBefore24 = 2023
    "return correct IF response when correct parameters are passed" in {
      val httpResponse = HttpResponse(NO_CONTENT, "")

      stubDeleteHttpClientCall(s"/if/income-tax/income/state-benefits/${asTys(taxYear)}/$nino/$benefitId", httpResponse)

      await(underTest.deleteStateBenefitDetailOverride(taxYear, nino, benefitId)(hc)) shouldBe Right(())
    }

    "return IF error and perform a pagerDutyLog when Left is returned" in {
      val httpResponse = HttpResponse(INTERNAL_SERVER_ERROR, Json.toJson(SingleErrorBody("some-code", "some-reason")).toString())

      (pagerDutyLoggerService.pagerDutyLog _).expects(*, "DeleteStateBenefitDetailOverrideResponse")

      stubDeleteHttpClientCall(s"/if/income-tax/income/state-benefits/${asTys(taxYear)}/$nino/$benefitId", httpResponse)

      await(underTest.deleteStateBenefitDetailOverride(taxYear, nino, benefitId)(hc)) shouldBe
        Left(ApiError(INTERNAL_SERVER_ERROR, SingleErrorBody("some-code", "some-reason")))
    }

    "return correct IF response when correct parameters are passed before 23-24" in {
      val httpResponse = HttpResponse(NO_CONTENT, "")

      stubDeleteHttpClientCall(s"/if/income-tax/income/state-benefits/$nino/${toTaxYearParameter(taxYearBefore24)}/$benefitId", httpResponse)

      await(underTest.deleteStateBenefitDetailOverride(taxYearBefore24, nino, benefitId)(hc)) shouldBe Right(())
    }

    "return IF error and perform a pagerDutyLog when Left is returned before 23-24" in {
      val httpResponse = HttpResponse(INTERNAL_SERVER_ERROR, Json.toJson(SingleErrorBody("some-code", "some-reason")).toString())

      (pagerDutyLoggerService.pagerDutyLog _).expects(*, "DeleteStateBenefitDetailOverrideResponse")

      stubDeleteHttpClientCall(s"/if/income-tax/income/state-benefits/$nino/${toTaxYearParameter(taxYearBefore24)}/$benefitId", httpResponse)

      await(underTest.deleteStateBenefitDetailOverride(taxYearBefore24, nino, benefitId)(hc)) shouldBe
        Left(ApiError(INTERNAL_SERVER_ERROR, SingleErrorBody("some-code", "some-reason")))
    }


  }

  ".deleteStateBenefit" should {
    "return correct IF response when correct parameters are passed" in {
      val httpResponse = HttpResponse(NO_CONTENT, "")

      stubDeleteHttpClientCall(s"/if/income-tax/income/state-benefits/$nino/${toTaxYearParameter(taxYear)}/custom/$benefitId", httpResponse)

      await(underTest.deleteStateBenefit(taxYear, nino, benefitId)(hc)) shouldBe Right(())
    }

    "return IF error and perform a pagerDutyLog when Left is returned" in {
      val httpResponse = HttpResponse(INTERNAL_SERVER_ERROR, Json.toJson(SingleErrorBody("some-code", "some-reason")).toString())

      (pagerDutyLoggerService.pagerDutyLog _).expects(*, "DeleteStateBenefitResponse")

      stubDeleteHttpClientCall(s"/if/income-tax/income/state-benefits/$nino/${toTaxYearParameter(taxYear)}/custom/$benefitId", httpResponse)

      await(underTest.deleteStateBenefit(taxYear, nino, benefitId)(hc)) shouldBe
        Left(ApiError(INTERNAL_SERVER_ERROR, SingleErrorBody("some-code", "some-reason")))
    }
  }

  ".ignoreStateBenefit" should {
    val taxYearBefore24 = 2023
    "return correct IF response when correct parameters are passed" in {
      val httpResponse = HttpResponse(CREATED, "")
      stubPutHttpClientCall(s"/if/income-tax/${asTys(taxYear)}/income/state-benefits/$nino/ignore/$benefitId", "{}", httpResponse)

      await(underTest.ignoreStateBenefit(taxYear, nino, benefitId)(hc)) shouldBe Right(())
    }

    "return IF error and perform a pagerDutyLog when Left is returned" in {
      val httpResponse = HttpResponse(INTERNAL_SERVER_ERROR, Json.toJson(SingleErrorBody("some-code", "some-reason")).toString())

      (pagerDutyLoggerService.pagerDutyLog _).expects(*, "IgnoreStateBenefitResponse")

      stubPutHttpClientCall(s"/if/income-tax/${asTys(taxYear)}/income/state-benefits/$nino/ignore/$benefitId", "{}", httpResponse)

      await(underTest.ignoreStateBenefit(taxYear, nino, benefitId)(hc)) shouldBe
        Left(ApiError(INTERNAL_SERVER_ERROR, SingleErrorBody("some-code", "some-reason")))
    }

    "return correct IF response when correct parameters are passed before 23-24" in {
      val httpResponse = HttpResponse(CREATED, "")

      stubPutHttpClientCall(s"/if/income-tax/income/state-benefits/$nino/${toTaxYearParameter(taxYearBefore24)}/ignore/$benefitId", "{}", httpResponse)

      await(underTest.ignoreStateBenefit(taxYearBefore24, nino, benefitId)(hc)) shouldBe Right(())
    }

    "return IF error and perform a pagerDutyLog when Left is returned before 23-24" in {
      val httpResponse = HttpResponse(INTERNAL_SERVER_ERROR, Json.toJson(SingleErrorBody("some-code", "some-reason")).toString())

      (pagerDutyLoggerService.pagerDutyLog _).expects(*, "IgnoreStateBenefitResponse")

      stubPutHttpClientCall(s"/if/income-tax/income/state-benefits/$nino/${toTaxYearParameter(taxYearBefore24)}/ignore/$benefitId", "{}", httpResponse)

      await(underTest.ignoreStateBenefit(taxYearBefore24, nino, benefitId)(hc)) shouldBe
        Left(ApiError(INTERNAL_SERVER_ERROR, SingleErrorBody("some-code", "some-reason")))
    }
  }

  ".unIgnoreStateBenefit" should {
    val taxYearBefore24 = 2023
    "return correct IF response when correct parameters are passed" in {
      val httpResponse = HttpResponse(NO_CONTENT, "")

      stubDeleteHttpClientCall(s"/if/income-tax/${asTys(taxYear)}/state-benefits/$nino/ignore/$benefitId", httpResponse)

      await(underTest.unIgnoreStateBenefit(taxYear, nino, benefitId)(hc)) shouldBe Right(())
    }

    "return IF error and perform a pagerDutyLog when Left is returned" in {
      val httpResponse = HttpResponse(INTERNAL_SERVER_ERROR, Json.toJson(SingleErrorBody("some-code", "some-reason")).toString())

      (pagerDutyLoggerService.pagerDutyLog _).expects(*, "UnIgnoreStateBenefitResponse")

      stubDeleteHttpClientCall(s"/if/income-tax/${asTys(taxYear)}/state-benefits/$nino/ignore/$benefitId", httpResponse)

      await(underTest.unIgnoreStateBenefit(taxYear, nino, benefitId)(hc)) shouldBe
        Left(ApiError(INTERNAL_SERVER_ERROR, SingleErrorBody("some-code", "some-reason")))
    }

    "return correct IF response when correct parameters are passed before 23-24" in {
      val httpResponse = HttpResponse(NO_CONTENT, "")

      stubDeleteHttpClientCall(s"/if/income-tax/state-benefits/$nino/${toTaxYearParameter(taxYearBefore24)}/ignore/$benefitId", httpResponse)

      await(underTest.unIgnoreStateBenefit(taxYearBefore24, nino, benefitId)(hc)) shouldBe Right(())
    }

    "return IF error and perform a pagerDutyLog when Left is returned before 23-24" in {
      val httpResponse = HttpResponse(INTERNAL_SERVER_ERROR, Json.toJson(SingleErrorBody("some-code", "some-reason")).toString())

      (pagerDutyLoggerService.pagerDutyLog _).expects(*, "UnIgnoreStateBenefitResponse")

      stubDeleteHttpClientCall(s"/if/income-tax/state-benefits/$nino/${toTaxYearParameter(taxYearBefore24)}/ignore/$benefitId", httpResponse)

      await(underTest.unIgnoreStateBenefit(taxYearBefore24, nino, benefitId)(hc)) shouldBe
        Left(ApiError(INTERNAL_SERVER_ERROR, SingleErrorBody("some-code", "some-reason")))
    }
  }
}
