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
import connectors.responses.{CreateOrUpdateStateBenefitResponse, DeleteStateBenefitResponse, GetStateBenefitsResponse}
import models.api.{AllStateBenefitsData, StateBenefitDetailOverride}
import services.PagerDutyLoggerService
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class IntegrationFrameworkConnector @Inject()(httpClient: HttpClient,
                                              pagerDutyLoggerService: PagerDutyLoggerService,
                                              appConf: AppConfig)
                                             (implicit ec: ExecutionContext) extends IFConnector {

  private val createOrUpdateApiVersion = "1651"
  private val getApiVersion = "1652"
  private val deleteApiVersion = "1678"

  override protected[connectors] val appConfig: AppConfig = appConf

  def getAllStateBenefitsData(taxYear: Int, nino: String)
                             (implicit hc: HeaderCarrier): Future[Either[ApiError, Option[AllStateBenefitsData]]] = {
    val url = baseUrl + s"/income-tax/income/state-benefits/$nino/${toTaxYearParam(taxYear)}"
    val getRequestResponse = callGetStateBenefits(url)(ifHeaderCarrier(url, getApiVersion))

    getRequestResponse.map { apiResponse =>
      if (apiResponse.result.isLeft) pagerDutyLoggerService.pagerDutyLog(apiResponse.httpResponse, apiResponse.getClass.getSimpleName)
      apiResponse.result
    }
  }

  def createOrUpdateStateBenefitDetailOverride(taxYear: Int,
                                               nino: String,
                                               benefitId: UUID,
                                               stateBenefitDetailOverride: StateBenefitDetailOverride)
                                              (implicit hc: HeaderCarrier): Future[Either[ApiError, Unit]] = {
    val url = baseUrl + s"/income-tax/income/state-benefits/$nino/${toTaxYearParam(taxYear)}/$benefitId"
    val eventualResponse = callCreateOrUpdateDetailOverride(url, stateBenefitDetailOverride)(ifHeaderCarrier(url, createOrUpdateApiVersion))

    eventualResponse.map { apiResponse: CreateOrUpdateStateBenefitResponse =>
      if (apiResponse.result.isLeft) pagerDutyLoggerService.pagerDutyLog(apiResponse.httpResponse, apiResponse.getClass.getSimpleName)
      apiResponse.result
    }
  }

  def deleteStateBenefit(taxYear: Int,
                         nino: String,
                         benefitId: UUID)
                        (implicit hc: HeaderCarrier): Future[Either[ApiError, Unit]] = {
    val url = baseUrl + s"/income-tax/income/state-benefits/$nino/${toTaxYearParam(taxYear)}/custom/$benefitId"
    val eventualResponse = callDeleteStateBenefit(url)(ifHeaderCarrier(url, deleteApiVersion))

    eventualResponse.map { apiResponse: DeleteStateBenefitResponse =>
      if (apiResponse.result.isLeft) pagerDutyLoggerService.pagerDutyLog(apiResponse.httpResponse, apiResponse.getClass.getSimpleName)
      apiResponse.result
    }
  }

  private def callCreateOrUpdateDetailOverride(url: String, stateBenefitDetailOverride: StateBenefitDetailOverride)
                                              (implicit hc: HeaderCarrier): Future[CreateOrUpdateStateBenefitResponse] = {
    httpClient.PUT[StateBenefitDetailOverride, CreateOrUpdateStateBenefitResponse](url, stateBenefitDetailOverride)
  }

  private def callGetStateBenefits(url: String)(implicit hc: HeaderCarrier): Future[GetStateBenefitsResponse] = {
    httpClient.GET[GetStateBenefitsResponse](url)
  }

  def callDeleteStateBenefit(url: String)(implicit hc: HeaderCarrier): Future[DeleteStateBenefitResponse] = {
    httpClient.DELETE[DeleteStateBenefitResponse](url)
  }

  private def toTaxYearParam(taxYear: Int): String = {
    s"${taxYear - 1}-${taxYear.toString takeRight 2}"
  }
}
