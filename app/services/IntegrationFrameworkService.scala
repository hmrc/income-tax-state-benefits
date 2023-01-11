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

import connectors.IntegrationFrameworkConnector
import connectors.errors.ApiError
import models.api.{AddStateBenefit, AllStateBenefitsData, StateBenefitDetailOverride, UpdateStateBenefit}
import models.errors.ApiServiceError
import models.mongo.{ClaimCYAModel, StateBenefitsUserData}
import uk.gov.hmrc.http.HeaderCarrier

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class IntegrationFrameworkService @Inject()(integrationFrameworkConnector: IntegrationFrameworkConnector)
                                           (implicit ec: ExecutionContext) {

  def getAllStateBenefitsData(taxYear: Int, nino: String)
                             (implicit hc: HeaderCarrier): Future[Either[ApiServiceError, Option[AllStateBenefitsData]]] = {
    integrationFrameworkConnector.getAllStateBenefitsData(taxYear, nino).map {
      case Left(error) => Left(ApiServiceError(error.status.toString))
      case Right(allStateBenefitsData) => Right(allStateBenefitsData)
    }
  }

  def createOrUpdateStateBenefit(userData: StateBenefitsUserData)
                                (implicit hc: HeaderCarrier): Future[Either[ApiServiceError, Unit]] = {
    val claimData = userData.claim.get
    createOrUpdateStateBenefit(userData, claimData).flatMap {
      case Left(error) => Future.successful(Left(ApiServiceError(error.status.toString)))
      case Right(benefitId: UUID) => handleCreateOrUpdateBenefitDetailsOverride(userData, benefitId)
    }
  }

  def removeOrIgnoreClaim(userData: StateBenefitsUserData, benefitId: UUID)
                         (implicit hc: HeaderCarrier): Future[Either[ApiServiceError, Unit]] = {
    val eventualIgnoreOrDelete = if (userData.isHmrcData) {
      integrationFrameworkConnector.ignoreStateBenefit(userData.taxYear, userData.nino, benefitId)
    } else {
      integrationFrameworkConnector.deleteStateBenefit(userData.taxYear, userData.nino, benefitId)
    }
    eventualIgnoreOrDelete.map {
      case Left(error) => Left(ApiServiceError(error.status.toString))
      case Right(_) => Right(())
    }
  }

  def unIgnoreClaim(userData: StateBenefitsUserData)
                   (implicit hc: HeaderCarrier): Future[Either[ApiServiceError, Unit]] = {
    val benefitId = userData.claim.get.benefitId.get
    integrationFrameworkConnector.unIgnoreStateBenefit(userData.taxYear, userData.nino, benefitId).map {
      case Left(error) => Left(ApiServiceError(error.status.toString))
      case Right(_) => Right(())
    }
  }

  private def createOrUpdateStateBenefit(userData: StateBenefitsUserData, claimData: ClaimCYAModel)
                                        (implicit hc: HeaderCarrier): Future[Either[ApiError, UUID]] = if (userData.isNewClaim) {
    integrationFrameworkConnector.addStateBenefit(userData.taxYear, userData.nino, AddStateBenefit(userData.benefitType, claimData))
  } else {
    val benefitId = claimData.benefitId.get
    integrationFrameworkConnector.updateStateBenefit(userData.taxYear, userData.nino, benefitId, UpdateStateBenefit(claimData)).map {
      case Left(error) => Left(error)
      case Right(_) => Right(benefitId)
    }
  }

  private def handleCreateOrUpdateBenefitDetailsOverride(userData: StateBenefitsUserData, benefitId: UUID)
                                                        (implicit hc: HeaderCarrier): Future[Either[ApiServiceError, Unit]] = {
    val claimData = userData.claim.get
    val detailOverride = StateBenefitDetailOverride(claimData.amount.get, claimData.taxPaid)
    integrationFrameworkConnector.createOrUpdateStateBenefitDetailOverride(userData.taxYear, userData.nino, benefitId, detailOverride).map {
      case Left(error) => Left(ApiServiceError(error.status.toString))
      case Right(_) => Right(())
    }
  }
}
