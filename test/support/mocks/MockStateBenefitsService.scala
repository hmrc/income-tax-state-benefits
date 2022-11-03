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

import connectors.errors.ApiError
import models.IncomeTaxUserData
import models.api.AllStateBenefitsData
import models.errors.ServiceError
import models.mongo.StateBenefitsUserData
import org.scalamock.handlers.{CallHandler1, CallHandler3, CallHandler4}
import org.scalamock.scalatest.MockFactory
import services.StateBenefitsService
import uk.gov.hmrc.http.HeaderCarrier

import java.util.UUID
import scala.concurrent.Future

trait MockStateBenefitsService extends MockFactory {

  protected val mockStateBenefitsService: StateBenefitsService = mock[StateBenefitsService]

  def mockGetAllStateBenefitsData(taxYear: Int,
                                  nino: String,
                                  result: Either[ApiError, Option[AllStateBenefitsData]]
                                 ): CallHandler3[Int, String, HeaderCarrier, Future[Either[ApiError, Option[AllStateBenefitsData]]]] = {
    (mockStateBenefitsService.getAllStateBenefitsData(_: Int, _: String)(_: HeaderCarrier))
      .expects(taxYear, nino, *)
      .returning(Future.successful(result))
  }

  def mockGetPriorData(taxYear: Int,
                       nino: String,
                       mtditid: String,
                       result: Either[ApiError, IncomeTaxUserData]
                      ): CallHandler4[Int, String, String, HeaderCarrier, Future[Either[ApiError, IncomeTaxUserData]]] = {
    (mockStateBenefitsService.getPriorData(_: Int, _: String, _: String)(_: HeaderCarrier))
      .expects(taxYear, nino, mtditid, *)
      .returning(Future.successful(result))
  }

  def mockGetStateBenefitsUserData(sessionDataId: UUID,
                                   result: Either[ServiceError, StateBenefitsUserData]
                                  ): CallHandler1[UUID, Future[Either[ServiceError, StateBenefitsUserData]]] = {
    (mockStateBenefitsService.getStateBenefitsUserData(_: UUID))
      .expects(sessionDataId)
      .returning(Future.successful(result))
  }

  def mockCreateOrUpdateStateBenefitsUserData(stateBenefitsUserData: StateBenefitsUserData,
                                              result: Either[ServiceError, UUID]): CallHandler1[StateBenefitsUserData, Future[Either[ServiceError, UUID]]] = {
    (mockStateBenefitsService.createOrUpdateStateBenefitsUserData(_: StateBenefitsUserData))
      .expects(stateBenefitsUserData)
      .returning(Future.successful(result))
  }
}
