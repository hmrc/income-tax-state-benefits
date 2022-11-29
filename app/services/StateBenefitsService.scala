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
import models.api.{AllStateBenefitsData, StateBenefitDetailOverride}
import models.errors.{DataNotUpdatedError, ServiceError}
import models.mongo.StateBenefitsUserData
import repositories.StateBenefitsUserDataRepository
import uk.gov.hmrc.http.HeaderCarrier

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class StateBenefitsService @Inject()(submissionConnector: SubmissionConnector,
                                     integrationFrameworkConnector: IntegrationFrameworkConnector,
                                     stateBenefitsUserDataRepository: StateBenefitsUserDataRepository)
                                    (implicit ec: ExecutionContext) {

  def getAllStateBenefitsData(taxYear: Int, nino: String)
                             (implicit hc: HeaderCarrier): Future[Either[ApiError, Option[AllStateBenefitsData]]] = {
    integrationFrameworkConnector.getAllStateBenefitsData(taxYear, nino)
  }

  def createOrUpdateStateBenefitDetailOverride(taxYear: Int,
                                               nino: String,
                                               benefitId: UUID,
                                               stateBenefitDetailOverride: StateBenefitDetailOverride)
                                              (implicit hc: HeaderCarrier): Future[Either[ApiError, Unit]] = {
    integrationFrameworkConnector.createOrUpdateStateBenefitDetailOverride(taxYear, nino, benefitId, stateBenefitDetailOverride)
  }

  def deleteStateBenefit(taxYear: Int, nino: String, benefitId: UUID)
                        (implicit hc: HeaderCarrier): Future[Either[ApiError, Unit]] = {
    integrationFrameworkConnector.deleteStateBenefit(taxYear, nino, benefitId)
  }

  def getPriorData(taxYear: Int, nino: String, mtdtid: String)
                  (implicit hc: HeaderCarrier): Future[Either[ApiError, IncomeTaxUserData]] = {
    submissionConnector.getIncomeTaxUserData(taxYear, nino, mtdtid)
  }

  def getStateBenefitsUserData(nino: String, sessionDataId: UUID): Future[Either[ServiceError, StateBenefitsUserData]] = {
    stateBenefitsUserDataRepository.find(nino, sessionDataId)
  }

  def createOrUpdateStateBenefitsUserData(stateBenefitsUserData: StateBenefitsUserData): Future[Either[ServiceError, UUID]] = {
    if (isCreate(stateBenefitsUserData)) deleteAndCreateNew(stateBenefitsUserData) else updateExisting(stateBenefitsUserData)
  }

  private def isCreate(stateBenefitsUserData: StateBenefitsUserData): Boolean =
    stateBenefitsUserData.sessionDataId.isEmpty

  private def deleteAndCreateNew(stateBenefitsUserData: StateBenefitsUserData): Future[Either[ServiceError, UUID]] = {
    stateBenefitsUserDataRepository.clear(stateBenefitsUserData.sessionId).flatMap {
      case false => Future.successful(Left(DataNotUpdatedError))
      case true => stateBenefitsUserDataRepository.createOrUpdate(stateBenefitsUserData)
    }
  }

  private def updateExisting(stateBenefitsUserData: StateBenefitsUserData): Future[Either[ServiceError, UUID]] =
    stateBenefitsUserDataRepository.createOrUpdate(stateBenefitsUserData)
}
