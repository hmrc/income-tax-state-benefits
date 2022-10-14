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

package connectors

import connectors.errors.{ApiError, SingleErrorBody}
import org.scalamock.scalatest.MockFactory
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.Json
import services.PagerDutyLoggerService
import support.ConnectorIntegrationTest
import support.builders.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import support.providers.TaxYearProvider
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, SessionId}

import scala.concurrent.ExecutionContext.Implicits.global

class SubmissionConnectorISpec extends ConnectorIntegrationTest
  with TaxYearProvider
  with MockFactory {

  private val nino = "some-nino"
  private val mtditid = "some-mtditid"
  private val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue")))

  private val pagerDutyLoggerService = mock[PagerDutyLoggerService]

  private val underTest = new SubmissionConnector(httpClient, pagerDutyLoggerService, appConfigStub)

  ".getIncomeTaxUserData" should {
    "return correct data when correct parameters are passed" in {
      val httpResponse = HttpResponse(OK, Json.toJson(anIncomeTaxUserData).toString())

      stubHttpClientCall(s"/income-tax/nino/$nino/sources/session\\?taxYear=$taxYear", httpResponse)

      await(underTest.getIncomeTaxUserData(taxYear, nino, mtditid)(hc)) shouldBe Right(anIncomeTaxUserData)
    }

    "return IF error and perform a pagerDutyLog when Left is returned" in {
      val httpResponse = HttpResponse(INTERNAL_SERVER_ERROR, Json.toJson(SingleErrorBody("some-code", "some-reason")).toString())

      (pagerDutyLoggerService.pagerDutyLog _).expects(*, "GetIncomeTaxUserDataResponse")

      stubHttpClientCall(s"/income-tax/nino/$nino/sources/session\\?taxYear=$taxYear", httpResponse)

      await(underTest.getIncomeTaxUserData(taxYear, nino, mtditid)(hc)) shouldBe
        Left(ApiError(INTERNAL_SERVER_ERROR, SingleErrorBody("some-code", "some-reason")))
    }
  }
}
