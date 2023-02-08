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
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NO_CONTENT}
import play.api.libs.json.Json
import services.PagerDutyLoggerService
import support.ConnectorIntegrationTest
import support.providers.TaxYearProvider
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, SessionId}

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global

class DataExchangeServiceConnectorISpec extends ConnectorIntegrationTest
  with MockFactory
  with TaxYearProvider {

  private val benefitId = UUID.randomUUID()
  private val nino = "some-nino"
  private val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue")))

  private val pagerDutyLoggerService = mock[PagerDutyLoggerService]

  private val underTest = new DataExchangeServiceConnector(httpClient, pagerDutyLoggerService, appConfigStub)

  private def toTaxYearParameter(taxYear: Int): String = {
    s"${taxYear - 1}-${taxYear.toString takeRight 2}"
  }

  ".deleteStateBenefitDetailOverride" should {
    "return correct response when correct data is passed" in {
      val httpResponse = HttpResponse(NO_CONTENT, "")

      stubDeleteHttpClientCall(s"/income-tax/income/state-benefits/$nino/${toTaxYearParameter(taxYear)}/$benefitId", httpResponse)

      await(underTest.deleteStateBenefitDetailOverride(taxYear, nino, benefitId)(hc)) shouldBe Right(())
    }

    "return error and perform a pagerDutyLog when Left is returned" in {
      val httpResponse = HttpResponse(INTERNAL_SERVER_ERROR, Json.toJson(SingleErrorBody("some-code", "some-reason")).toString())

      (pagerDutyLoggerService.pagerDutyLog _).expects(*, "DeleteStateBenefitDetailOverrideResponse")

      stubDeleteHttpClientCall(s"/income-tax/income/state-benefits/$nino/${toTaxYearParameter(taxYear)}/$benefitId", httpResponse)

      await(underTest.deleteStateBenefitDetailOverride(taxYear, nino, benefitId)(hc)) shouldBe
        Left(ApiError(INTERNAL_SERVER_ERROR, SingleErrorBody("some-code", "some-reason")))
    }
  }
}
