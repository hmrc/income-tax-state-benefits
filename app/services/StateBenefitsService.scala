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

import connectors.errors.ApiError
import connectors.{IntegrationFrameworkConnector, SubmissionConnector}
import models.IncomeTaxUserData
import models.api.AllStateBenefitsData
import models.errors.ServiceError
import models.mongo.StateBenefitsUserData
import repositories.StateBenefitsUserDataRepository
import uk.gov.hmrc.http.HeaderCarrier

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class StateBenefitsService @Inject()(submissionConnector: SubmissionConnector,
                                     integrationFrameworkConnector: IntegrationFrameworkConnector,
                                     stateBenefitsUserDataRepository: StateBenefitsUserDataRepository) {

  def getAllStateBenefitsData(taxYear: Int, nino: String)
                             (implicit hc: HeaderCarrier): Future[Either[ApiError, Option[AllStateBenefitsData]]] = {
    integrationFrameworkConnector.getAllStateBenefitsData(taxYear, nino)
  }

  def getPriorData(taxYear: Int, nino: String, mtdtid: String)
                  (implicit hc: HeaderCarrier): Future[Either[ApiError, IncomeTaxUserData]] = {
    submissionConnector.getIncomeTaxUserData(taxYear, nino, mtdtid)
  }

  def getStateBenefitsUserData(sessionDataId: UUID): Future[Either[ServiceError, StateBenefitsUserData]] = {
    stateBenefitsUserDataRepository.find(sessionDataId)
  }

  def createOrUpdateStateBenefitsUserData(stateBenefitsUserData: StateBenefitsUserData): Future[Either[ServiceError, UUID]] = {
    stateBenefitsUserDataRepository.createOrUpdate(stateBenefitsUserData)
  }
}