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

package support.builders.api

import models.api.AddStateBenefit
import models.api.BenefitType.JobSeekersAllowance
import support.utils.TaxYearUtils.taxYearEOY

import java.time.LocalDate

object AddStateBenefitBuilder {

  val anAddStateBenefit: AddStateBenefit = AddStateBenefit(
    benefitType = JobSeekersAllowance.typeName,
    startDate = LocalDate.parse(s"${taxYearEOY - 1}-01-01"),
    endDate = Some(LocalDate.parse(s"${taxYearEOY - 1}-02-01"))
  )
}
