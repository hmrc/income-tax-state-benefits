/*
 * Copyright 2025 HM Revenue & Customs
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

package models.prePopulation

import models.api.{CustomerAddedStateBenefit, StateBenefit}
import support.UnitTest
import support.builders.api.StateBenefitBuilder.aStateBenefit
import support.builders.api.StateBenefitsDataBuilder.aStateBenefitsData

import java.time.{Instant, LocalDate}
import java.util.UUID

class PrePopulationDataWrapperSpec extends UnitTest {
  case class TestCustomerPrePop(employmentSupportAllowances: Option[Set[CustomerAddedStateBenefit]],
                                jobSeekersAllowances: Option[Set[CustomerAddedStateBenefit]],
                                statePensions: Option[Set[CustomerAddedStateBenefit]],
                                statePensionLumpSums: Option[Set[CustomerAddedStateBenefit]])
    extends CustomerPrePopulationDataWrapper

  case class TestHmrcPrePop(employmentSupportAllowances: Option[Set[StateBenefit]],
                            jobSeekersAllowances: Option[Set[StateBenefit]],
                            statePension: Option[StateBenefit],
                            statePensionLumpSum: Option[StateBenefit])
    extends HmrcHeldPrePopulationDataWrapper

  "CustomerPrePopulationDataWrapper" when {
    val dummyCustomerData = CustomerAddedStateBenefit(new UUID(5, 0), LocalDate.MIN)
    "hasDataForOpt" should {
      "return 'true' when data exists" in {
        TestCustomerPrePop(None, None, None, None).hasDataForOpt(Some(Set(dummyCustomerData))) shouldBe true
      }

      "return 'false' when data exists and is empty" in {
        TestCustomerPrePop(None, None, None, None).hasDataForOpt(Some(Set())) shouldBe false
      }

      "return 'false' when data is 'None'" in {
        TestCustomerPrePop(None, None, None, None).hasDataForOpt(None) shouldBe false
      }
    }

    "hasEsa" should {
      "return 'true' when data exists for ESA" in {
        TestCustomerPrePop(
          employmentSupportAllowances = Some(Set(dummyCustomerData)),
          jobSeekersAllowances = Some(Set(dummyCustomerData)),
          statePensions = None,
          statePensionLumpSums = None
        ).hasEsaData shouldBe true
      }

      "return 'false' when data does not exist for ESA" in {
        TestCustomerPrePop(
          employmentSupportAllowances = Some(Set()),
          jobSeekersAllowances = Some(Set(dummyCustomerData)),
          statePensions = None,
          statePensionLumpSums = None
        ).hasEsaData shouldBe false
      }

      "return 'false' when ESA is 'None'" in {
        TestCustomerPrePop(
          employmentSupportAllowances = None,
          jobSeekersAllowances = Some(Set(dummyCustomerData)),
          statePensions = None,
          statePensionLumpSums = None
        ).hasEsaData shouldBe false
      }
    }

    "hasJsa" should {
      "return 'true' when data exists for JSA" in {
        TestCustomerPrePop(
          employmentSupportAllowances = Some(Set(dummyCustomerData)),
          jobSeekersAllowances = Some(Set(dummyCustomerData)),
          statePensions = None,
          statePensionLumpSums = None
        ).hasJsaData shouldBe true
      }

      "return 'false' when data does not exist for JSA" in {
        TestCustomerPrePop(
          employmentSupportAllowances = Some(Set()),
          jobSeekersAllowances = Some(Set()),
          statePensions = None,
          statePensionLumpSums = None
        ).hasJsaData shouldBe false
      }

      "return 'false' when JSA is 'None'" in {
        TestCustomerPrePop(
          employmentSupportAllowances = None,
          jobSeekersAllowances = None,
          statePensions = None,
          statePensionLumpSums = None
        ).hasJsaData shouldBe false
      }
    }

    "hasPensionsData" should {
      "return 'true' when data exists for state pensions" in {
        TestCustomerPrePop(
          employmentSupportAllowances = Some(Set(dummyCustomerData)),
          jobSeekersAllowances = Some(Set(dummyCustomerData)),
          statePensions = Some(Set(dummyCustomerData)),
          statePensionLumpSums = None
        ).hasPensionsData shouldBe true
      }

      "return 'false' when data does not exist for state pensions" in {
        TestCustomerPrePop(
          employmentSupportAllowances = Some(Set()),
          jobSeekersAllowances = Some(Set()),
          statePensions = Some(Set()),
          statePensionLumpSums = None
        ).hasPensionsData shouldBe false
      }

      "return 'false' when state pensions is 'None'" in {
        TestCustomerPrePop(
          employmentSupportAllowances = None,
          jobSeekersAllowances = None,
          statePensions = None,
          statePensionLumpSums = None
        ).hasPensionsData shouldBe false
      }
    }

    "hasPensionsLumpSumData" should {
      "return 'true' when data exists for state pension lump sum" in {
        TestCustomerPrePop(
          employmentSupportAllowances = Some(Set(dummyCustomerData)),
          jobSeekersAllowances = Some(Set(dummyCustomerData)),
          statePensions = None,
          statePensionLumpSums = Some(Set(dummyCustomerData))
        ).hasPensionsLumpSumData shouldBe true
      }

      "return 'false' when data does not exist for state pension lump sum" in {
        TestCustomerPrePop(
          employmentSupportAllowances = Some(Set()),
          jobSeekersAllowances = Some(Set()),
          statePensions = None,
          statePensionLumpSums = Some(Set())
        ).hasPensionsLumpSumData shouldBe false
      }

      "return 'false' when state pension lump sum is 'None'" in {
        TestCustomerPrePop(
          employmentSupportAllowances = None,
          jobSeekersAllowances = None,
          statePensions = None,
          statePensionLumpSums = None
        ).hasPensionsLumpSumData shouldBe false
      }
    }
  }

  "HmrcHeldPrePopulationDataWrapper" when {
    val dummyHmrcData = StateBenefit(new UUID(5, 5), LocalDate.MIN)
    val dummyIgnoredHmrcData = StateBenefit(new UUID(5, 5), LocalDate.MIN, dateIgnored = Some(Instant.MIN))

    "hasDataForOpt" should {
      val stateBenefit = aStateBenefit
        .copy(startDate = LocalDate.parse("2019-04-23"))
        .copy(endDate = Some(LocalDate.parse("2020-08-13")))
        .copy(dateIgnored = Some(Instant.parse("2019-07-08T05:23:00Z")))
        .copy(submittedOn = Some(Instant.parse("2020-09-11T17:23:00Z")))

      val stateBenefitsData = aStateBenefitsData
        .copy(incapacityBenefits = Some(Set(stateBenefit.copy(benefitId = UUID.fromString("a1e8057e-fbbc-47a8-a8b4-78d9f015c934")))))
        .copy(statePension = Some(stateBenefit.copy(benefitId = UUID.fromString("a1e8057e-fbbc-47a8-a8b4-78d9f015c935"))))
        .copy(statePensionLumpSum = Some(stateBenefit.copy(benefitId = UUID.fromString("a1e8057e-fbbc-47a8-a8b4-78d9f015c936"))))
        .copy(employmentSupportAllowances = Some(Set(stateBenefit.copy(benefitId = UUID.fromString("a1e8057e-fbbc-47a8-a8b4-78d9f015c937")))))
        .copy(jobSeekersAllowances = Some(Set(stateBenefit.copy(benefitId = UUID.fromString("a1e8057e-fbbc-47a8-a8b4-78d9f015c938")))))
        .copy(bereavementAllowance = Some(stateBenefit.copy(benefitId = UUID.fromString("a1e8057e-fbbc-47a8-a8b4-78d9f015c939"))))
        .copy(other = Some(stateBenefit.copy(benefitId = UUID.fromString("a1e8057e-fbbc-47a8-a8b4-78d9f015c940"))))

      "return 'true' when non-ignored entries exist for an optional state benefit" in {
        val testData = Some(Set(
          stateBenefit.copy(dateIgnored = None),
          stateBenefit
        ))

        stateBenefitsData.hasDataForOpt(testData) shouldBe true
      }

      "return 'false' when an optional state benefit is 'None'" in {
        stateBenefitsData.hasDataForOpt(None) shouldBe false
      }

      "return 'false' when an optional state benefit is empty" in {
        stateBenefitsData.hasDataForOpt(Some(Set())) shouldBe false
      }

      "return 'false' when only ignored benefits exist for an optional state benefit" in {
        stateBenefitsData.hasDataForOpt(Some(Set(stateBenefit))) shouldBe false
      }
    }

    "hasEsa" should {
      "return 'true' when data exists for ESA" in {
        TestHmrcPrePop(
          employmentSupportAllowances = Some(Set(dummyHmrcData)),
          jobSeekersAllowances = Some(Set(dummyHmrcData)),
          statePension = None,
          statePensionLumpSum = None
        ).hasEsaData shouldBe true
      }

      "return 'false' when data does not exist for ESA" in {
        TestHmrcPrePop(
          employmentSupportAllowances = Some(Set()),
          jobSeekersAllowances = Some(Set(dummyHmrcData)),
          statePension = None,
          statePensionLumpSum = None
        ).hasEsaData shouldBe false
      }

      "return 'false' when ESA is 'None'" in {
        TestHmrcPrePop(
          employmentSupportAllowances = None,
          jobSeekersAllowances = Some(Set(dummyHmrcData)),
          statePension = None,
          statePensionLumpSum = None
        ).hasEsaData shouldBe false
      }
    }

    "hasJsa" should {
      "return 'true' when data exists for JSA" in {
        TestHmrcPrePop(
          employmentSupportAllowances = Some(Set(dummyHmrcData)),
          jobSeekersAllowances = Some(Set(dummyHmrcData)),
          statePension = None,
          statePensionLumpSum = None
        ).hasJsaData shouldBe true
      }

      "return 'false' when data does not exist for JSA" in {
        TestHmrcPrePop(
          employmentSupportAllowances = Some(Set()),
          jobSeekersAllowances = Some(Set()),
          statePension = None,
          statePensionLumpSum = None
        ).hasJsaData shouldBe false
      }

      "return 'false' when JSA is 'None'" in {
        TestHmrcPrePop(
          employmentSupportAllowances = None,
          jobSeekersAllowances = None,
          statePension = None,
          statePensionLumpSum = None
        ).hasJsaData shouldBe false
      }
    }

    "hasPensionsData" should {
      "return 'true' when data exists for state pensions" in {
        TestHmrcPrePop(
          employmentSupportAllowances = Some(Set(dummyHmrcData)),
          jobSeekersAllowances = Some(Set(dummyHmrcData)),
          statePension = Some(dummyHmrcData),
          statePensionLumpSum = None
        ).hasPensionsData shouldBe true
      }

      "return 'false' when data is ignored for state pensions" in {
        TestHmrcPrePop(
          employmentSupportAllowances = Some(Set()),
          jobSeekersAllowances = Some(Set()),
          statePension = Some(dummyIgnoredHmrcData),
          statePensionLumpSum = None
        ).hasPensionsData shouldBe false
      }

      "return 'false' when state pensions is 'None'" in {
        TestHmrcPrePop(
          employmentSupportAllowances = None,
          jobSeekersAllowances = None,
          statePension = None,
          statePensionLumpSum = None
        ).hasPensionsData shouldBe false
      }
    }

    "hasPensionsLumpSumData" should {
      "return 'true' when data exists for state pension lump sum" in {
        TestHmrcPrePop(
          employmentSupportAllowances = Some(Set(dummyHmrcData)),
          jobSeekersAllowances = Some(Set(dummyHmrcData)),
          statePension = None,
          statePensionLumpSum = Some(dummyHmrcData)
        ).hasPensionsLumpSumData shouldBe true
      }

      "return 'false' when data is ignored for state pensions" in {
        TestHmrcPrePop(
          employmentSupportAllowances = Some(Set()),
          jobSeekersAllowances = Some(Set()),
          statePension = None,
          statePensionLumpSum = Some(dummyIgnoredHmrcData)
        ).hasPensionsLumpSumData shouldBe false
      }

      "return 'false' when state pension lump sum is 'None'" in {
        TestHmrcPrePop(
          employmentSupportAllowances = None,
          jobSeekersAllowances = None,
          statePension = None,
          statePensionLumpSum = None
        ).hasPensionsLumpSumData shouldBe false
      }
    }
  }
}



