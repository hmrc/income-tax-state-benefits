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
import org.scalamock.handlers._
import org.scalamock.scalatest.MockFactory
import services.StateBenefitsService
import uk.gov.hmrc.http.HeaderCarrier

import java.util.UUID
import scala.concurrent.Future

trait MockStateBenefitsService extends MockFactory {

  protected val mockStateBenefitsService: StateBenefitsService = mock[StateBenefitsService]

  def mockGetAllStateBenefitsData(taxYear: Int, nino: String, result: Either[ServiceError, Option[AllStateBenefitsData]])
  : CallHandler3[Int, String, HeaderCarrier, Future[Either[ServiceError, Option[AllStateBenefitsData]]]] =
    (mockStateBenefitsService
      .getAllStateBenefitsData(_: Int, _: String)(_: HeaderCarrier))
      .expects(taxYear, nino, *)
      .returning(Future.successful(result))

    def mockGetAllStateBenefitsDataException(taxYear: Int, nino: String, result: Throwable)
    : CallHandler3[Int, String, HeaderCarrier, Future[Either[ServiceError, Option[AllStateBenefitsData]]]] =
    (mockStateBenefitsService
      .getAllStateBenefitsData(_: Int, _: String)(_: HeaderCarrier))
      .expects(taxYear, nino, *)
      .returning(Future.failed(result))

  def mockGetPriorData(taxYear: Int, nino: String, mtditid: String, result: Either[ApiServiceError, IncomeTaxUserData])
      : CallHandler4[Int, String, String, HeaderCarrier, Future[Either[ApiServiceError, IncomeTaxUserData]]] =
    (mockStateBenefitsService
      .getPriorData(_: Int, _: String, _: String)(_: HeaderCarrier))
      .expects(taxYear, nino, mtditid, *)
      .returning(Future.successful(result))

  def mockGetStateBenefitsUserData(
      nino: String,
      sessionDataId: UUID,
      result: Either[ServiceError, StateBenefitsUserData]): CallHandler2[String, UUID, Future[Either[ServiceError, StateBenefitsUserData]]] =
    (mockStateBenefitsService
      .getSessionData(_: String, _: UUID))
      .expects(nino, sessionDataId)
      .returning(Future.successful(result))

  def mockCreateSessionData(stateBenefitsUserData: StateBenefitsUserData,
                            result: Either[ServiceError, UUID]): CallHandler1[StateBenefitsUserData, Future[Either[ServiceError, UUID]]] =
    (mockStateBenefitsService
      .createSessionData(_: StateBenefitsUserData))
      .expects(stateBenefitsUserData)
      .returning(Future.successful(result))

  def mockUpdateSessionData(stateBenefitsUserData: StateBenefitsUserData,
                            result: Either[ServiceError, UUID]): CallHandler1[StateBenefitsUserData, Future[Either[ServiceError, UUID]]] =
    (mockStateBenefitsService
      .updateSessionData(_: StateBenefitsUserData))
      .expects(stateBenefitsUserData)
      .returning(Future.successful(result))

  def mockSaveUserData(
      userData: StateBenefitsUserData,
      result: Either[ServiceError, Unit]): CallHandler3[StateBenefitsUserData, Boolean, HeaderCarrier, Future[Either[ServiceError, Unit]]] =
    (mockStateBenefitsService
      .saveClaim(_: StateBenefitsUserData, _: Boolean)(_: HeaderCarrier))
      .expects(userData, *, *)
      .returning(Future.successful(result))

  def mockRemoveClaim(nino: String,
                      sessionDataId: UUID,
                      result: Either[ServiceError, Unit]): CallHandler3[String, UUID, HeaderCarrier, Future[Either[ServiceError, Unit]]] =
    (mockStateBenefitsService
      .removeClaim(_: String, _: UUID)(_: HeaderCarrier))
      .expects(nino, sessionDataId, *)
      .returning(Future.successful(result))

  def mockRemoveClaimById(nino: String, benefitId: UUID, taxYear: Int, mtdItId: String)(
      result: Either[ServiceError, Unit]): CallHandler5[String, Int, String, UUID, HeaderCarrier, Future[Either[ServiceError, Unit]]] =
    (mockStateBenefitsService
      .removeClaimById(_: String, _: Int, _: String, _: UUID)(_: HeaderCarrier))
      .expects(nino, taxYear, mtdItId, benefitId, *)
      .returning(Future.successful(result))

  def mockRestoreClaim(nino: String,
                       sessionDataId: UUID,
                       result: Either[ServiceError, Unit]): CallHandler3[String, UUID, HeaderCarrier, Future[Either[ServiceError, Unit]]] =
    (mockStateBenefitsService
      .restoreClaim(_: String, _: UUID)(_: HeaderCarrier))
      .expects(nino, sessionDataId, *)
      .returning(Future.successful(result))
}
