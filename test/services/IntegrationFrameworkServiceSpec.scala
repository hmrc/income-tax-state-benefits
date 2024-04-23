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
import models.api.UpdateStateBenefit
import models.errors.ApiServiceError
import models.mongo.BenefitDataType.{CustomerAdded, CustomerOverride, HmrcData}
import org.scalatest.OptionValues.convertOptionToValuable
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

class IntegrationFrameworkServiceSpec extends UnitTest with MockIntegrationFrameworkConnector with TaxYearProvider {

  private implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

  private val apiError  = ApiError(INTERNAL_SERVER_ERROR, SingleErrorBody.parsingError)
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

  ".saveStateBenefitsUserData" when {
    "is HMRC data" should {
      val userData = aStateBenefitsUserData.copy(benefitDataType = HmrcData.name)
      "return error when" when {
        "createOrUpdateStateBenefitDetailOverride returns error" in {
          mockCreateOrUpdateStateBenefitDetailOverride(userData.taxYear, userData.nino, benefitId, aStateBenefitDetailOverride, Left(apiError))

          await(underTest.saveStateBenefitsUserData(userData)) shouldBe Left(ApiServiceError(apiError.status.toString))
        }

        "updateCustomerStateBenefit returns error" in {
          mockCreateOrUpdateStateBenefitDetailOverride(userData.taxYear, userData.nino, benefitId, aStateBenefitDetailOverride, Right(()))
          mockUpdateCustomerStateBenefit(userData.taxYear, userData.nino, benefitId, UpdateStateBenefit(aClaimCYAModel), Left(apiError))

          await(underTest.saveStateBenefitsUserData(userData)) shouldBe Left(ApiServiceError(apiError.status.toString))
        }
      }

      "return successful response" when {
        "all calls succeed" in {
          mockCreateOrUpdateStateBenefitDetailOverride(userData.taxYear, userData.nino, benefitId, aStateBenefitDetailOverride, Right(()))
          mockUpdateCustomerStateBenefit(userData.taxYear, userData.nino, benefitId, UpdateStateBenefit(aClaimCYAModel), Right(()))
          mockCreateOrUpdateStateBenefitDetailOverride(userData.taxYear, userData.nino, benefitId, aStateBenefitDetailOverride, Right(()))

          await(underTest.saveStateBenefitsUserData(userData)) shouldBe Right(benefitId)
        }
      }
    }

    "new claim" should {
      val userData = aStateBenefitsUserData.copy(benefitDataType = CustomerAdded.name, claim = Some(aClaimCYAModel.copy(benefitId = None)))
      "return error when" when {
        "addCustomerStateBenefit returns error" in {
          mockAddCustomerStateBenefit(userData.taxYear, userData.nino, anAddStateBenefit, Left(apiError))

          await(underTest.saveStateBenefitsUserData(userData)) shouldBe Left(ApiServiceError(apiError.status.toString))
        }

        "createOrUpdateStateBenefitDetailOverride returns error" in {
          mockAddCustomerStateBenefit(userData.taxYear, userData.nino, anAddStateBenefit, Right(benefitId))
          mockCreateOrUpdateStateBenefitDetailOverride(userData.taxYear, userData.nino, benefitId, aStateBenefitDetailOverride, Left(apiError))

          await(underTest.saveStateBenefitsUserData(userData)) shouldBe Left(ApiServiceError(apiError.status.toString))
        }
      }

      "return successful response" when {
        "all calls succeed" in {
          mockAddCustomerStateBenefit(userData.taxYear, userData.nino, anAddStateBenefit, Right(benefitId))
          mockCreateOrUpdateStateBenefitDetailOverride(userData.taxYear, userData.nino, benefitId, aStateBenefitDetailOverride, Right(()))

          await(underTest.saveStateBenefitsUserData(userData)) shouldBe Right(benefitId)
        }
      }
    }

    "is customer added data" should {
      val userData = aStateBenefitsUserData.copy(benefitDataType = CustomerAdded.name)
      "return error when" when {
        "updateCustomerStateBenefit returns error" in {
          mockUpdateCustomerStateBenefit(userData.taxYear, userData.nino, benefitId, anUpdateStateBenefit, Left(apiError))

          await(underTest.saveStateBenefitsUserData(userData)) shouldBe Left(ApiServiceError(apiError.status.toString))
        }

        "createOrUpdateStateBenefitDetailOverride returns error" in {
          mockUpdateCustomerStateBenefit(userData.taxYear, userData.nino, benefitId, anUpdateStateBenefit, Right(()))
          mockCreateOrUpdateStateBenefitDetailOverride(userData.taxYear, userData.nino, benefitId, aStateBenefitDetailOverride, Left(apiError))

          await(underTest.saveStateBenefitsUserData(userData)) shouldBe Left(ApiServiceError(apiError.status.toString))
        }
      }

      "return successful response" when {
        "all calls succeed" in {
          mockUpdateCustomerStateBenefit(userData.taxYear, userData.nino, benefitId, anUpdateStateBenefit, Right(()))
          mockCreateOrUpdateStateBenefitDetailOverride(userData.taxYear, userData.nino, benefitId, aStateBenefitDetailOverride, Right(()))

          await(underTest.saveStateBenefitsUserData(userData)) shouldBe Right(benefitId)
        }
      }
    }

    "is customer override" should {
      val userData = aStateBenefitsUserData.copy(benefitDataType = CustomerOverride.name)
      "return error when" when {
        "updateCustomerStateBenefit returns error" in {
          mockUpdateCustomerStateBenefit(userData.taxYear, userData.nino, benefitId, anUpdateStateBenefit, Left(apiError))

          await(underTest.saveStateBenefitsUserData(userData)) shouldBe Left(ApiServiceError(apiError.status.toString))
        }

        "createOrUpdateStateBenefitDetailOverride returns error" in {
          mockUpdateCustomerStateBenefit(userData.taxYear, userData.nino, benefitId, anUpdateStateBenefit, Right(()))
          mockCreateOrUpdateStateBenefitDetailOverride(userData.taxYear, userData.nino, benefitId, aStateBenefitDetailOverride, Left(apiError))

          await(underTest.saveStateBenefitsUserData(userData)) shouldBe Left(ApiServiceError(apiError.status.toString))
        }
      }

      "return successful response" when {
        "all calls succeed" in {
          mockUpdateCustomerStateBenefit(userData.taxYear, userData.nino, benefitId, anUpdateStateBenefit, Right(()))
          mockCreateOrUpdateStateBenefitDetailOverride(userData.taxYear, userData.nino, benefitId, aStateBenefitDetailOverride, Right(()))

          await(underTest.saveStateBenefitsUserData(userData)) shouldBe Right(benefitId)
        }
      }
    }
  }

