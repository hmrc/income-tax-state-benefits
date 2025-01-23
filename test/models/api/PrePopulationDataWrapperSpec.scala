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

package models.api

import support.UnitTest

class PrePopulationDataWrapperSpec extends UnitTest {
  case class TestPrePop(employmentSupportAllowances: Option[Set[String]],
                        jobSeekersAllowances: Option[Set[String]]) extends PrePopulationDataWrapper[String]

  "PrePopulationDataWrapper" when {
    "hasDataForOpt" should {
      "return 'true' when data exists" in {
        TestPrePop(None, None).hasDataForOpt(Some(Set(""))) shouldBe true
      }

      "return 'false' when data exists and is empty" in {
        TestPrePop(None, None).hasDataForOpt(Some(Set())) shouldBe false
      }

      "return 'false' when data is 'None'" in {
        TestPrePop(None, None).hasDataForOpt(None) shouldBe false
      }
    }

    "hasEsa" should {
      "return 'true' when data exists for ESA" in {
        TestPrePop(
          employmentSupportAllowances = Some(Set("otherValue")),
          jobSeekersAllowances = Some(Set("value"))
        ).hasEsaData shouldBe true
      }

      "return 'false' when data does not exist for ESA" in {
        TestPrePop(
          employmentSupportAllowances = Some(Set()),
          jobSeekersAllowances = Some(Set("value"))
        ).hasEsaData shouldBe false
      }

      "return 'false' when ESA is 'None'" in {
        TestPrePop(
          employmentSupportAllowances = None,
          jobSeekersAllowances = Some(Set("value"))
        ).hasEsaData shouldBe false
      }
    }

    "hasJsa" should {
      "return 'true' when data exists for JSA" in {
        TestPrePop(
          employmentSupportAllowances = Some(Set("otherValue")),
          jobSeekersAllowances = Some(Set("value"))
        ).hasEsaData shouldBe true
      }

      "return 'false' when data does not exist for JSA" in {
        TestPrePop(
          employmentSupportAllowances = Some(Set()),
          jobSeekersAllowances = Some(Set())
        ).hasEsaData shouldBe false
      }

      "return 'false' when JSA is 'None'" in {
        TestPrePop(
          employmentSupportAllowances = None,
          jobSeekersAllowances = None
        ).hasEsaData shouldBe false
      }
    }
  }
}



