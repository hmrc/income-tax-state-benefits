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
import models.api.AllStateBenefitsData
import models.errors.ServiceError
import models.prePopulation.PrePopulationResponse
import uk.gov.hmrc.http.HeaderCarrier
import utils.PrePopulationLogging

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PrePopulationService @Inject()(service: StateBenefitsService) extends PrePopulationLogging {
  val classLoggingContext = "PrePopulationService"

  def get(taxYear: Int, nino: String)
         (implicit ec: ExecutionContext,hc: HeaderCarrier): EitherT[Future, ServiceError, PrePopulationResponse] = {

    val methodLoggingContext: String = "get"
    val userDataLogString: String = s" for NINO: $nino, and tax year: $taxYear"
    val getInfoLogger = infoLog(methodLoggingContext = methodLoggingContext, dataLog = userDataLogString)

    getInfoLogger("Attempting to retrieve user's state benefits data from IF")

    def result: EitherT[Future, ServiceError, PrePopulationResponse] = for {
      allStateBenefitsDataOpt <- EitherT(service.getAllStateBenefitsData(taxYear, nino))
    } yield toPrePopulationResponse(allStateBenefitsDataOpt)

    def toPrePopulationResponse(data: Option[AllStateBenefitsData]): PrePopulationResponse = data.fold {
      getInfoLogger("No state benefits data found in success response from IF. Returning 'no pre-pop' response")
      PrePopulationResponse.noPrePop
    }(prePop => {
      getInfoLogger("Valid state benefits data found within success response from IF. Determining correct pre-pop response")

      PrePopulationResponse.fromData(
        customerData = prePop.customerAddedStateBenefitsData,
        hmrcData = prePop.stateBenefitsData
      )
    })

    val errorLogString: String = "Attempt to retrieve user's state benefits data from IF failed with "

    result.leftFlatMap{
      case error if error.message.contains("404") =>
        getInfoLogger(errorLogString + "'404 data not found' error. Returning 'no pre-pop' response")
        EitherT.right(Future.successful(PrePopulationResponse.noPrePop))
      case err =>
        warnLog(methodLoggingContext, userDataLogString)(errorLogString + s"error: ${err.message}")
        EitherT.left(Future.successful(err))
    }
  }
}
