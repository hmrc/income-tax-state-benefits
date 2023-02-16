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

import cats.data.EitherT
import models.IncomeTaxUserData
import models.api.AllStateBenefitsData
import models.errors.{ApiServiceError, ServiceError}
import models.mongo.StateBenefitsUserData
import repositories.StateBenefitsUserDataRepository
import uk.gov.hmrc.http.HeaderCarrier

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class StateBenefitsService @Inject()(ifService: IntegrationFrameworkService,
                                     submissionService: SubmissionService,
                                     stateBenefitsUserDataRepository: StateBenefitsUserDataRepository)
                                    (implicit ec: ExecutionContext) {

  def getAllStateBenefitsData(taxYear: Int, nino: String)
                             (implicit hc: HeaderCarrier): Future[Either[ServiceError, Option[AllStateBenefitsData]]] = {
    ifService.getAllStateBenefitsData(taxYear, nino)
  }

  def getPriorData(taxYear: Int, nino: String, mtdtid: String)
                  (implicit hc: HeaderCarrier): Future[Either[ApiServiceError, IncomeTaxUserData]] = {
    submissionService.getIncomeTaxUserData(taxYear, nino, mtdtid)
  }

  def getSessionData(nino: String, sessionDataId: UUID): Future[Either[ServiceError, StateBenefitsUserData]] = {
    findSessionData(nino, sessionDataId).value
  }

  def saveUserData(userData: StateBenefitsUserData)
                  (implicit hc: HeaderCarrier): Future[Either[ServiceError, Unit]] = {
    val response = for {
      data <- findSessionData(userData.nino, userData.sessionDataId.get)
      _ <- createOrUpdateStateBenefit(data)
      _ <- refreshSubmissionServiceData(data)
      result <- clearSessionData(data)
    } yield result
    response.value
  }

  def removeClaim(nino: String, sessionDataId: UUID)
                 (implicit hc: HeaderCarrier): Future[Either[ServiceError, Unit]] = {
    val response = for {
      userData <- findSessionData(nino, sessionDataId)
      result <- handleClaimRemovalFor(userData)
    } yield result
    response.value
  }

  def restoreClaim(nino: String, sessionDataId: UUID)
                  (implicit hc: HeaderCarrier): Future[Either[ServiceError, Unit]] = {
    val response = for {
      userData <- findSessionData(nino, sessionDataId)
      _ <- unIgnoreClaim(userData)
      _ <- refreshSubmissionServiceData(userData)
      result <- clearSessionData(userData)
    } yield result
    response.value
  }

  def createSessionData(userData: StateBenefitsUserData): Future[Either[ServiceError, UUID]] =
    deleteAndCreateNewSessionData(userData).value

  def updateSessionData(userData: StateBenefitsUserData): Future[Either[ServiceError, UUID]] =
    updateExistingSessionData(userData).value

  private def deleteAndCreateNewSessionData(stateBenefitsUserData: StateBenefitsUserData): EitherT[Future, ServiceError, UUID] = for {
    _ <- clearSessionData(stateBenefitsUserData)
    result <- createOrUpdateUserSessionData(stateBenefitsUserData)
  } yield result

  private def updateExistingSessionData(stateBenefitsUserData: StateBenefitsUserData): EitherT[Future, ServiceError, UUID] =
    createOrUpdateUserSessionData(stateBenefitsUserData)

  private def handleClaimRemovalFor(userData: StateBenefitsUserData)
                                   (implicit hc: HeaderCarrier): EitherT[Future, ServiceError, Unit] =
    if (userData.isPriorSubmission) {
      val benefitId = userData.claim.flatMap(_.benefitId).get
      for {
        _ <- removeOrIgnoreClaim(userData, benefitId)
        _ <- refreshSubmissionServiceData(userData)
        result <- clearSessionData(userData)
      } yield result
    } else {
      clearSessionData(userData)
    }

  private def findSessionData(nino: String, sessionDataId: UUID): EitherT[Future, ServiceError, StateBenefitsUserData] =
    EitherT(stateBenefitsUserDataRepository.find(nino, sessionDataId))

  private def refreshSubmissionServiceData(userData: StateBenefitsUserData)
                                          (implicit hc: HeaderCarrier): EitherT[Future, ApiServiceError, Unit] =
    EitherT(submissionService.refreshStateBenefits(userData.taxYear, userData.nino, userData.mtdItId))

  private def removeOrIgnoreClaim(userData: StateBenefitsUserData, benefitId: UUID)
                                 (implicit hc: HeaderCarrier): EitherT[Future, ApiServiceError, Unit] =
    EitherT(ifService.removeOrIgnoreClaim(userData, benefitId))

  private def unIgnoreClaim(userData: StateBenefitsUserData)
                           (implicit hc: HeaderCarrier): EitherT[Future, ApiServiceError, Unit] =
    EitherT(ifService.unIgnoreClaim(userData))

  private def createOrUpdateStateBenefit(userData: StateBenefitsUserData)
                                        (implicit hc: HeaderCarrier): EitherT[Future, ApiServiceError, Unit] =
    EitherT(ifService.createOrUpdateStateBenefit(userData))

  private def clearSessionData(userData: StateBenefitsUserData): EitherT[Future, ServiceError, Unit] =
    EitherT(stateBenefitsUserDataRepository.clear(userData.sessionId))

  private def createOrUpdateUserSessionData(stateBenefitsUserData: StateBenefitsUserData): EitherT[Future, ServiceError, UUID] =
    EitherT(stateBenefitsUserDataRepository.createOrUpdate(stateBenefitsUserData))
}
