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

package services

import connectors.SubmissionConnector
import models.IncomeTaxUserData
import models.errors.ApiServiceError
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubmissionService @Inject()(submissionConnector: SubmissionConnector)
                                 (implicit ec: ExecutionContext) {

  def getIncomeTaxUserData(taxYear: Int, nino: String, mtdItId: String)
                          (implicit hc: HeaderCarrier): Future[Either[ApiServiceError, IncomeTaxUserData]] = {
    submissionConnector.getIncomeTaxUserData(taxYear: Int, nino: String, mtdItId: String).map {
      case Left(error) => Left(ApiServiceError(error.status.toString))
      case Right(data) => Right(data)
    }
  }

  def refreshStateBenefits(taxYear: Int, nino: String, mtdItId: String)
                          (implicit hc: HeaderCarrier): Future[Either[ApiServiceError, Unit]] = {
    submissionConnector.refreshStateBenefits(taxYear, nino, mtdItId).map {
      case Left(error) => Left(ApiServiceError(error.status.toString))
      case Right(_) => Right(())
    }
  }
}
