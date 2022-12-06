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

package support.mocks

import models.api.AllStateBenefitsData
import models.errors.ApiServiceError
import models.mongo.StateBenefitsUserData
import org.scalamock.handlers._
import org.scalamock.scalatest.MockFactory
import services.IntegrationFrameworkService
import uk.gov.hmrc.http.HeaderCarrier

import java.util.UUID
import scala.concurrent.Future

trait MockIntegrationFrameworkService extends MockFactory {

  protected val mockIntegrationFrameworkService: IntegrationFrameworkService = mock[IntegrationFrameworkService]

  def mockGetAllStateBenefitsData(taxYear: Int,
                                  nino: String,
                                  result: Either[ApiServiceError, Option[AllStateBenefitsData]]
                                 ): CallHandler3[Int, String, HeaderCarrier, Future[Either[ApiServiceError, Option[AllStateBenefitsData]]]] = {
    (mockIntegrationFrameworkService.getAllStateBenefitsData(_: Int, _: String)(_: HeaderCarrier))
      .expects(taxYear, nino, *)
      .returning(Future.successful(result))
  }

  def mockCreateOrUpdateStateBenefit(userData: StateBenefitsUserData,
                                     result: Either[ApiServiceError, Unit])
  : CallHandler2[StateBenefitsUserData, HeaderCarrier, Future[Either[ApiServiceError, Unit]]] = {
    (mockIntegrationFrameworkService.createOrUpdateStateBenefit(_: StateBenefitsUserData)(_: HeaderCarrier))
      .expects(userData, *)
      .returning(Future.successful(result))
  }

  def mockRemoveOrIgnoreClaim(userData: StateBenefitsUserData,
                              benefitId: UUID,
                              result: Either[ApiServiceError, Unit])
  : CallHandler3[StateBenefitsUserData, UUID, HeaderCarrier, Future[Either[ApiServiceError, Unit]]] = {
    (mockIntegrationFrameworkService.removeOrIgnoreClaim(_: StateBenefitsUserData, _: UUID)(_: HeaderCarrier))
      .expects(userData, benefitId, *)
      .returning(Future.successful(result))
  }
}
