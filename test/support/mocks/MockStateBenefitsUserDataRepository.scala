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

import models.errors.ServiceError
import models.mongo.StateBenefitsUserData
import org.mockito.ArgumentMatchers.{eq as eqTo}
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import repositories.StateBenefitsUserDataRepository

import java.util.UUID
import scala.concurrent.Future

trait MockStateBenefitsUserDataRepository extends MockitoSugar {

  protected val mockStateBenefitsUserDataRepository: StateBenefitsUserDataRepository = mock[StateBenefitsUserDataRepository]

  def mockCreateOrUpdate(stateBenefitsUserData: StateBenefitsUserData,
                         result: Either[ServiceError, UUID]): Unit =
    when(mockStateBenefitsUserDataRepository.createOrUpdate(eqTo(stateBenefitsUserData)))
      .thenReturn(Future.successful(result))

  def mockFind(nino: String,
               sessionDataId: UUID,
               result: Either[ServiceError, StateBenefitsUserData]): Unit =
    when(mockStateBenefitsUserDataRepository.find(eqTo(nino), eqTo(sessionDataId)))
      .thenReturn(Future.successful(result))

  def mockClear(sessionId: String,
                result: Either[ServiceError, Unit]): Unit =
    when(mockStateBenefitsUserDataRepository.clear(eqTo(sessionId)))
      .thenReturn(Future.successful(result))
}
