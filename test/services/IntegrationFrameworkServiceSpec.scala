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
import models.errors.ApiServiceError
import play.api.http.Status.INTERNAL_SERVER_ERROR
import support.UnitTest
import support.builders.api.AddStateBenefitBuilder.anAddStateBenefit
import support.builders.api.AllStateBenefitsDataBuilder.anAllStateBenefitsData
import support.builders.api.StateBenefitDetailOverrideBuilder.aStateBenefitDetailOverride
import support.builders.api.UpdateStateBenefitBuilder.anUpdateStateBenefit
import support.builders.mongo.ClaimCYAModelBuilder.aClaimCYAModel
import support.builders.mongo.StateBenefitsUserDataBuilder.aStateBenefitsUserData
import support.mocks.MockIntegrationFrameworkConnector
import support.providers.TaxYearProvider
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global

class IntegrationFrameworkServiceSpec extends UnitTest
  with MockIntegrationFrameworkConnector
  with TaxYearProvider {

  private implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

  private val apiError = ApiError(INTERNAL_SERVER_ERROR, SingleErrorBody.parsingError)
  private val benefitId = aClaimCYAModel.benefitId.get

  private val underTest = new IntegrationFrameworkService(mockIntegrationFrameworkConnector)

  ".getAllStateBenefitsData" should {
    "return error when getAllStateBenefitsData fails" in {
      mockGetAllStateBenefitsData(taxYear, "some-nino", Left(apiError))

      await(underTest.getAllStateBenefitsData(taxYear, "some-nino")) shouldBe Left(ApiServiceError(apiError.status.toString))
    }

    "return AllStateBenefitsData when getAllStateBenefitsData succeeds" in {
      mockGetAllStateBenefitsData(taxYear, "some-nino", Right(Some(anAllStateBenefitsData)))

      await(underTest.getAllStateBenefitsData(taxYear, "some-nino")) shouldBe Right(Some(anAllStateBenefitsData))
    }
  }

  ".createOrUpdateStateBenefit" when {
    "new claim" should {
      "and addStateBenefit returns error then return error" in {
        val userData = aStateBenefitsUserData.copy(claim = Some(aClaimCYAModel.copy(benefitId = None)))

        mockAddStateBenefit(userData.taxYear, userData.nino, anAddStateBenefit, Left(apiError))

        await(underTest.createOrUpdateStateBenefit(userData)) shouldBe Left(ApiServiceError(apiError.status.toString))
      }

      "and createOrUpdateStateBenefitDetailOverride returns error then return error" in {
        val userData = aStateBenefitsUserData.copy(claim = Some(aClaimCYAModel.copy(benefitId = None)))

        mockAddStateBenefit(userData.taxYear, userData.nino, anAddStateBenefit, Right(benefitId))
        mockCreateOrUpdateStateBenefitDetailOverride(userData.taxYear, userData.nino, benefitId, aStateBenefitDetailOverride, Left(apiError))

        await(underTest.createOrUpdateStateBenefit(userData)) shouldBe Left(ApiServiceError(apiError.status.toString))
      }

      "succeed when all calls succeed" in {
        val userData = aStateBenefitsUserData.copy(claim = Some(aClaimCYAModel.copy(benefitId = None)))

        mockAddStateBenefit(userData.taxYear, userData.nino, anAddStateBenefit, Right(benefitId))
        mockCreateOrUpdateStateBenefitDetailOverride(userData.taxYear, userData.nino, benefitId, aStateBenefitDetailOverride, Right(()))

        await(underTest.createOrUpdateStateBenefit(userData)) shouldBe Right(())
      }
    }

    "existing claim" should {
      "and updateStateBenefit returns error then return error" in {
        mockUpdateStateBenefit(aStateBenefitsUserData.taxYear, aStateBenefitsUserData.nino, benefitId, anUpdateStateBenefit, Left(apiError))

        await(underTest.createOrUpdateStateBenefit(aStateBenefitsUserData)) shouldBe Left(ApiServiceError(apiError.status.toString))
      }

      "and createOrUpdateStateBenefitDetailOverride returns error then return error" in {
        val userData = aStateBenefitsUserData

        mockUpdateStateBenefit(aStateBenefitsUserData.taxYear, aStateBenefitsUserData.nino, benefitId, anUpdateStateBenefit, Right(()))
        mockCreateOrUpdateStateBenefitDetailOverride(userData.taxYear, aStateBenefitsUserData.nino, benefitId, aStateBenefitDetailOverride, Left(apiError))

        await(underTest.createOrUpdateStateBenefit(aStateBenefitsUserData)) shouldBe Left(ApiServiceError(apiError.status.toString))
      }

      "succeed when all calls succeed" in {
        val userData = aStateBenefitsUserData

        mockUpdateStateBenefit(userData.taxYear, userData.nino, benefitId, anUpdateStateBenefit, Right(()))
        mockCreateOrUpdateStateBenefitDetailOverride(userData.taxYear, userData.nino, benefitId, aStateBenefitDetailOverride, Right(()))

        await(underTest.createOrUpdateStateBenefit(userData)) shouldBe Right(())
      }
    }
  }

  ".removeOrIgnoreClaim" when {
    "is HMRC data" should {
      "and ignoreStateBenefit returns error then return error" in {
        val userData = aStateBenefitsUserData.copy(claim = Some(aClaimCYAModel.copy(isHmrcData = true)))

        mockIgnoreStateBenefit(userData.taxYear, userData.nino, benefitId, Left(apiError))

        await(underTest.removeOrIgnoreClaim(userData, benefitId)) shouldBe Left(ApiServiceError(apiError.status.toString))
      }

      "and ignoreStateBenefit succeeds then return success" in {
        val userData = aStateBenefitsUserData.copy(claim = Some(aClaimCYAModel.copy(benefitId = None)))

        mockIgnoreStateBenefit(userData.taxYear, userData.nino, benefitId, Right(()))

        await(underTest.removeOrIgnoreClaim(userData, benefitId)) shouldBe Right(())
      }
    }

    "is customer added data" should {
      "and deleteStateBenefit returns error then return error" in {
        val userData = aStateBenefitsUserData.copy(claim = Some(aClaimCYAModel.copy(isHmrcData = false)))

        mockDeleteStateBenefit(userData.taxYear, userData.nino, benefitId, Left(apiError))

        await(underTest.removeOrIgnoreClaim(userData, benefitId)) shouldBe Left(ApiServiceError(apiError.status.toString))
      }

      "and deleteStateBenefit succeeds then return success" in {
        val userData = aStateBenefitsUserData.copy(claim = Some(aClaimCYAModel.copy(isHmrcData = false)))

        mockDeleteStateBenefit(userData.taxYear, userData.nino, benefitId, Right(()))

        await(underTest.removeOrIgnoreClaim(userData, benefitId)) shouldBe Right(())
      }
    }
  }
}
