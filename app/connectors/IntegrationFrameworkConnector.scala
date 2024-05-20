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
import connectors.responses._
import models.api.{AddStateBenefit, AllStateBenefitsData, StateBenefitDetailOverride, UpdateStateBenefit}
import services.PagerDutyLoggerService
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, StringContextOps}

import java.net.URL
import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class IntegrationFrameworkConnector @Inject()(httpClient: HttpClient,
                                              pagerDutyLoggerService: PagerDutyLoggerService,
                                              appConf: AppConfig)
                                             (implicit ec: ExecutionContext) extends IFConnector {

  private val createOrUpdateApiVersion = "1651"
  private val createOrUpdateApiVersionAfter23 = "1937"
  private val getApiVersion = "1652"
  private val getApi2324Version = "1938"
  private val addApiVersion = "1676"
  private val updateApiVersion = "1677"
  private val deleteApiVersion = "1678"
  private val deleteOverrideApiVersion = "1653"
  private val deleteOverrideApi2324Version = "1796"
  private val ignoreApiVersion = "1679"
  private val ignoreApi2324Version = "1944"
  private val unIgnoreApiVersion = "1700"
  private val unIgnoreApi2324Version = "1945"

  override protected[connectors] val appConfig: AppConfig = appConf

  def getAllStateBenefitsData(taxYear: Int, nino: String)
                             (implicit hc: HeaderCarrier): Future[Either[ApiError, Option[AllStateBenefitsData]]] = {
    val (url, apiVersion) = if (shouldUse2324(taxYear)) {
      (new URL(s"$baseUrl/income-tax/income/state-benefits/23-24/$nino"), getApi2324Version)
    } else {
      (new URL(s"$baseUrl/income-tax/income/state-benefits/$nino/${toTaxYearParam(taxYear)}"), getApiVersion)
    }

    val getRequestResponse = callGetStateBenefits(url)(ifHeaderCarrier(url, apiVersion))

    getRequestResponse.map { apiResponse =>
      if (apiResponse.result.isLeft) pagerDutyLoggerService.pagerDutyLog(apiResponse.httpResponse, apiResponse.getClass.getSimpleName)
      apiResponse.result
    }
  }

  def addCustomerStateBenefit(taxYear: Int, nino: String, addStateBenefit: AddStateBenefit)
                             (implicit hc: HeaderCarrier): Future[Either[ApiError, UUID]] = {
    val url = new URL(s"$baseUrl/income-tax/income/state-benefits/$nino/${toTaxYearParam(taxYear)}/custom")
    val getRequestResponse = callAddStateBenefit(url, addStateBenefit)(ifHeaderCarrier(url, addApiVersion))

    getRequestResponse.map { apiResponse =>
      if (apiResponse.result.isLeft) pagerDutyLoggerService.pagerDutyLog(apiResponse.httpResponse, apiResponse.getClass.getSimpleName)
      apiResponse.result
    }
  }

  def updateCustomerStateBenefit(taxYear: Int, nino: String, benefitId: UUID, updateStateBenefit: UpdateStateBenefit)
                                (implicit hc: HeaderCarrier): Future[Either[ApiError, Unit]] = {
    val url = new URL(s"$baseUrl/income-tax/income/state-benefits/$nino/${toTaxYearParam(taxYear)}/custom/$benefitId")
    val eventualResponse = callUpdateStateBenefit(url, updateStateBenefit)(ifHeaderCarrier(url, updateApiVersion))

    eventualResponse.map { apiResponse =>
      if (apiResponse.result.isLeft) pagerDutyLoggerService.pagerDutyLog(apiResponse.httpResponse, apiResponse.getClass.getSimpleName)
      apiResponse.result
    }
  }

  def createOrUpdateStateBenefitDetailOverride(taxYear: Int,
                                               nino: String,
                                               benefitId: UUID,
                                               stateBenefitDetailOverride: StateBenefitDetailOverride)
                                              (implicit hc: HeaderCarrier): Future[Either[ApiError, Unit]] = {

    val (url, apiVersion) = if (isAfter2324Api(taxYear)) {
      (new URL(s"$baseUrl/income-tax/${asTys(taxYear)}/income/state-benefits/$nino/$benefitId"), createOrUpdateApiVersionAfter23)
    } else {
      (new URL(s"$baseUrl/income-tax/income/state-benefits/$nino/${toTaxYearParam(taxYear)}/$benefitId"), createOrUpdateApiVersion)
    }
    val eventualResponse = callCreateOrUpdateDetailOverride(url, stateBenefitDetailOverride)(ifHeaderCarrier(url, apiVersion))

    eventualResponse.map { apiResponse: CreateOrUpdateStateBenefitResponse =>
      if (apiResponse.result.isLeft) pagerDutyLoggerService.pagerDutyLog(apiResponse.httpResponse, apiResponse.getClass.getSimpleName)
      apiResponse.result
    }
  }

  def deleteStateBenefitDetailOverride(taxYear: Int, nino: String, benefitId: UUID)
                                      (implicit hc: HeaderCarrier): Future[Either[ApiError, Unit]] = {

    val (url, apiVersion) = if (isAfter2324Api(taxYear)) {
      (new URL(s"$baseUrl/income-tax/income/state-benefits/${asTys(taxYear)}/$nino/$benefitId"), deleteOverrideApi2324Version)
    } else {
      (new URL(s"$baseUrl/income-tax/income/state-benefits/$nino/${toTaxYearParam(taxYear)}/$benefitId"), deleteOverrideApiVersion)
    }

    val eventualResponse = callDeleteStateBenefitDetailOverride(url)(ifHeaderCarrier(url, apiVersion))

    eventualResponse.map { apiResponse: DeleteStateBenefitDetailOverrideResponse =>
      if (apiResponse.result.isLeft) pagerDutyLoggerService.pagerDutyLog(apiResponse.httpResponse, apiResponse.getClass.getSimpleName)
      apiResponse.result
    }
  }

  def deleteStateBenefit(taxYear: Int, nino: String, benefitId: UUID)
                        (implicit hc: HeaderCarrier): Future[Either[ApiError, Unit]] = {
    val url = new URL(s"$baseUrl/income-tax/income/state-benefits/$nino/${toTaxYearParam(taxYear)}/custom/$benefitId")
    val eventualResponse = callDeleteStateBenefit(url)(ifHeaderCarrier(url, deleteApiVersion))

    eventualResponse.map { apiResponse: DeleteStateBenefitResponse =>
      if (apiResponse.result.isLeft) pagerDutyLoggerService.pagerDutyLog(apiResponse.httpResponse, apiResponse.getClass.getSimpleName)
      apiResponse.result
    }
  }

  def ignoreStateBenefit(taxYear: Int,
                         nino: String,
                         benefitId: UUID)
                        (implicit hc: HeaderCarrier): Future[Either[ApiError, Unit]] = {

    val (url, apiVersion) = if (isAfter2324Api(taxYear)) {
      (url"$baseUrl/income-tax/${asTys(taxYear)}/income/state-benefits/$nino/ignore/$benefitId", ignoreApi2324Version)
    } else {
      (url"$baseUrl/income-tax/income/state-benefits/$nino/${toTaxYearParam(taxYear)}/ignore/$benefitId", ignoreApiVersion)
    }
    val eventualResponse = callIgnoreStateBenefit(url)(ifHeaderCarrier(url, apiVersion))

    eventualResponse.map { apiResponse: IgnoreStateBenefitResponse =>
      if (apiResponse.result.isLeft) pagerDutyLoggerService.pagerDutyLog(apiResponse.httpResponse, apiResponse.getClass.getSimpleName)
      apiResponse.result
    }
  }

  def unIgnoreStateBenefit(taxYear: Int,
                           nino: String,
                           benefitId: UUID)
                          (implicit hc: HeaderCarrier): Future[Either[ApiError, Unit]] = {

    val (url, apiVersion) = if (isAfter2324Api(taxYear)) {
      (url"$baseUrl/income-tax/${asTys(taxYear)}/state-benefits/$nino/ignore/$benefitId", unIgnoreApi2324Version)
    } else {
      (url"$baseUrl/income-tax/state-benefits/$nino/${toTaxYearParam(taxYear)}/ignore/$benefitId", unIgnoreApiVersion)
    }
    val eventualResponse = callUnIgnoreStateBenefit(url)(ifHeaderCarrier(url, apiVersion))

    eventualResponse.map { apiResponse: UnIgnoreStateBenefitResponse =>
      if (apiResponse.result.isLeft) pagerDutyLoggerService.pagerDutyLog(apiResponse.httpResponse, apiResponse.getClass.getSimpleName)
      apiResponse.result
    }
  }

  private def callCreateOrUpdateDetailOverride(url: URL, stateBenefitDetailOverride: StateBenefitDetailOverride)
                                              (implicit hc: HeaderCarrier): Future[CreateOrUpdateStateBenefitResponse] = {
    httpClient.PUT[StateBenefitDetailOverride, CreateOrUpdateStateBenefitResponse](url, stateBenefitDetailOverride)
  }

  private def callGetStateBenefits(url: URL)(implicit hc: HeaderCarrier): Future[GetStateBenefitsResponse] = {
    httpClient.GET[GetStateBenefitsResponse](url)
  }

  private def callAddStateBenefit(url: URL, addStateBenefit: AddStateBenefit)
                                 (implicit hc: HeaderCarrier): Future[AddStateBenefitResponse] = {
    httpClient.POST[AddStateBenefit, AddStateBenefitResponse](url, addStateBenefit)
  }

  private def callUpdateStateBenefit(url: URL, updateStateBenefit: UpdateStateBenefit)(implicit hc: HeaderCarrier): Future[UpdateStateBenefitResponse] = {
    httpClient.PUT[UpdateStateBenefit, UpdateStateBenefitResponse](url, updateStateBenefit)
  }

  private def callDeleteStateBenefit(url: URL)(implicit hc: HeaderCarrier): Future[DeleteStateBenefitResponse] = {
    httpClient.DELETE[DeleteStateBenefitResponse](url)
  }

  private def callDeleteStateBenefitDetailOverride(url: URL)(implicit hc: HeaderCarrier): Future[DeleteStateBenefitDetailOverrideResponse] = {
    httpClient.DELETE[DeleteStateBenefitDetailOverrideResponse](url)
  }

  private def callIgnoreStateBenefit(url: URL)(implicit hc: HeaderCarrier): Future[IgnoreStateBenefitResponse] = {
    httpClient.PUT[Map[String, String], IgnoreStateBenefitResponse](url, Map[String, String]())
  }

  private def callUnIgnoreStateBenefit(url: URL)(implicit hc: HeaderCarrier): Future[UnIgnoreStateBenefitResponse] = {
    httpClient.DELETE[UnIgnoreStateBenefitResponse](url)
  }

  private def toTaxYearParam(taxYear: Int): String = {
    s"${taxYear - 1}-${taxYear.toString takeRight 2}"
  }

  private def shouldUse2324(taxYear: Int): Boolean = {
    taxYear == 2024
  }

  private def isAfter2324Api(taxYear: Int): Boolean = {
    taxYear >= 2024
  }

  private def asTys(taxYear: Int): String = {
    val end = taxYear - 2000
    val start = end - 1
    s"$start-$end"
  }

}
