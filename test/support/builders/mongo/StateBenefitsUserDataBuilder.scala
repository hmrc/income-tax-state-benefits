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

package support.builders.mongo

import models.mongo.StateBenefitsUserData
import support.builders.UserBuilder.aUser
import support.builders.mongo.ClaimCYAModelBuilder.aClaimCYAModel
import support.utils.TaxYearUtils.taxYear

import java.util.UUID

object StateBenefitsUserDataBuilder {

  val aStateBenefitsUserData: StateBenefitsUserData = StateBenefitsUserData(
    id = Some(UUID.fromString("558238ef-d2ff-4839-bd6d-307324d6fe37")),
    sessionId = "sessionId-eb3158c2-0aff-4ce8-8d1b-f2208ace52fe",
    mtdItId = aUser.mtditid,
    nino = "AA123456A",
    taxYear = taxYear,
    isPriorSubmission = false,
    claim = Some(aClaimCYAModel)
  )
}
