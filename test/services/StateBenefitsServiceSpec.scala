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

import models.errors.{ApiServiceError, DataNotUpdatedError, MongoError}
import models.mongo.BenefitDataType.{CustomerAdded, CustomerOverride, HmrcData}
import org.scalatest.OptionValues.convertOptionToValuable
import support.UnitTest
import support.builders.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import support.builders.api.AllStateBenefitsDataBuilder.anAllStateBenefitsData
import support.builders.mongo.ClaimCYAModelBuilder.aClaimCYAModel
import support.builders.mongo.StateBenefitsUserDataBuilder.aStateBenefitsUserData
import support.mocks.{MockIntegrationFrameworkService, MockStateBenefitsUserDataRepository, MockSubmissionService}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global

class StateBenefitsServiceSpec
    extends UnitTest
    with MockIntegrationFrameworkService
    with MockSubmissionService
    with MockStateBenefitsUserDataRepository {

  private implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

  private val anyTaxYear    = 2022
  private val anyNino       = "any-nino"
  private val sessionDataId = aStateBenefitsUserData.sessionDataId.get
  private val benefitId     = aStateBenefitsUserData.claim.get.benefitId.get

  private val underTest = new StateBenefitsService(
    mockIntegrationFrameworkService,
    mockSubmissionService,
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
    "delegate to submissionService and return the result" in {
      mockGetIncomeTaxUserData(anyTaxYear, nino = anyNino, mtdItId = "any-mtditid", Right(anIncomeTaxUserData))

      underTest.getPriorData(anyTaxYear, anyNino, "any-mtditid")
    }
  }

  ".getSessionData" should {
    "delegate to stateBenefitsUserDataRepository and return the result" in {
      mockFind(anyNino, sessionDataId, Right(aStateBenefitsUserData))

      await(underTest.getSessionData(anyNino, sessionDataId)) shouldBe Right(aStateBenefitsUserData)
    }
  }

  ".createSessionData" should {
    "delegate to repository createOrUpdate and return result when clear succeeds for given sessionId" in {
      val userData = aStateBenefitsUserData.copy(sessionDataId = None, sessionId = "some-session-id")

      mockClear("some-session-id", result = Right(()))
      mockCreateOrUpdate(userData, Right(sessionDataId))

      await(underTest.createSessionData(userData)) shouldBe Right(sessionDataId)
    }

    "return DataNotUpdatedError when clear session data fails" in {
      val userData = aStateBenefitsUserData.copy(sessionDataId = None, sessionId = "some-session-id")

      mockClear("some-session-id", result = Left(ApiServiceError("some-error")))

      await(underTest.createSessionData(userData)) shouldBe Left(ApiServiceError("some-error"))
    }
  }

  ".updateSessionData" should {
    "delegate to repository createOrUpdate update existing data when already exists" in {
      val stateBenefitsUserData = aStateBenefitsUserData.copy(sessionDataId = Some(sessionDataId))

      mockCreateOrUpdate(stateBenefitsUserData, Right(sessionDataId))

      await(underTest.updateSessionData(aStateBenefitsUserData)) shouldBe Right(sessionDataId)
    }
  }

  ".saveClaim" should {
    "return error when stateBenefitsUserDataRepository.find(...) returns error" in {
      mockFind(aStateBenefitsUserData.nino, aStateBenefitsUserData.sessionDataId.get, Left(DataNotUpdatedError))

      await(underTest.saveClaim(aStateBenefitsUserData)) shouldBe Left(DataNotUpdatedError)
    }

    "return error when ifService.saveStateBenefitsUserData(...) fails" in {
      mockFind(aStateBenefitsUserData.nino, sessionDataId, Right(aStateBenefitsUserData))
      mockSaveStateBenefitsUserData(aStateBenefitsUserData, Left(ApiServiceError("some-error")))

      await(underTest.saveClaim(aStateBenefitsUserData)) shouldBe Left(ApiServiceError("some-error"))
    }

    "return error when submissionService.refreshStateBenefits(...) fails" in {
      val userData = aStateBenefitsUserData
      mockFind(userData.nino, sessionDataId, Right(userData))
      mockSaveStateBenefitsUserData(userData, Right(benefitId))
      mockRefreshStateBenefits(userData.taxYear, userData.nino, userData.mtdItId, Left(ApiServiceError("some-error")))

      await(underTest.saveClaim(userData)) shouldBe Left(ApiServiceError("some-error"))
    }

    "return error when clear fails" in {
      val userData = aStateBenefitsUserData
      mockFind(userData.nino, sessionDataId, Right(userData))
      mockSaveStateBenefitsUserData(userData, Right(benefitId))
      mockRefreshStateBenefits(userData.taxYear, userData.nino, userData.mtdItId, Right(()))
      mockClear(userData.sessionId, result = Left(MongoError("some-error")))

      await(underTest.saveClaim(userData)) shouldBe Left(MongoError("some-error"))
    }

    "perform save a claim when all calls succeed when provided with a sessionId" in {
      val userData = aStateBenefitsUserData
      mockFind(userData.nino, sessionDataId, Right(userData))
      mockSaveStateBenefitsUserData(userData, Right(benefitId))
      mockRefreshStateBenefits(userData.taxYear, userData.nino, userData.mtdItId, Right(()))
      mockClear(userData.sessionId, result = Right(()))

      await(underTest.saveClaim(userData)) shouldBe Right(())
    }

    "perform save a claim when all calls succeed when a sessionId is not provided" in {
      val userData = aStateBenefitsUserData
      mockSaveStateBenefitsUserData(userData, Right(benefitId))
      mockRefreshStateBenefits(userData.taxYear, userData.nino, userData.mtdItId, Right(()))
      mockClear(userData.sessionId, result = Right(()))

      await(underTest.saveClaim(userData, useSessionData = false)) shouldBe Right(())
    }
  }

  ".removeClaim(...) when" when {
    "is new claim" should {
      val userData = aStateBenefitsUserData.copy(benefitDataType = CustomerAdded.name, claim = Some(aClaimCYAModel.copy(benefitId = None)))
      "return error when" when {
        "stateBenefitsUserDataRepository.find(...) fails" in {
          mockFind(userData.nino, userData.sessionDataId.get, Left(DataNotUpdatedError))

          await(underTest.removeClaim(userData.nino, userData.sessionDataId.get)) shouldBe Left(DataNotUpdatedError)
        }

        "submissionService.refreshStateBenefits(...) fails" in {
          mockFind(userData.nino, userData.sessionDataId.get, Right(userData))
          mockRefreshStateBenefits(userData.taxYear, userData.nino, userData.mtdItId, Left(ApiServiceError("some-error")))

          await(underTest.removeClaim(userData.nino, userData.sessionDataId.get)) shouldBe Left(ApiServiceError("some-error"))
        }

        "stateBenefitsUserDataRepository.clear(...) fails" in {
          mockFind(userData.nino, userData.sessionDataId.get, Right(userData))
          mockRefreshStateBenefits(userData.taxYear, userData.nino, userData.mtdItId, Right(()))
          mockClear(userData.sessionId, result = Left(ApiServiceError("some-error")))

          await(underTest.removeClaim(userData.nino, userData.sessionDataId.get)) shouldBe Left(ApiServiceError("some-error"))
        }
      }

      "succeed when all calls succeed" in {
        mockFind(userData.nino, userData.sessionDataId.get, Right(userData))
        mockRefreshStateBenefits(userData.taxYear, userData.nino, userData.mtdItId, Right(()))
        mockClear(userData.sessionId, result = Right(()))

        await(underTest.removeClaim(userData.nino, userData.sessionDataId.get)) shouldBe Right(())
      }
    }

    "is customer added" should {
      val userData = aStateBenefitsUserData.copy(benefitDataType = CustomerAdded.name)
      "return error when" when {
        "stateBenefitsUserDataRepository.find(...) fails" in {
          mockFind(userData.nino, userData.sessionDataId.get, Left(DataNotUpdatedError))

          await(underTest.removeClaim(userData.nino, userData.sessionDataId.get)) shouldBe Left(DataNotUpdatedError)
        }

        "ifService.removeOrIgnoreClaim(...) fails" in {
          mockFind(userData.nino, userData.sessionDataId.get, Right(userData))
          mockRemoveOrIgnoreClaim(userData, Left(ApiServiceError("some-error")))

          await(underTest.removeClaim(userData.nino, userData.sessionDataId.get)) shouldBe Left(ApiServiceError("some-error"))
        }

        "submissionService.refreshStateBenefits(...) fails" in {
          mockFind(userData.nino, userData.sessionDataId.get, Right(userData))
          mockRemoveOrIgnoreClaim(userData, Right(()))
          mockRefreshStateBenefits(userData.taxYear, userData.nino, userData.mtdItId, Left(ApiServiceError("some-error")))

          await(underTest.removeClaim(userData.nino, userData.sessionDataId.get)) shouldBe Left(ApiServiceError("some-error"))
        }

        "stateBenefitsUserDataRepository.clear(...) fails" in {
          mockFind(userData.nino, userData.sessionDataId.get, Right(userData))
          mockRemoveOrIgnoreClaim(userData, Right(()))
          mockRefreshStateBenefits(userData.taxYear, userData.nino, userData.mtdItId, Right(()))
          mockClear(userData.sessionId, result = Left(ApiServiceError("some-error")))

          await(underTest.removeClaim(userData.nino, userData.sessionDataId.get)) shouldBe Left(ApiServiceError("some-error"))
        }
      }

      "succeed when all calls succeed" in {
        mockFind(userData.nino, userData.sessionDataId.get, Right(userData))
        mockRemoveOrIgnoreClaim(userData, Right(()))
        mockRefreshStateBenefits(userData.taxYear, userData.nino, userData.mtdItId, Right(()))
        mockClear(userData.sessionId, result = Right(()))

        await(underTest.removeClaim(userData.nino, userData.sessionDataId.get)) shouldBe Right(())
      }
    }

    "is HMRC data" should {
      val userData = aStateBenefitsUserData.copy(benefitDataType = HmrcData.name)
      "return error when" when {
        "stateBenefitsUserDataRepository.find(...) fails" in {
          mockFind(userData.nino, userData.sessionDataId.get, Left(DataNotUpdatedError))

          await(underTest.removeClaim(userData.nino, userData.sessionDataId.get)) shouldBe Left(DataNotUpdatedError)
        }

        "ifService.removeOrIgnoreClaim(...) fails" in {
          mockFind(userData.nino, userData.sessionDataId.get, Right(userData))
          mockRemoveOrIgnoreClaim(userData, Left(ApiServiceError("some-error")))

          await(underTest.removeClaim(userData.nino, userData.sessionDataId.get)) shouldBe Left(ApiServiceError("some-error"))
        }

        "submissionService.refreshStateBenefits(...) fails" in {
          mockFind(userData.nino, userData.sessionDataId.get, Right(userData))
          mockRemoveOrIgnoreClaim(userData, Right(()))
          mockRefreshStateBenefits(userData.taxYear, userData.nino, userData.mtdItId, Left(ApiServiceError("some-error")))

          await(underTest.removeClaim(userData.nino, userData.sessionDataId.get)) shouldBe Left(ApiServiceError("some-error"))
        }

        "stateBenefitsUserDataRepository.clear(...) fails" in {
          mockFind(userData.nino, userData.sessionDataId.get, Right(userData))
          mockRemoveOrIgnoreClaim(userData, Right(()))
          mockRefreshStateBenefits(userData.taxYear, userData.nino, userData.mtdItId, Right(()))
          mockClear(userData.sessionId, result = Left(ApiServiceError("some-error")))

          await(underTest.removeClaim(userData.nino, userData.sessionDataId.get)) shouldBe Left(ApiServiceError("some-error"))
        }
      }

      "succeed when all calls succeed" in {
        mockFind(userData.nino, userData.sessionDataId.get, Right(userData))
        mockRemoveOrIgnoreClaim(userData, Right(()))
        mockRefreshStateBenefits(userData.taxYear, userData.nino, userData.mtdItId, Right(()))
        mockClear(userData.sessionId, result = Right(()))

        await(underTest.removeClaim(userData.nino, userData.sessionDataId.get)) shouldBe Right(())
      }
    }

    "is customer override" should {
      val userData = aStateBenefitsUserData.copy(benefitDataType = CustomerOverride.name)
      "return error when" when {
        "stateBenefitsUserDataRepository.find(...) fails" in {
          mockFind(userData.nino, userData.sessionDataId.get, Left(DataNotUpdatedError))

          await(underTest.removeClaim(userData.nino, userData.sessionDataId.get)) shouldBe Left(DataNotUpdatedError)
        }

        "removeOrIgnoreClaim(...) fails" in {
          mockFind(userData.nino, userData.sessionDataId.get, Right(userData))
          mockRemoveCustomerOverride(userData, Left(ApiServiceError("some-error")))

          await(underTest.removeClaim(userData.nino, userData.sessionDataId.get)) shouldBe Left(ApiServiceError("some-error"))
        }

        "submissionService.refreshStateBenefits(...) fails" in {
          mockFind(userData.nino, userData.sessionDataId.get, Right(userData))
          mockRemoveCustomerOverride(userData, Right(()))
          mockRefreshStateBenefits(userData.taxYear, userData.nino, userData.mtdItId, Left(ApiServiceError("some-error")))

          await(underTest.removeClaim(userData.nino, userData.sessionDataId.get)) shouldBe Left(ApiServiceError("some-error"))
        }

        "stateBenefitsUserDataRepository.clear(...) fails" in {
          mockFind(userData.nino, userData.sessionDataId.get, Right(userData))
          mockRemoveCustomerOverride(userData, Right(()))
          mockRefreshStateBenefits(userData.taxYear, userData.nino, userData.mtdItId, Right(()))
          mockClear(userData.sessionId, result = Left(ApiServiceError("some-error")))

          await(underTest.removeClaim(userData.nino, userData.sessionDataId.get)) shouldBe Left(ApiServiceError("some-error"))
        }
      }

      "succeed when all calls succeed" in {
        mockFind(userData.nino, userData.sessionDataId.get, Right(userData))
        mockRemoveCustomerOverride(userData, Right(()))
        mockRefreshStateBenefits(userData.taxYear, userData.nino, userData.mtdItId, Right(()))
        mockClear(userData.sessionId, result = Right(()))

        await(underTest.removeClaim(userData.nino, userData.sessionDataId.get)) shouldBe Right(())
      }
    }
  }

  ".removeClaimById" should {
    val userData  = aStateBenefitsUserData.copy(benefitDataType = CustomerAdded.name, claim = Some(aClaimCYAModel.copy(benefitId = None)))
    val taxYear   = userData.taxYear
    val nino      = userData.nino
    val mtdItId   = userData.mtdItId
    val benefitId = userData.sessionDataId.value
    "return error when" when {
      "update cache call fails" in {
        mockRemoveClaim(nino, taxYear, benefitId)(result = Right(()))
        mockRefreshStateBenefits(taxYear, nino, mtdItId, Left(ApiServiceError("some-error")))

        await(underTest.removeClaimById(nino, taxYear, mtdItId, benefitId)) shouldBe Left(ApiServiceError("some-error"))
      }
      "ifs call fails" in {
        mockRemoveClaim(nino, taxYear, benefitId)(result = Left(ApiServiceError("some-error")))

        await(underTest.removeClaimById(nino, taxYear, mtdItId, benefitId)) shouldBe Left(ApiServiceError("some-error"))
      }
    }

    "succeed when all calls succeed" in {
      mockRefreshStateBenefits(taxYear, nino, mtdItId, Right(()))
      mockRemoveClaim(nino, taxYear, benefitId)(result = Right())

      await(underTest.removeClaimById(nino, taxYear, mtdItId, benefitId)) shouldBe Right(())
    }

  }

  ".restoreClaim(...)" should {
    val userData = aStateBenefitsUserData
    "return error when stateBenefitsUserDataRepository.find(...) returns error" in {
      mockFind(userData.nino, userData.sessionDataId.get, Left(DataNotUpdatedError))

      await(underTest.restoreClaim(userData.nino, userData.sessionDataId.get)) shouldBe Left(DataNotUpdatedError)
    }

    "return error when unIgnoreClaim fails" in {
      mockFind(userData.nino, userData.sessionDataId.get, Right(userData))
      mockUnIgnoreClaim(userData, Left(ApiServiceError("some-error")))

      await(underTest.restoreClaim(userData.nino, userData.sessionDataId.get)) shouldBe Left(ApiServiceError("some-error"))
    }

    "return error when refreshStateBenefits fails" in {
      mockFind(userData.nino, userData.sessionDataId.get, Right(userData))
      mockUnIgnoreClaim(userData, Right(()))
      mockRefreshStateBenefits(userData.taxYear, userData.nino, userData.mtdItId, Left(ApiServiceError("some-error")))

      await(underTest.restoreClaim(userData.nino, userData.sessionDataId.get)) shouldBe Left(ApiServiceError("some-error"))
    }

    "return error when clear fails" in {
      mockFind(userData.nino, userData.sessionDataId.get, Right(userData))
      mockUnIgnoreClaim(userData, Right(()))
      mockRefreshStateBenefits(userData.taxYear, userData.nino, userData.mtdItId, Right(()))
      mockClear(userData.sessionId, result = Left(ApiServiceError("some-error")))

      await(underTest.restoreClaim(userData.nino, userData.sessionDataId.get)) shouldBe Left(ApiServiceError("some-error"))
    }

    "perform unIgnoreClaim when all calls succeed" in {
      mockFind(userData.nino, userData.sessionDataId.get, Right(userData))
      mockUnIgnoreClaim(userData, Right(()))
      mockRefreshStateBenefits(userData.taxYear, userData.nino, userData.mtdItId, Right(()))
      mockClear(userData.sessionId, result = Right(()))

      await(underTest.restoreClaim(userData.nino, userData.sessionDataId.get)) shouldBe Right(())
    }
  }
}
