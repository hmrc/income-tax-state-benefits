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
import connectors.responses.GetStateBenefitsResponse
import connectors.responses.GetStateBenefitsResponse.getStateBenefitsResponseReads
import models.api.AllStateBenefitsData
import services.PagerDutyLoggerService
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class IntegrationFrameworkConnector @Inject()(httpClient: HttpClient,
                                              pagerDutyLoggerService: PagerDutyLoggerService,
                                              appConf: AppConfig)
                                             (implicit ec: ExecutionContext) extends IFConnector {
  val apiVersion = "1652"

  override protected[connectors] val appConfig: AppConfig = appConf

  def getAllStateBenefitsData(taxYear: Int, nino: String)
                             (implicit hc: HeaderCarrier): Future[Either[ApiError, Option[AllStateBenefitsData]]] = {
    val getRequestResponse = callGetStateBenefits(taxYear, nino)(ifHeaderCarrier(getStateBenefitsUrl(taxYear, nino), apiVersion))

    getRequestResponse.map { apiResponse =>
      if (apiResponse.result.isLeft) pagerDutyLoggerService.pagerDutyLog(apiResponse.httpResponse, apiResponse.getClass.getSimpleName)
      apiResponse.result
    }
  }

  private def getStateBenefitsUrl(taxYear: Int, nino: String): String = {
    val taxYearParameter = s"${taxYear - 1}-${taxYear.toString takeRight 2}"
    baseUrl + s"/income-tax/income/state-benefits/$nino/$taxYearParameter"
  }

  private def callGetStateBenefits(taxYear: Int, nino: String)
                                  (implicit hc: HeaderCarrier): Future[GetStateBenefitsResponse] = {
    httpClient.GET[GetStateBenefitsResponse](getStateBenefitsUrl(taxYear, nino))
  }
}
