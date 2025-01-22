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

trait PrePopulationDataWrapper[I] {
  val employmentSupportAllowances: Option[Set[I]]
  val jobSeekersAllowances: Option[Set[I]]

  def hasDataForOpt(dataOpt: Option[Set[I]]): Boolean =
    dataOpt.fold(false)(data => data.nonEmpty)

  val (hasEsaData, hasJsaData) = (
    hasDataForOpt(employmentSupportAllowances),
    hasDataForOpt(jobSeekersAllowances)
  )
}
