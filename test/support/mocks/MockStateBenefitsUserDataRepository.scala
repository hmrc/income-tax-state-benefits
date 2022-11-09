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

import models.errors.ServiceError
import models.mongo.StateBenefitsUserData
import org.scalamock.handlers.{CallHandler1, CallHandler2}
import org.scalamock.scalatest.MockFactory
import repositories.StateBenefitsUserDataRepository

import java.util.UUID
import scala.concurrent.Future

trait MockStateBenefitsUserDataRepository extends MockFactory {

  protected val mockStateBenefitsUserDataRepository: StateBenefitsUserDataRepository = mock[StateBenefitsUserDataRepository]

  def mockCreateOrUpdate(stateBenefitsUserData: StateBenefitsUserData,
                         result: Either[ServiceError, UUID]): CallHandler1[StateBenefitsUserData, Future[Either[ServiceError, UUID]]] = {
    (mockStateBenefitsUserDataRepository.createOrUpdate(_: StateBenefitsUserData))
      .expects(stateBenefitsUserData)
      .returning(Future.successful(result))
  }

  def mockFind(nino: String,
               sessionDataId: UUID,
               result: Either[ServiceError, StateBenefitsUserData]): CallHandler2[String, UUID, Future[Either[ServiceError, StateBenefitsUserData]]] = {
    (mockStateBenefitsUserDataRepository.find(_: String, _: UUID))
      .expects(nino, sessionDataId)
      .returning(Future.successful(result))
  }
}
