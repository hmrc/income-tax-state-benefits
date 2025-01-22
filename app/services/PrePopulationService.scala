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

import cats.data.EitherT
import models.api.{AllStateBenefitsData, PrePopulationDataWrapper}
import models.errors.ServiceError
import models.prePopulation.PrePopulationResponse
import play.api.Logging
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PrePopulationService @Inject()(service: StateBenefitsService) extends Logging {

  def get(taxYear: Int, nino: String)
         (implicit ec: ExecutionContext,hc: HeaderCarrier): EitherT[Future, ServiceError, PrePopulationResponse] = {

    val loggingContext: String = "[PrePopulationService][get] - "
    val userDataLogString: String = s" for NINO: $nino, and tax year: $taxYear"

    def infoLog(message: String): Unit = logger.info(loggingContext + message + userDataLogString)
    def warnLog(message: String): Unit = logger.warn(loggingContext + message + userDataLogString)

    infoLog("Attempting to retrieve user's state benefits data from IF")

    def result: EitherT[Future, ServiceError, PrePopulationResponse] = for {
      allStateBenefitsDataOpt <- EitherT(service.getAllStateBenefitsData(taxYear, nino))
    } yield toPrePopulationResponse(allStateBenefitsDataOpt)

    def toPrePopulationResponse(data: Option[AllStateBenefitsData]): PrePopulationResponse = data.fold {
      infoLog("No state benefits data found in success response from IF. Returning 'no pre-pop' response")
      PrePopulationResponse.noPrePop
    }(prePop => {
      infoLog("Valid state benefits data found within success response from IF. Determining correct pre-pop response")

      def isEsaJsaPrePopulated[I](data: Option[PrePopulationDataWrapper[I]]): (Boolean, Boolean) =
        data.fold((false, false))(data => (data.hasEsaData, data.hasJsaData))

      val (esaCustomerPrePopulated, jsaCustomerPrePopulated) = isEsaJsaPrePopulated(prePop.customerAddedStateBenefitsData)
      val (esaHmrcHeldPrePopulated, jsaHmrcHeldPrePopulated) = isEsaJsaPrePopulated(prePop.stateBenefitsData)

      PrePopulationResponse(
        hasEsaPrePop = esaCustomerPrePopulated || esaHmrcHeldPrePopulated,
        hasJsaPrePop = jsaCustomerPrePopulated || jsaCustomerPrePopulated
      )
    })

    val errorLogString: String = "Attempt to retrieve user's state benefits data from IF failed with "

    result.leftFlatMap{
      case error if error.message.contains("404") =>
        infoLog(errorLogString + "'404 data not found' error. Returning 'no pre-pop' response")
        EitherT.right(Future.successful(PrePopulationResponse.noPrePop))
      case err =>
        warnLog(errorLogString + s"error: ${err.message}")
        EitherT.left(Future.successful(err))
    }
  }
}
