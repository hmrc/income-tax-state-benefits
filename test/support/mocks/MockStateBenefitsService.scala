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

package support.mocks

import models.IncomeTaxUserData
import models.api.AllStateBenefitsData
import models.errors.{ApiServiceError, ServiceError}
import models.mongo.StateBenefitsUserData
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import services.StateBenefitsService
import uk.gov.hmrc.http.HeaderCarrier

import java.util.UUID
import scala.concurrent.Future

trait MockStateBenefitsService extends MockitoSugar {

  protected val mockStateBenefitsService: StateBenefitsService = mock[StateBenefitsService]

  def mockGetAllStateBenefitsData(taxYear: Int, nino: String, result: Either[ServiceError, Option[AllStateBenefitsData]]): Unit =
    when(mockStateBenefitsService.getAllStateBenefitsData(eqTo(taxYear), eqTo(nino))(any[HeaderCarrier]()))
      .thenReturn(Future.successful(result))

  def mockGetAllStateBenefitsDataException(taxYear: Int, nino: String, result: Throwable): Unit =
    when(mockStateBenefitsService.getAllStateBenefitsData(eqTo(taxYear), eqTo(nino))(any[HeaderCarrier]()))
      .thenReturn(Future.failed(result))

  def mockGetPriorData(taxYear: Int, nino: String, mtditid: String, result: Either[ApiServiceError, IncomeTaxUserData]): Unit =
    when(mockStateBenefitsService.getPriorData(eqTo(taxYear), eqTo(nino), eqTo(mtditid))(any[HeaderCarrier]()))
      .thenReturn(Future.successful(result))

  def mockGetStateBenefitsUserData(nino: String, sessionDataId: UUID, result: Either[ServiceError, StateBenefitsUserData]): Unit =
    when(mockStateBenefitsService.getSessionData(eqTo(nino), eqTo(sessionDataId)))
      .thenReturn(Future.successful(result))

  def mockCreateSessionData(stateBenefitsUserData: StateBenefitsUserData, result: Either[ServiceError, UUID]): Unit =
    when(mockStateBenefitsService.createSessionData(eqTo(stateBenefitsUserData)))
      .thenReturn(Future.successful(result))

  def mockUpdateSessionData(stateBenefitsUserData: StateBenefitsUserData, result: Either[ServiceError, UUID]): Unit =
    when(mockStateBenefitsService.updateSessionData(eqTo(stateBenefitsUserData)))
      .thenReturn(Future.successful(result))

  def mockSaveUserData(userData: StateBenefitsUserData, result: Either[ServiceError, Unit]): Unit =
    when(mockStateBenefitsService.saveClaim(eqTo(userData), any[Boolean]())(any[HeaderCarrier]()))
      .thenReturn(Future.successful(result))

  def mockRemoveClaim(nino: String, sessionDataId: UUID, result: Either[ServiceError, Unit]): Unit =
    when(mockStateBenefitsService.removeClaim(eqTo(nino), eqTo(sessionDataId))(any[HeaderCarrier]()))
      .thenReturn(Future.successful(result))

  def mockRemoveClaimById(nino: String, benefitId: UUID, taxYear: Int, mtdItId: String)(result: Either[ServiceError, Unit]): Unit =
    when(mockStateBenefitsService.removeClaimById(eqTo(nino), eqTo(taxYear), eqTo(mtdItId), eqTo(benefitId))(any[HeaderCarrier]()))
      .thenReturn(Future.successful(result))

  def mockRestoreClaim(nino: String, sessionDataId: UUID, result: Either[ServiceError, Unit]): Unit =
    when(mockStateBenefitsService.restoreClaim(eqTo(nino), eqTo(sessionDataId))(any[HeaderCarrier]()))
      .thenReturn(Future.successful(result))
}
