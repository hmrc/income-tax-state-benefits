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
import models.api.{AllStateBenefitsData, StateBenefitDetailOverride}
import models.errors.ServiceError
import models.mongo.StateBenefitsUserData
import org.scalamock.handlers._
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

  def mockCreateOrUpdateStateBenefitDetailOverride(taxYear: Int,
                                                   nino: String,
                                                   benefitId: UUID,
                                                   stateBenefitDetailOverride: StateBenefitDetailOverride,
                                                   result: Either[ApiError, Unit]
                                                  ): CallHandler5[Int, String, UUID, StateBenefitDetailOverride, HeaderCarrier, Future[Either[ApiError, Unit]]] = {
    (mockStateBenefitsService.createOrUpdateStateBenefitDetailOverride(_: Int, _: String, _: UUID, _: StateBenefitDetailOverride)(_: HeaderCarrier))
      .expects(taxYear, nino, benefitId, stateBenefitDetailOverride, *)
      .returning(Future.successful(result))
  }

  def mockDeleteStateBenefit(taxYear: Int,
                             nino: String,
                             benefitId: UUID,
                             result: Either[ApiError, Unit]): CallHandler4[Int, String, UUID, HeaderCarrier, Future[Either[ApiError, Unit]]] = {
    (mockStateBenefitsService.deleteStateBenefit(_: Int, _: String, _: UUID)(_: HeaderCarrier))
      .expects(taxYear, nino, benefitId, *)
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

  def mockGetStateBenefitsUserData(nino: String,
                                   sessionDataId: UUID,
                                   result: Either[ServiceError, StateBenefitsUserData]
                                  ): CallHandler2[String, UUID, Future[Either[ServiceError, StateBenefitsUserData]]] = {
    (mockStateBenefitsService.getUserData(_: String, _: UUID))
      .expects(nino, sessionDataId)
      .returning(Future.successful(result))
  }

  def mockCreateOrUpdateStateBenefitsUserData(stateBenefitsUserData: StateBenefitsUserData,
                                              result: Either[ServiceError, UUID]): CallHandler1[StateBenefitsUserData, Future[Either[ServiceError, UUID]]] = {
    (mockStateBenefitsService.createOrUpdateUserData(_: StateBenefitsUserData))
      .expects(stateBenefitsUserData)
      .returning(Future.successful(result))
  }

  def mockRemoveClaim(nino: String,
                      sessionDataId: UUID,
                      reslt: Either[ServiceError, Unit]): CallHandler3[String, UUID, HeaderCarrier, Future[Either[ServiceError, Unit]]] = {
    (mockStateBenefitsService.removeClaim(_: String, _: UUID)(_: HeaderCarrier))
      .expects(nino, sessionDataId, *)
      .returning(Future.successful(reslt))
  }
}
