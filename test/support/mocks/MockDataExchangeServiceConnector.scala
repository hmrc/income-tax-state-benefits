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

import connectors.DataExchangeServiceConnector
import connectors.errors.ApiError
import org.scalamock.handlers.CallHandler4
import org.scalamock.scalatest.MockFactory
import uk.gov.hmrc.http.HeaderCarrier

import java.util.UUID
import scala.concurrent.Future

trait MockDataExchangeServiceConnector extends MockFactory {

  protected val mockDataExchangeServiceConnector: DataExchangeServiceConnector = mock[DataExchangeServiceConnector]

  def mockDeleteStateBenefitDetailOverride(taxYear: Int,
                                  nino: String,
                                  benefitId: UUID,
                                  result: Either[ApiError, Unit]): CallHandler4[Int, String, UUID, HeaderCarrier, Future[Either[ApiError, Unit]]] = {
    (mockDataExchangeServiceConnector.deleteStateBenefitDetailOverride(_: Int, _: String, _: UUID)(_: HeaderCarrier))
      .expects(taxYear, nino, benefitId, *)
      .returning(Future.successful(result))
  }
}
