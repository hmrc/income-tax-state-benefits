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

import models.errors.DataNotUpdatedError
import support.UnitTest
import support.builders.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import support.builders.api.AllStateBenefitsDataBuilder.anAllStateBenefitsData
import support.builders.api.StateBenefitDetailOverrideBuilder.aStateBenefitDetailOverride
import support.builders.mongo.StateBenefitsUserDataBuilder.aStateBenefitsUserData
import support.mocks.{MockIntegrationFrameworkConnector, MockStateBenefitsUserDataRepository, MockSubmissionConnector}
import uk.gov.hmrc.http.HeaderCarrier

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global

class StateBenefitsServiceSpec extends UnitTest
  with MockSubmissionConnector
  with MockIntegrationFrameworkConnector
  with MockStateBenefitsUserDataRepository {

  private implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

  private val anyTaxYear = 2022
  private val anyNino = "any-nino"
  private val anyBenefitId = UUID.randomUUID()
  private val sessionDataId = UUID.randomUUID()

  private val underTest = new StateBenefitsService(mockSubmissionConnector, mockIntegrationFrameworkConnector, mockStateBenefitsUserDataRepository)

  ".getAllStateBenefitsData" should {
    "delegate to integrationFrameworkConnector and return the result" in {
      val result = Right(Some(anAllStateBenefitsData))

      mockGetAllStateBenefitsData(anyTaxYear, anyNino, result)

      underTest.getAllStateBenefitsData(anyTaxYear, anyNino)
    }
  }

  ".createOrUpdateStateBenefitDetailOverride" should {
    "delegate to integrationFrameworkConnector and return the result" in {
      mockCreateOrUpdateStateBenefitDetailOverride(anyTaxYear, anyNino, anyBenefitId, aStateBenefitDetailOverride, Right(()))

      underTest.createOrUpdateStateBenefitDetailOverride(anyTaxYear, anyNino, anyBenefitId, aStateBenefitDetailOverride)
    }
  }

  ".deleteStateBenefit" should {
    "delegate to integrationFrameworkConnector and return the result" in {
      mockDeleteStateBenefit(anyTaxYear, anyNino, anyBenefitId, Right(()))

      underTest.deleteStateBenefit(anyTaxYear, anyNino, anyBenefitId)
    }
  }

  ".getPriorData" should {
    "delegate to submissionConnector and return the result" in {
      mockGetIncomeTaxUserData(anyTaxYear, nino = anyNino, mtditid = "any-mtditid", Right(anIncomeTaxUserData))

      underTest.getPriorData(anyTaxYear, anyNino, "any-mtditid")
    }
  }

  ".getStateBenefitsUserData" should {
    "delegate to stateBenefitsUserDataRepository and return the result" in {
      mockFind(anyNino, sessionDataId, Right(aStateBenefitsUserData))

      underTest.getStateBenefitsUserData(anyNino, sessionDataId)
    }
  }

  ".createOrUpdateStateBenefitsUserData" should {
    "delegate to createOrUpdate and return result when clear succeeds for given sessionId" in {
      val userData = aStateBenefitsUserData.copy(sessionDataId = None, sessionId = "some-session-id")

      mockClear("some-session-id", result = true)
      mockCreateOrUpdate(userData, Right(sessionDataId))

      await(underTest.createOrUpdateStateBenefitsUserData(userData)) shouldBe Right(sessionDataId)
    }

    "return DataNotUpdatedError when clear data fails" in {
      val userData = aStateBenefitsUserData.copy(sessionDataId = None, sessionId = "some-session-id")

      mockClear("some-session-id", result = false)

      await(underTest.createOrUpdateStateBenefitsUserData(userData)) shouldBe Left(DataNotUpdatedError)
    }

    "update existing data when already exists" in {
      mockCreateOrUpdate(aStateBenefitsUserData, Right(sessionDataId))

      await(underTest.createOrUpdateStateBenefitsUserData(aStateBenefitsUserData)) shouldBe Right(sessionDataId)
    }
  }
}
