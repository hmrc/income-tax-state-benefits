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

package services

import support.UnitTest
import support.builders.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import support.builders.api.AllStateBenefitsDataBuilder.anAllStateBenefitsData
import support.builders.mongo.StateBenefitsUserDataBuilder.aStateBenefitsUserData
import support.mocks.{MockIntegrationFrameworkConnector, MockStateBenefitsUserDataRepository, MockSubmissionConnector}
import uk.gov.hmrc.http.HeaderCarrier

import java.util.UUID

class StateBenefitsServiceSpec extends UnitTest
  with MockSubmissionConnector
  with MockIntegrationFrameworkConnector
  with MockStateBenefitsUserDataRepository {

  private implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

  private val anyTaxYear = 2022
  private val sessionDataId = UUID.randomUUID()

  private val underTest = new StateBenefitsService(mockSubmissionConnector, mockIntegrationFrameworkConnector, mockStateBenefitsUserDataRepository)

  ".getAllStateBenefitsData" should {
    "delegate to integrationFrameworkConnector and return the result" in {
      val result = Right(Some(anAllStateBenefitsData))

      mockGetAllStateBenefitsData(anyTaxYear, "any-nino", result)

      underTest.getAllStateBenefitsData(anyTaxYear, "any-nino")
    }
  }

  ".getPriorData" should {
    "delegate to submissionConnector and return the result" in {
      mockGetIncomeTaxUserData(anyTaxYear, nino = "any-nino", mtditid = "any-mtditid", Right(anIncomeTaxUserData))

      underTest.getPriorData(anyTaxYear, "any-nino", "any-mtditid")
    }
  }

  ".getStateBenefitsUserData" should {
    "delegate to stateBenefitsUserDataRepository and return the result" in {
      mockFind(sessionDataId, Right(aStateBenefitsUserData))

      underTest.getStateBenefitsUserData(sessionDataId)
    }
  }

  ".createOrUpdateStateBenefitsUserData" should {
    "delegate to stateBenefitsUserDataRepository and return the result" in {
      mockCreateOrUpdate(aStateBenefitsUserData, Right(sessionDataId))

      await(underTest.createOrUpdateStateBenefitsUserData(aStateBenefitsUserData)) shouldBe Right(sessionDataId)
    }
  }
}
