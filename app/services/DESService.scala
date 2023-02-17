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

package services

import connectors.DataExchangeServiceConnector
import models.errors.ApiServiceError
import models.mongo.StateBenefitsUserData
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DESService @Inject()(connector: DataExchangeServiceConnector)
                          (implicit ec: ExecutionContext) {

  def removeCustomerOverride(stateBenefitsUserData: StateBenefitsUserData)
                            (implicit hc: HeaderCarrier): Future[Either[ApiServiceError, Unit]] = {
    connector.deleteStateBenefitDetailOverride(stateBenefitsUserData.taxYear, stateBenefitsUserData.nino, stateBenefitsUserData.claim.get.benefitId.get).map {
      case Left(error) => Left(ApiServiceError(error.status.toString))
      case Right(_) => Right(())
    }
  }
}
