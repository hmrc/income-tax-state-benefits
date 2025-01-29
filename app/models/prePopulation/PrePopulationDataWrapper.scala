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

sealed trait PrePopulationDataWrapper[I] {
  val employmentSupportAllowances: Option[Set[I]]
  val jobSeekersAllowances: Option[Set[I]]

  protected[prePopulation] def hasDataForOpt(dataOpt: Option[Set[I]]): Boolean =
    dataOpt.fold(false)(data => data.nonEmpty)

  val hasPensionsData: Boolean
  val hasPensionsLumpSumData: Boolean

  val (hasEsaData, hasJsaData) = (
    hasDataForOpt(employmentSupportAllowances),
    hasDataForOpt(jobSeekersAllowances)
  )
}

object PrePopulationDataWrapper {
  def isPrePopulated[I](data: Option[PrePopulationDataWrapper[I]]): (Boolean, Boolean, Boolean, Boolean) =
    data.fold((false, false, false, false))(data =>
      (data.hasEsaData, data.hasJsaData, data.hasPensionsData, data.hasPensionsLumpSumData)
    )
}

trait CustomerPrePopulationDataWrapper extends PrePopulationDataWrapper[CustomerAddedStateBenefit] {
  val statePensions: Option[Set[CustomerAddedStateBenefit]]
  val statePensionLumpSums: Option[Set[CustomerAddedStateBenefit]]

  val (hasPensionsData, hasPensionsLumpSumData) = (
    hasDataForOpt(statePensions),
    hasDataForOpt(statePensionLumpSums)
  )
}

trait HmrcHeldPrePopulationDataWrapper extends PrePopulationDataWrapper[StateBenefit] {
  val statePension: Option[StateBenefit]
  val statePensionLumpSum: Option[StateBenefit]

  override def hasDataForOpt(dataOpt: Option[Set[StateBenefit]]): Boolean =
    dataOpt.fold(false)(data => data.exists(_.dateIgnored.isEmpty))

  val (hasPensionsData, hasPensionsLumpSumData) = (
    hasDataForOpt(statePension.map(Set(_))),
    hasDataForOpt(statePensionLumpSum.map(Set(_)))
  )
}
