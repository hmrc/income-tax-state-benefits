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
class StateBenefitsService @Inject() (ifService: IntegrationFrameworkService,
                                      submissionService: SubmissionService,
                                      stateBenefitsUserDataRepository: StateBenefitsUserDataRepository)(implicit ec: ExecutionContext) {

  def getAllStateBenefitsData(taxYear: Int, nino: String)(implicit hc: HeaderCarrier): Future[Either[ServiceError, Option[AllStateBenefitsData]]] =
    ifService.getAllStateBenefitsData(taxYear, nino)

  def getPriorData(taxYear: Int, nino: String, mtdtid: String)(implicit hc: HeaderCarrier): Future[Either[ApiServiceError, IncomeTaxUserData]] =
    submissionService.getIncomeTaxUserData(taxYear, nino, mtdtid)

  def getSessionData(nino: String, sessionDataId: UUID): Future[Either[ServiceError, StateBenefitsUserData]] =
    findSessionData(nino, sessionDataId).value

  def createSessionData(stateBenefitsUserData: StateBenefitsUserData): Future[Either[ServiceError, UUID]] = {
    val response = for {
      _      <- clearSessionData(stateBenefitsUserData)
      result <- createOrUpdateUserSessionData(stateBenefitsUserData)
    } yield result
    response.value
  }

  def updateSessionData(userData: StateBenefitsUserData): Future[Either[ServiceError, UUID]] =
    createOrUpdateUserSessionData(userData).value

  def saveClaim(stateBenefitsUserData: StateBenefitsUserData, useSessionData: Boolean = true)(implicit
      hc: HeaderCarrier): Future[Either[ServiceError, Unit]] = {
    val response = for {
      data <-
        if (useSessionData) {
          findSessionData(stateBenefitsUserData.nino, stateBenefitsUserData.sessionDataId.get)
        } else { EitherT(Future(Either.cond(test = true, stateBenefitsUserData, ApiServiceError("saveClaim with StateBenefitsUserData")))) }
      _ <- saveStateBenefitsUserData(data)
      _ <- refreshSubmissionServiceData(data)
      _ <- clearSessionData(data)
    } yield ()
    response.value
  }

  def removeClaim(nino: String, sessionDataId: UUID)(implicit hc: HeaderCarrier): Future[Either[ServiceError, Unit]] = {
    val response = for {
      userData <- findSessionData(nino, sessionDataId)
      _        <- removeStateBenefitUserData(userData)
      _        <- refreshSubmissionServiceData(userData)
      _        <- clearSessionData(userData)
    } yield ()
    response.value
  }

  def removeClaimById(nino: String, taxYear: Int, mtdItId: String, benefitId: UUID)(implicit hc: HeaderCarrier): Future[Either[ServiceError, Unit]] =
    (for {
      _ <- EitherT(ifService.removeClaim(nino, taxYear, benefitId))
      _ <- EitherT(submissionService.refreshStateBenefits(taxYear, nino, mtdItId))
    } yield ()).value

  def restoreClaim(nino: String, sessionDataId: UUID)(implicit hc: HeaderCarrier): Future[Either[ServiceError, Unit]] = {
    val response = for {
      userData <- findSessionData(nino, sessionDataId)
      _        <- unIgnoreClaim(userData)
      _        <- refreshSubmissionServiceData(userData)
      _        <- clearSessionData(userData)
    } yield ()
    response.value
  }

  private def findSessionData(nino: String, sessionDataId: UUID): EitherT[Future, ServiceError, StateBenefitsUserData] =
    EitherT(stateBenefitsUserDataRepository.find(nino, sessionDataId))

  private def refreshSubmissionServiceData(userData: StateBenefitsUserData)(implicit hc: HeaderCarrier): EitherT[Future, ApiServiceError, Unit] =
    EitherT(submissionService.refreshStateBenefits(userData.taxYear, userData.nino, userData.mtdItId))

  private def removeStateBenefitUserData(userData: StateBenefitsUserData)(implicit hc: HeaderCarrier): EitherT[Future, ServiceError, Unit] =
    userData match {
      case _ if userData.isNewClaim                             => EitherT(Future.successful[Either[ServiceError, Unit]](Right(())))
      case _ if userData.isCustomerAdded || userData.isHmrcData => EitherT(ifService.removeOrIgnoreClaim(userData))
      case _                                                    => EitherT(ifService.removeCustomerOverride(userData))
    }

  private def unIgnoreClaim(userData: StateBenefitsUserData)(implicit hc: HeaderCarrier): EitherT[Future, ApiServiceError, Unit] =
    EitherT(ifService.unIgnoreClaim(userData))

  private def saveStateBenefitsUserData(userData: StateBenefitsUserData)(implicit hc: HeaderCarrier): EitherT[Future, ApiServiceError, UUID] =
    EitherT(ifService.saveStateBenefitsUserData(userData))

  private def clearSessionData(userData: StateBenefitsUserData): EitherT[Future, ServiceError, Unit] =
    EitherT(stateBenefitsUserDataRepository.clear(userData.sessionId))

  private def createOrUpdateUserSessionData(stateBenefitsUserData: StateBenefitsUserData): EitherT[Future, ServiceError, UUID] =
    EitherT(stateBenefitsUserDataRepository.createOrUpdate(stateBenefitsUserData))
}
