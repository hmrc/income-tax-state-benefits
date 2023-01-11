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
import org.scalamock.handlers.{CallHandler3, CallHandler4, CallHandler5}
import org.scalamock.scalatest.MockFactory
import uk.gov.hmrc.http.HeaderCarrier

import java.util.UUID
import scala.concurrent.Future

trait MockIntegrationFrameworkConnector extends MockFactory {

  protected val mockIntegrationFrameworkConnector: IntegrationFrameworkConnector = mock[IntegrationFrameworkConnector]

  def mockGetAllStateBenefitsData(taxYear: Int,
                                  nino: String,
                                  result: Either[ApiError, Option[AllStateBenefitsData]])
  : CallHandler3[Int, String, HeaderCarrier, Future[Either[ApiError, Option[AllStateBenefitsData]]]] = {
    (mockIntegrationFrameworkConnector.getAllStateBenefitsData(_: Int, _: String)(_: HeaderCarrier))
      .expects(taxYear, nino, *)
      .returning(Future.successful(result))
  }

  def mockAddStateBenefit(taxYear: Int,
                          nino: String,
                          addStateBenefit: AddStateBenefit,
                          result: Either[ApiError, UUID]): CallHandler4[Int, String, AddStateBenefit, HeaderCarrier, Future[Either[ApiError, UUID]]] = {
    (mockIntegrationFrameworkConnector.addStateBenefit(_: Int, _: String, _: AddStateBenefit)(_: HeaderCarrier))
      .expects(taxYear, nino, addStateBenefit, *)
      .returning(Future.successful(result))
  }

  def mockUpdateStateBenefit(taxYear: Int,
                             nino: String,
                             benefitId: UUID,
                             updateStateBenefit: UpdateStateBenefit,
                             result: Either[ApiError, Unit])
  : CallHandler5[Int, String, UUID, UpdateStateBenefit, HeaderCarrier, Future[Either[ApiError, Unit]]] = {
    (mockIntegrationFrameworkConnector.updateStateBenefit(_: Int, _: String, _: UUID, _: UpdateStateBenefit)(_: HeaderCarrier))
      .expects(taxYear, nino, benefitId, updateStateBenefit, *)
      .returning(Future.successful(result))
  }

  def mockCreateOrUpdateStateBenefitDetailOverride(taxYear: Int,
                                                   nino: String,
                                                   benefitId: UUID,
                                                   stateBenefitDetailOverride: StateBenefitDetailOverride,
                                                   result: Either[ApiError, Unit])
  : CallHandler5[Int, String, UUID, StateBenefitDetailOverride, HeaderCarrier, Future[Either[ApiError, Unit]]] = {
    (mockIntegrationFrameworkConnector.createOrUpdateStateBenefitDetailOverride(_: Int, _: String, _: UUID, _: StateBenefitDetailOverride)(_: HeaderCarrier))
      .expects(taxYear, nino, benefitId, stateBenefitDetailOverride, *)
      .returning(Future.successful(result))
  }

  def mockDeleteStateBenefit(taxYear: Int,
                             nino: String,
                             benefitId: UUID,
                             result: Either[ApiError, Unit]): CallHandler4[Int, String, UUID, HeaderCarrier, Future[Either[ApiError, Unit]]] = {
    (mockIntegrationFrameworkConnector.deleteStateBenefit(_: Int, _: String, _: UUID)(_: HeaderCarrier))
      .expects(taxYear, nino, benefitId, *)
      .returning(Future.successful(result))
  }

  def mockIgnoreStateBenefit(taxYear: Int,
                             nino: String,
                             benefitId: UUID,
                             result: Either[ApiError, Unit]): CallHandler4[Int, String, UUID, HeaderCarrier, Future[Either[ApiError, Unit]]] = {
    (mockIntegrationFrameworkConnector.ignoreStateBenefit(_: Int, _: String, _: UUID)(_: HeaderCarrier))
      .expects(taxYear, nino, benefitId, *)
      .returning(Future.successful(result))
  }

  def mockUnIgnoreStateBenefit(taxYear: Int,
                               nino: String,
                               benefitId: UUID,
                               result: Either[ApiError, Unit]): CallHandler4[Int, String, UUID, HeaderCarrier, Future[Either[ApiError, Unit]]] = {
    (mockIntegrationFrameworkConnector.unIgnoreStateBenefit(_: Int, _: String, _: UUID)(_: HeaderCarrier))
      .expects(taxYear, nino, benefitId, *)
      .returning(Future.successful(result))
  }
}
