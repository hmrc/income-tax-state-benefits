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

import config.AppConfig
import connectors.errors.ApiError
import connectors.responses.DeleteStateBenefitDetailOverrideResponse
import services.PagerDutyLoggerService
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import java.net.URL
import java.util.UUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DataExchangeServiceConnector @Inject()(httpClient: HttpClient,
                                             pagerDutyLoggerService: PagerDutyLoggerService,
                                             appConf: AppConfig)
                                            (implicit ec: ExecutionContext) extends DESConnector {

  override protected[connectors] val appConfig: AppConfig = appConf

  def deleteStateBenefitDetailOverride(taxYear: Int, nino: String, benefitId: UUID)
                                      (implicit hc: HeaderCarrier): Future[Either[ApiError, Unit]] = {
    val url = new URL(s"$baseUrl/income-tax/income/state-benefits/$nino/${toTaxYearParam(taxYear)}/$benefitId")
    val eventualResponse = callDeleteStateBenefitDetailOverride(url)(desHeaderCarrier(url))

    eventualResponse.map { apiResponse: DeleteStateBenefitDetailOverrideResponse =>
      if (apiResponse.result.isLeft) pagerDutyLoggerService.pagerDutyLog(apiResponse.httpResponse, apiResponse.getClass.getSimpleName)
      apiResponse.result
    }
  }

  private def callDeleteStateBenefitDetailOverride(url: URL)(implicit hc: HeaderCarrier): Future[DeleteStateBenefitDetailOverrideResponse] = {
    httpClient.DELETE[DeleteStateBenefitDetailOverrideResponse](url)
  }

  private def toTaxYearParam(taxYear: Int): String = {
    s"${taxYear - 1}-${taxYear.toString takeRight 2}"
  }
}
