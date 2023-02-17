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

package services

import connectors.errors.{ApiError, SingleErrorBody}
import models.errors.ApiServiceError
import play.api.http.Status.INTERNAL_SERVER_ERROR
import support.UnitTest
import support.builders.mongo.StateBenefitsUserDataBuilder.aStateBenefitsUserData
import support.mocks.MockDataExchangeServiceConnector
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global

class DESServiceSpec extends UnitTest
  with MockDataExchangeServiceConnector {

  private implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

  private val apiError = ApiError(INTERNAL_SERVER_ERROR, SingleErrorBody.parsingError)

  private val underTest = new DESService(mockDataExchangeServiceConnector)

  ".removeCustomerOverride" should {
    val userData = aStateBenefitsUserData
    "return error when connector.deleteStateBenefitDetailOverride(...) fails" in {
      mockDeleteStateBenefitDetailOverride(userData.taxYear, userData.nino, userData.claim.get.benefitId.get, Left(apiError))

      await(underTest.removeCustomerOverride(userData)) shouldBe Left(ApiServiceError(apiError.status.toString))
    }

    "succeed when connector.deleteStateBenefitDetailOverride(...) succeeds" in {
      mockDeleteStateBenefitDetailOverride(userData.taxYear, userData.nino, userData.claim.get.benefitId.get, Right(()))

      await(underTest.removeCustomerOverride(userData)) shouldBe Right(())
    }
  }
}
