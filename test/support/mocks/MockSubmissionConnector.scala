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

import connectors.SubmissionConnector
import connectors.errors.ApiError
import models.IncomeTaxUserData
import org.scalamock.handlers.CallHandler4
import org.scalamock.scalatest.MockFactory
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

trait MockSubmissionConnector extends MockFactory {

  protected val mockSubmissionConnector: SubmissionConnector = mock[SubmissionConnector]

  def mockGetIncomeTaxUserData(taxYear: Int,
                               nino: String,
                               mtditid: String,
                               result: Either[ApiError, IncomeTaxUserData]
                              ): CallHandler4[Int, String, String, HeaderCarrier, Future[Either[ApiError, IncomeTaxUserData]]] = {

    (mockSubmissionConnector.getIncomeTaxUserData(_: Int, _: String, _: String)(_: HeaderCarrier))
      .expects(taxYear, nino, mtditid, *)
      .returning(Future.successful(result))
  }

  def mockRefreshStateBenefits(taxYear: Int,
                               nino: String,
                               mtditid: String,
                               result: Either[ApiError, Unit]): CallHandler4[Int, String, String, HeaderCarrier, Future[Either[ApiError, Unit]]] = {
    (mockSubmissionConnector.refreshStateBenefits(_: Int, _: String, _: String)(_: HeaderCarrier))
      .expects(taxYear, nino, mtditid, *)
      .returning(Future.successful(result))
  }
}
