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
import connectors.IntegrationFrameworkConnector
import models.api.{AddStateBenefit, AllStateBenefitsData, StateBenefitDetailOverride, UpdateStateBenefit}
import models.errors.ApiServiceError
import models.mongo.StateBenefitsUserData
import uk.gov.hmrc.http.HeaderCarrier

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class IntegrationFrameworkService @Inject()(connector: IntegrationFrameworkConnector)
                                           (implicit ec: ExecutionContext) {

  def getAllStateBenefitsData(taxYear: Int, nino: String)
                             (implicit hc: HeaderCarrier): Future[Either[ApiServiceError, Option[AllStateBenefitsData]]] = {
    connector.getAllStateBenefitsData(taxYear, nino).map {
      case Left(error) => Left(ApiServiceError(error.status.toString))
      case Right(allStateBenefitsData) => Right(allStateBenefitsData)
    }
  }

  def saveStateBenefitsUserData(stateBenefitsUserData: StateBenefitsUserData)
                               (implicit hc: HeaderCarrier): Future[Either[ApiServiceError, UUID]] = {
    val response = for {
      benefitId <- createOrUpdateStateBenefit(stateBenefitsUserData)
      result <- createOrUpdateBenefitDetails(stateBenefitsUserData, benefitId)
    } yield result
    response.value
  }

  def removeOrIgnoreClaim(stateBenefitsUserData: StateBenefitsUserData)
                         (implicit hc: HeaderCarrier): Future[Either[ApiServiceError, Unit]] = {
    val benefitId = stateBenefitsUserData.claim.get.benefitId.get
    val eventualIgnoreOrDelete = if (stateBenefitsUserData.isHmrcData) {
      connector.ignoreStateBenefit(stateBenefitsUserData.taxYear, stateBenefitsUserData.nino, benefitId)
    } else {
      connector.deleteStateBenefit(stateBenefitsUserData.taxYear, stateBenefitsUserData.nino, benefitId)
    }

    eventualIgnoreOrDelete.map {
      case Left(error) => Left(ApiServiceError(error.status.toString))
      case Right(_) => Right(())
    }
  }

  def unIgnoreClaim(stateBenefitsUserData: StateBenefitsUserData)
                   (implicit hc: HeaderCarrier): Future[Either[ApiServiceError, Unit]] = {
    val benefitId = stateBenefitsUserData.claim.get.benefitId.get
    connector.unIgnoreStateBenefit(stateBenefitsUserData.taxYear, stateBenefitsUserData.nino, benefitId).map {
      case Left(error) => Left(ApiServiceError(error.status.toString))
      case Right(_) => Right(())
    }
  }

  private def createOrUpdateStateBenefit(userData: StateBenefitsUserData)
                                        (implicit hc: HeaderCarrier): EitherT[Future, ApiServiceError, UUID] = userData match {
    case _ if userData.isHmrcData => EitherT(Future.successful[Either[ApiServiceError, UUID]](Right(userData.claim.get.benefitId.get)))
    case _ if userData.isNewClaim => addCustomerStateBenefit(userData)
    case _ => updateCustomerStateBenefit(userData)
  }

  private def createOrUpdateBenefitDetails(userData: StateBenefitsUserData, benefitId: UUID)
                                          (implicit hc: HeaderCarrier): EitherT[Future, ApiServiceError, UUID] = EitherT {
    val claimData = userData.claim.get
    val detailOverride = StateBenefitDetailOverride(claimData.amount.get, claimData.taxPaid)
    connector.createOrUpdateStateBenefitDetailOverride(userData.taxYear, userData.nino, benefitId, detailOverride).map {
      case Left(error) => Left(ApiServiceError(error.status.toString))
      case Right(_) => Right(benefitId)
    }
  }

  private def updateCustomerStateBenefit(userData: StateBenefitsUserData)
                                        (implicit hc: HeaderCarrier): EitherT[Future, ApiServiceError, UUID] = EitherT {
    val claimData = userData.claim.get
    val benefitId = claimData.benefitId.get
    connector.updateCustomerStateBenefit(userData.taxYear, userData.nino, benefitId, UpdateStateBenefit(claimData)).map {
      case Left(error) => Left(ApiServiceError(error.status.toString))
      case Right(_) => Right(benefitId)
    }
  }

  private def addCustomerStateBenefit(userData: StateBenefitsUserData)
                                     (implicit hc: HeaderCarrier): EitherT[Future, ApiServiceError, UUID] = EitherT {
    connector.addCustomerStateBenefit(userData.taxYear, userData.nino, AddStateBenefit(userData.benefitType, userData.claim.get)).map {
      case Left(error) => Left(ApiServiceError(error.status.toString))
      case Right(benefitId) => Right(benefitId)
    }
  }
}
