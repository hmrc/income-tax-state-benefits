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

import connectors.IntegrationFrameworkConnector
import connectors.errors.ApiError
import models.api.{AddStateBenefit, AllStateBenefitsData, StateBenefitDetailOverride, UpdateStateBenefit}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.http.HeaderCarrier

import java.util.UUID
import scala.concurrent.Future

trait MockIntegrationFrameworkConnector extends MockitoSugar {

  protected val mockIntegrationFrameworkConnector: IntegrationFrameworkConnector = mock[IntegrationFrameworkConnector]

  def mockGetAllStateBenefitsData(taxYear: Int,
                                  nino: String,
                                  result: Either[ApiError, Option[AllStateBenefitsData]]): Unit =
    when(mockIntegrationFrameworkConnector.getAllStateBenefitsData(eqTo(taxYear), eqTo(nino))(any[HeaderCarrier]()))
      .thenReturn(Future.successful(result))

  def mockAddCustomerStateBenefit(taxYear: Int,
                                  nino: String,
                                  addStateBenefit: AddStateBenefit,
                                  result: Either[ApiError, UUID]): Unit =
    when(mockIntegrationFrameworkConnector.addCustomerStateBenefit(eqTo(taxYear), eqTo(nino), eqTo(addStateBenefit))(any[HeaderCarrier]()))
      .thenReturn(Future.successful(result))

  def mockUpdateCustomerStateBenefit(taxYear: Int,
                                     nino: String,
                                     benefitId: UUID,
                                     updateStateBenefit: UpdateStateBenefit,
                                     result: Either[ApiError, Unit]): Unit =
    when(mockIntegrationFrameworkConnector.updateCustomerStateBenefit(eqTo(taxYear), eqTo(nino), eqTo(benefitId), eqTo(updateStateBenefit))(any[HeaderCarrier]()))
      .thenReturn(Future.successful(result))

  def mockCreateOrUpdateStateBenefitDetailOverride(taxYear: Int,
                                                   nino: String,
                                                   benefitId: UUID,
                                                   stateBenefitDetailOverride: StateBenefitDetailOverride,
                                                   result: Either[ApiError, Unit]): Unit =
    when(mockIntegrationFrameworkConnector.createOrUpdateStateBenefitDetailOverride(eqTo(taxYear), eqTo(nino), eqTo(benefitId), eqTo(stateBenefitDetailOverride))(any[HeaderCarrier]()))
      .thenReturn(Future.successful(result))

  def mockDeleteStateBenefit(taxYear: Int,
                             nino: String,
                             benefitId: UUID,
                             result: Either[ApiError, Unit]): Unit =
    when(mockIntegrationFrameworkConnector.deleteStateBenefit(eqTo(taxYear), eqTo(nino), eqTo(benefitId))(any[HeaderCarrier]()))
      .thenReturn(Future.successful(result))

  def mockIgnoreStateBenefit(taxYear: Int,
                             nino: String,
                             benefitId: UUID,
                             result: Either[ApiError, Unit]): Unit =
    when(mockIntegrationFrameworkConnector.ignoreStateBenefit(eqTo(taxYear), eqTo(nino), eqTo(benefitId))(any[HeaderCarrier]()))
      .thenReturn(Future.successful(result))

  def mockUnIgnoreStateBenefit(taxYear: Int,
                               nino: String,
                               benefitId: UUID,
                               result: Either[ApiError, Unit]): Unit =
    when(mockIntegrationFrameworkConnector.unIgnoreStateBenefit(eqTo(taxYear), eqTo(nino), eqTo(benefitId))(any[HeaderCarrier]()))
      .thenReturn(Future.successful(result))

  def mockDeleteStateBenefitDetailOverride(taxYear: Int,
                                           nino: String,
                                           benefitId: UUID,
                                           result: Either[ApiError, Unit]): Unit =
    when(mockIntegrationFrameworkConnector.deleteStateBenefitDetailOverride(eqTo(taxYear), eqTo(nino), eqTo(benefitId))(any[HeaderCarrier]()))
      .thenReturn(Future.successful(result))
}
