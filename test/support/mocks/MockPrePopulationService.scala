/*
 * Copyright 2025 HM Revenue & Customs
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

import cats.data.EitherT
import models.errors.ServiceError
import models.prePopulation.PrePopulationResponse
import org.scalamock.handlers._
import org.scalamock.scalatest.MockFactory
import services.PrePopulationService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait MockPrePopulationService extends MockFactory {

  protected val mockPrePopService: PrePopulationService = mock[PrePopulationService]

  def mockGetPrePop(taxYear: Int, nino: String, result: Either[ServiceError, PrePopulationResponse]):
  CallHandler4[Int, String, ExecutionContext, HeaderCarrier, EitherT[Future, ServiceError, PrePopulationResponse]] =
    (mockPrePopService
      .get(_: Int, _: String)(_: ExecutionContext, _: HeaderCarrier))
      .expects(taxYear, nino, *, *)
      .returning(EitherT(Future.successful(result)))

    def mockGetPrePopException(taxYear: Int, nino: String, result: Throwable):
    CallHandler4[Int, String, ExecutionContext, HeaderCarrier, EitherT[Future, ServiceError, PrePopulationResponse]] =
    (mockPrePopService
      .get(_: Int, _: String)(_: ExecutionContext, _: HeaderCarrier))
      .expects(taxYear, nino, *, *)
      .returning(EitherT[Future, ServiceError, PrePopulationResponse](Future.failed(result)))
}