  ".removeOrIgnoreClaim" when {
    val userData = aStateBenefitsUserData.copy(benefitDataType = HmrcData.name)
    "is HMRC data" should {
      "and ignoreStateBenefit returns error then return error" in {
        mockIgnoreStateBenefit(userData.taxYear, userData.nino, userData.claim.get.benefitId.get, Left(apiError))

        await(underTest.removeOrIgnoreClaim(userData)) shouldBe Left(ApiServiceError(apiError.status.toString))
      }

      "and ignoreStateBenefit succeeds then return success" in {
        mockIgnoreStateBenefit(userData.taxYear, userData.nino, userData.claim.get.benefitId.get, Right(()))

        await(underTest.removeOrIgnoreClaim(userData)) shouldBe Right(())
      }
    }

    "is customer added data" should {
      val userData = aStateBenefitsUserData.copy(benefitDataType = CustomerAdded.name)
      "and deleteStateBenefit returns error then return error" in {
        mockDeleteStateBenefit(userData.taxYear, userData.nino, userData.claim.get.benefitId.get, Left(apiError))

        await(underTest.removeOrIgnoreClaim(userData)) shouldBe Left(ApiServiceError(apiError.status.toString))
      }

      "and deleteStateBenefit succeeds then return success" in {
        mockDeleteStateBenefit(userData.taxYear, userData.nino, userData.claim.get.benefitId.get, Right(()))

        await(underTest.removeOrIgnoreClaim(userData)) shouldBe Right(())
      }
    }
  }

  ".removeClaim" when {
    val userData = aStateBenefitsUserData.copy(benefitDataType = HmrcData.name)
    "downstream call is successful" should {
      "return Unit" in {
        mockDeleteStateBenefit(userData.taxYear, userData.nino, userData.claim.value.benefitId.value, Right(()))
        await(underTest.removeClaim(userData.nino, userData.taxYear, userData.claim.value.benefitId.value)) shouldBe Right(())
      }
    }
    "downstream returns an error" should {
      "return the error status wrapped in ApiServiceError" in {
        mockDeleteStateBenefit(userData.taxYear, userData.nino, userData.claim.value.benefitId.value, Left(apiError))
        await(underTest.removeClaim(userData.nino, userData.taxYear, userData.claim.value.benefitId.value)) shouldBe Left(
          ApiServiceError(apiError.status.toString))
      }
    }
  }

  ".unIgnoreClaim" should {
    val userData = aStateBenefitsUserData

    "return error when unIgnoreStateBenefit fails" in {
      mockUnIgnoreStateBenefit(userData.taxYear, userData.nino, benefitId, Left(apiError))

      await(underTest.unIgnoreClaim(userData)) shouldBe Left(ApiServiceError(apiError.status.toString))
    }

    "return success when unIgnoreStateBenefit succeeds" in {
      mockUnIgnoreStateBenefit(userData.taxYear, userData.nino, benefitId, Right(()))

      await(underTest.unIgnoreClaim(userData)) shouldBe Right(())
    }
  }
}
