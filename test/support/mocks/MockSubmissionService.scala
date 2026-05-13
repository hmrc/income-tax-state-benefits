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
import models.errors.ApiServiceError
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import services.SubmissionService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

trait MockSubmissionService extends MockitoSugar {

  protected val mockSubmissionService: SubmissionService = mock[SubmissionService]

  def mockGetIncomeTaxUserData(taxYear: Int,
                               nino: String,
                               mtdItId: String,
                               result: Either[ApiServiceError, IncomeTaxUserData]): Unit =
    when(mockSubmissionService.getIncomeTaxUserData(eqTo(taxYear), eqTo(nino), eqTo(mtdItId))(any[HeaderCarrier]()))
      .thenReturn(Future.successful(result))

  def mockRefreshStateBenefits(taxYear: Int,
                               nino: String,
                               mtdItId: String,
                               result: Either[ApiServiceError, Unit]): Unit =
    when(mockSubmissionService.refreshStateBenefits(eqTo(taxYear), eqTo(nino), eqTo(mtdItId))(any[HeaderCarrier]()))
      .thenReturn(Future.successful(result))
}
