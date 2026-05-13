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

import models.api.AllStateBenefitsData
import models.errors.ApiServiceError
import models.mongo.StateBenefitsUserData
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import services.IntegrationFrameworkService
import uk.gov.hmrc.http.HeaderCarrier

import java.util.UUID
import scala.concurrent.Future

trait MockIntegrationFrameworkService extends MockitoSugar {

  protected val mockIntegrationFrameworkService: IntegrationFrameworkService = mock[IntegrationFrameworkService]

  def mockGetAllStateBenefitsData(taxYear: Int, nino: String, result: Either[ApiServiceError, Option[AllStateBenefitsData]]): Unit =
    when(mockIntegrationFrameworkService.getAllStateBenefitsData(eqTo(taxYear), eqTo(nino))(any[HeaderCarrier]()))
      .thenReturn(Future.successful(result))

  def mockSaveStateBenefitsUserData(userData: StateBenefitsUserData, result: Either[ApiServiceError, UUID]): Unit =
    when(mockIntegrationFrameworkService.saveStateBenefitsUserData(eqTo(userData))(any[HeaderCarrier]()))
      .thenReturn(Future.successful(result))

  def mockRemoveClaim(nino: String, taxYear: Int, benefitId: UUID)(result: Either[ApiServiceError, Unit]): Unit =
    when(mockIntegrationFrameworkService.removeClaim(eqTo(nino), eqTo(taxYear), eqTo(benefitId))(any[HeaderCarrier]()))
      .thenReturn(Future.successful(result))

  def mockRemoveOrIgnoreClaim(userData: StateBenefitsUserData, result: Either[ApiServiceError, Unit]): Unit =
    when(mockIntegrationFrameworkService.removeOrIgnoreClaim(eqTo(userData))(any[HeaderCarrier]()))
      .thenReturn(Future.successful(result))

  def mockUnIgnoreClaim(userData: StateBenefitsUserData, result: Either[ApiServiceError, Unit]): Unit =
    when(mockIntegrationFrameworkService.unIgnoreClaim(eqTo(userData))(any[HeaderCarrier]()))
      .thenReturn(Future.successful(result))

  def mockRemoveCustomerOverride(userData: StateBenefitsUserData, result: Either[ApiServiceError, Unit]): Unit =
    when(mockIntegrationFrameworkService.removeCustomerOverride(eqTo(userData))(any[HeaderCarrier]()))
      .thenReturn(Future.successful(result))
}
