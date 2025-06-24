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
import org.scalamock.handlers._
import org.scalamock.scalatest.MockFactory
import org.scalatest.TestSuite
import services.IntegrationFrameworkService
import uk.gov.hmrc.http.HeaderCarrier

import java.util.UUID
import scala.concurrent.Future

trait MockIntegrationFrameworkService extends MockFactory { _: TestSuite =>

  protected val mockIntegrationFrameworkService: IntegrationFrameworkService = mock[IntegrationFrameworkService]

  def mockGetAllStateBenefitsData(taxYear: Int, nino: String, result: Either[ApiServiceError, Option[AllStateBenefitsData]])
      : CallHandler3[Int, String, HeaderCarrier, Future[Either[ApiServiceError, Option[AllStateBenefitsData]]]] =
    (mockIntegrationFrameworkService
      .getAllStateBenefitsData(_: Int, _: String)(_: HeaderCarrier))
      .expects(taxYear, nino, *)
      .returning(Future.successful(result))

  def mockSaveStateBenefitsUserData(
      userData: StateBenefitsUserData,
      result: Either[ApiServiceError, UUID]): CallHandler2[StateBenefitsUserData, HeaderCarrier, Future[Either[ApiServiceError, UUID]]] =
    (mockIntegrationFrameworkService
      .saveStateBenefitsUserData(_: StateBenefitsUserData)(_: HeaderCarrier))
      .expects(userData, *)
      .returning(Future.successful(result))

  def mockRemoveClaim(nino: String, taxYear: Int, benefitId: UUID)(
      result: Either[ApiServiceError, Unit]): CallHandler4[String, Int, UUID, HeaderCarrier, Future[Either[ApiServiceError, Unit]]] =
    (mockIntegrationFrameworkService
      .removeClaim(_: String, _: Int, _: UUID)(_: HeaderCarrier))
      .expects(nino, taxYear, benefitId, *)
      .returning(Future.successful(result))

  def mockRemoveOrIgnoreClaim(
      userData: StateBenefitsUserData,
      result: Either[ApiServiceError, Unit]): CallHandler2[StateBenefitsUserData, HeaderCarrier, Future[Either[ApiServiceError, Unit]]] =
    (mockIntegrationFrameworkService
      .removeOrIgnoreClaim(_: StateBenefitsUserData)(_: HeaderCarrier))
      .expects(userData, *)
      .returning(Future.successful(result))

  def mockUnIgnoreClaim(
      userData: StateBenefitsUserData,
      result: Either[ApiServiceError, Unit]): CallHandler2[StateBenefitsUserData, HeaderCarrier, Future[Either[ApiServiceError, Unit]]] =
    (mockIntegrationFrameworkService
      .unIgnoreClaim(_: StateBenefitsUserData)(_: HeaderCarrier))
      .expects(userData, *)
      .returning(Future.successful(result))

  def mockRemoveCustomerOverride(userData: StateBenefitsUserData,
                                 result: Either[ApiServiceError, Unit]
                                ): CallHandler2[StateBenefitsUserData, HeaderCarrier, Future[Either[ApiServiceError, Unit]]] = {
    (mockIntegrationFrameworkService.removeCustomerOverride(_: StateBenefitsUserData)(_: HeaderCarrier))
      .expects(userData, *)
      .returning(Future.successful(result))
  }

}
