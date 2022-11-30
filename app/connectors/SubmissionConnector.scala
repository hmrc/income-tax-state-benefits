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

import config.AppConfig
import connectors.errors.ApiError
import connectors.responses.{GetIncomeTaxUserDataResponse, RefreshIncomeSourceResponse}
import models.IncomeTaxUserData
import models.requests.RefreshIncomeSourceRequest
import services.PagerDutyLoggerService
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SubmissionConnector @Inject()(httpClient: HttpClient,
                                    pagerDutyLoggerService: PagerDutyLoggerService,
                                    config: AppConfig)(implicit ec: ExecutionContext) {

  def getIncomeTaxUserData(taxYear: Int, nino: String, mtditid: String)
                          (implicit hc: HeaderCarrier): Future[Either[ApiError, IncomeTaxUserData]] = {
    val eventualResponse = getAllStateBenefitsDataResponse(taxYear, nino)(hc.withExtraHeaders(("mtditid", mtditid)))

    eventualResponse.map { response =>
      if (response.result.isLeft) pagerDutyLoggerService.pagerDutyLog(response.httpResponse, response.getClass.getSimpleName)
      response.result
    }
  }

  def refreshStateBenefits(taxYear: Int, nino: String, mtditid: String)
                          (implicit hc: HeaderCarrier): Future[Either[ApiError, Unit]] = {
    val eventualResponse = refreshStateBenefitsResponse(taxYear, nino)(hc.withExtraHeaders(("mtditid", mtditid)))

    eventualResponse.map { response =>
      if (response.result.isLeft) pagerDutyLoggerService.pagerDutyLog(response.httpResponse, response.getClass.getSimpleName)
      response.result
    }
  }

  private def getAllStateBenefitsDataResponse(taxYear: Int, nino: String)
                                             (implicit hc: HeaderCarrier): Future[GetIncomeTaxUserDataResponse] = {
    val url = config.submissionBaseUrl + s"/income-tax/nino/$nino/sources/session?taxYear=$taxYear"
    httpClient.GET[GetIncomeTaxUserDataResponse](url)
  }

  private def refreshStateBenefitsResponse(taxYear: Int, nino: String)
                                          (implicit hc: HeaderCarrier): Future[RefreshIncomeSourceResponse] = {
    val url = config.submissionBaseUrl + s"/income-tax/nino/$nino/sources/session?taxYear=$taxYear"
    httpClient.PUT[RefreshIncomeSourceRequest, RefreshIncomeSourceResponse](url, RefreshIncomeSourceRequest("state-benefits"))
  }
}
