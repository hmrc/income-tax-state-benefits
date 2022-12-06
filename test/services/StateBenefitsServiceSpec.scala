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

import connectors.errors.{ApiError, SingleErrorBody}
import models.errors.{ApiServiceError, DataNotUpdatedError, MongoError}
import play.api.http.Status.SERVICE_UNAVAILABLE
import support.UnitTest
import support.builders.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import support.builders.api.AllStateBenefitsDataBuilder.anAllStateBenefitsData
import support.builders.mongo.ClaimCYAModelBuilder.aClaimCYAModel
import support.builders.mongo.StateBenefitsUserDataBuilder.aStateBenefitsUserData
import support.mocks.{MockIntegrationFrameworkService, MockStateBenefitsUserDataRepository, MockSubmissionConnector}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global

class StateBenefitsServiceSpec extends UnitTest
  with MockIntegrationFrameworkService
  with MockSubmissionConnector
  with MockStateBenefitsUserDataRepository {

  private implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

  private val anyTaxYear = 2022
  private val anyNino = "any-nino"
  private val sessionDataId = aStateBenefitsUserData.sessionDataId.get
  private val apiError = ApiError(SERVICE_UNAVAILABLE, SingleErrorBody("SERVER_ERROR", "Service is unavailable"))

  private val underTest = new StateBenefitsService(
    mockIntegrationFrameworkService,
    mockSubmissionConnector,
    mockStateBenefitsUserDataRepository
  )

  ".getAllStateBenefitsData" should {
    "delegate to IntegrationFrameworkService and return the result" in {
      val result = Right(Some(anAllStateBenefitsData))

      mockGetAllStateBenefitsData(anyTaxYear, anyNino, result)

      underTest.getAllStateBenefitsData(anyTaxYear, anyNino)
    }
  }

  ".getPriorData" should {
    "delegate to submissionConnector and return the result" in {
      mockGetIncomeTaxUserData(anyTaxYear, nino = anyNino, mtditid = "any-mtditid", Right(anIncomeTaxUserData))

      underTest.getPriorData(anyTaxYear, anyNino, "any-mtditid")
    }
  }

  ".getUserData" should {
    "delegate to stateBenefitsUserDataRepository and return the result" in {
      mockFind(anyNino, sessionDataId, Right(aStateBenefitsUserData))

      await(underTest.getUserData(anyNino, sessionDataId)) shouldBe Right(aStateBenefitsUserData)
    }
  }

  ".createOrUpdateUserData" should {
    "delegate to createOrUpdate and return result when clear succeeds for given sessionId" in {
      val userData = aStateBenefitsUserData.copy(sessionDataId = None, sessionId = "some-session-id")

      mockClear("some-session-id", result = true)
      mockCreateOrUpdate(userData, Right(sessionDataId))

      await(underTest.createOrUpdateUserData(userData)) shouldBe Right(sessionDataId)
    }

    "return DataNotUpdatedError when clear data fails" in {
      val userData = aStateBenefitsUserData.copy(sessionDataId = None, sessionId = "some-session-id")

      mockClear("some-session-id", result = false)

      await(underTest.createOrUpdateUserData(userData)) shouldBe Left(DataNotUpdatedError)
    }

    "update existing data when already exists" in {
      mockCreateOrUpdate(aStateBenefitsUserData, Right(sessionDataId))

      await(underTest.createOrUpdateUserData(aStateBenefitsUserData)) shouldBe Right(sessionDataId)
    }
  }

  ".saveUserData" should {
    "return error when stateBenefitsUserDataRepository.find(...) returns error" in {
      mockFind(aStateBenefitsUserData.nino, aStateBenefitsUserData.sessionDataId.get, Left(DataNotUpdatedError))

      await(underTest.saveUserData(aStateBenefitsUserData)) shouldBe Left(DataNotUpdatedError)
    }

    "return error when createOrUpdateStateBenefit fails" in {
      mockFind(aStateBenefitsUserData.nino, sessionDataId, Right(aStateBenefitsUserData))
      mockCreateOrUpdateStateBenefit(aStateBenefitsUserData, Left(ApiServiceError("some-error")))

      await(underTest.saveUserData(aStateBenefitsUserData)) shouldBe Left(ApiServiceError("some-error"))
    }

    "return error when refreshStateBenefits fails" in {
      val userData = aStateBenefitsUserData
      mockFind(userData.nino, sessionDataId, Right(userData))
      mockCreateOrUpdateStateBenefit(aStateBenefitsUserData, Right(()))
      mockRefreshStateBenefits(userData.taxYear, userData.nino, userData.mtdItId, Left(apiError))

      await(underTest.saveUserData(userData)) shouldBe Left(ApiServiceError(apiError.status.toString))
    }

    "return error when clear fails" in {
      val userData = aStateBenefitsUserData
      mockFind(userData.nino, sessionDataId, Right(userData))
      mockCreateOrUpdateStateBenefit(aStateBenefitsUserData, Right(()))
      mockRefreshStateBenefits(userData.taxYear, userData.nino, userData.mtdItId, Right(()))
      mockClear(userData.sessionId, result = false)

      await(underTest.saveUserData(userData)) shouldBe Left(MongoError("FAILED_TO_CLEAR_STATE_BENEFITS_DATA"))
    }

    "perform proper saveUserData when all subsequent calls succeed" in {
      val userData = aStateBenefitsUserData
      mockFind(userData.nino, sessionDataId, Right(userData))
      mockCreateOrUpdateStateBenefit(aStateBenefitsUserData, Right(()))
      mockRefreshStateBenefits(userData.taxYear, userData.nino, userData.mtdItId, Right(()))
      mockClear(userData.sessionId, result = true)

      await(underTest.saveUserData(userData)) shouldBe Right(())
    }
  }

  ".removeClaim(...)" should {
    "return error when stateBenefitsUserDataRepository.find(...) returns error" in {
      mockFind(anyNino, sessionDataId, Left(DataNotUpdatedError))

      await(underTest.removeClaim(anyNino, sessionDataId)) shouldBe Left(DataNotUpdatedError)
    }

    "return error when isPriorSubmission is false and clear from repository fails" in {
      val userData = aStateBenefitsUserData.copy(isPriorSubmission = false)
      mockFind(anyNino, sessionDataId, Right(userData))
      mockClear(userData.sessionId, result = false)

      await(underTest.removeClaim(anyNino, sessionDataId)) shouldBe Left(MongoError("FAILED_TO_CLEAR_STATE_BENEFITS_DATA"))
    }

    "clear repository data when isPriorSubmission is false and clear succeeds" in {
      val userData = aStateBenefitsUserData.copy(isPriorSubmission = false)
      mockFind(anyNino, sessionDataId, Right(userData))
      mockClear(userData.sessionId, result = true)

      await(underTest.removeClaim(anyNino, sessionDataId)) shouldBe Right(())
    }

    "return error when isPriorSubmission is true and removeOrIgnoreClaim fails" in {
      val userData = aStateBenefitsUserData.copy(isPriorSubmission = true)
      mockFind(anyNino, sessionDataId, Right(userData))
      mockRemoveOrIgnoreClaim(userData, userData.claim.get.benefitId.get, Left(ApiServiceError("some-error")))

      await(underTest.removeClaim(anyNino, sessionDataId)) shouldBe Left(ApiServiceError("some-error"))
    }

    "return error when isPriorSubmission is true and refreshStateBenefits fails" in {
      val userData = aStateBenefitsUserData.copy(isPriorSubmission = true)
      mockFind(anyNino, sessionDataId, Right(userData))
      mockRemoveOrIgnoreClaim(userData, userData.claim.get.benefitId.get, Right(()))
      mockRefreshStateBenefits(userData.taxYear, userData.nino, userData.mtdItId, Left(apiError))

      await(underTest.removeClaim(anyNino, sessionDataId)) shouldBe Left(ApiServiceError(apiError.status.toString))
    }

    "return error when isPriorSubmission is true and clear fails" in {
      val userData = aStateBenefitsUserData.copy(isPriorSubmission = true)
      mockFind(anyNino, sessionDataId, Right(userData))
      mockRemoveOrIgnoreClaim(userData, userData.claim.get.benefitId.get, Right(()))
      mockRefreshStateBenefits(userData.taxYear, userData.nino, userData.mtdItId, Right(()))
      mockClear(userData.sessionId, result = false)

      await(underTest.removeClaim(anyNino, sessionDataId)) shouldBe Left(MongoError("FAILED_TO_CLEAR_STATE_BENEFITS_DATA"))
    }

    "perform proper remove when isPriorSubmission is true all subsequent calls succeed" in {
      val userData = aStateBenefitsUserData.copy(isPriorSubmission = true)
      mockFind(anyNino, sessionDataId, Right(userData))
      mockRemoveOrIgnoreClaim(userData, userData.claim.get.benefitId.get, Right(()))
      mockRefreshStateBenefits(userData.taxYear, userData.nino, userData.mtdItId, Right(()))
      mockClear(userData.sessionId, result = true)

      await(underTest.removeClaim(anyNino, sessionDataId)) shouldBe Right(())
    }

    "perform proper remove when isPriorSubmission is true all subsequent calls succeed for Customer added data" in {
      val userData = aStateBenefitsUserData.copy(isPriorSubmission = true, claim = Some(aClaimCYAModel.copy(isHmrcData = false)))
      mockFind(anyNino, sessionDataId, Right(userData))
      mockRemoveOrIgnoreClaim(userData, userData.claim.get.benefitId.get, Right(()))
      mockRefreshStateBenefits(userData.taxYear, userData.nino, userData.mtdItId, Right(()))
      mockClear(userData.sessionId, result = true)

      await(underTest.removeClaim(anyNino, sessionDataId)) shouldBe Right(())
    }
  }
}
