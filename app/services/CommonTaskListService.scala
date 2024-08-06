/*
 * Copyright 2024 HM Revenue & Customs
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

import config.AppConfig
import models.api.{CustomerAddedStateBenefit, StateBenefit}
import models.tasklist._
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CommonTaskListService @Inject()(appConfig: AppConfig,
                                      service: StateBenefitsService) {

  def get(taxYear: Int, nino: String)(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Seq[TaskListSection]] = {

    val jsaUrl: String = s"${appConfig.stateBenefitsFrontendUrl}/$taxYear/jobseekers-allowance/claims"
    val esaUrl: String = s"${appConfig.stateBenefitsFrontendUrl}/$taxYear/employment-support-allowance/claims"

    service.getAllStateBenefitsData(taxYear, nino).map {
      case Left(_) => None
      case Right(benefits) => benefits
    }.map {
      case Some(benefits) =>
        val jsaTask: Option[Seq[TaskListSectionItem]] = getStateBenefitTasks(
          benefits.stateBenefitsData.flatMap(_.jobSeekersAllowances),
          benefits.customerAddedStateBenefitsData.flatMap(_.jobSeekersAllowances),
          TaskTitle.JSA,
          jsaUrl
        )
        val esaTask: Option[Seq[TaskListSectionItem]] = getStateBenefitTasks(
          benefits.stateBenefitsData.flatMap(_.employmentSupportAllowances),
          benefits.customerAddedStateBenefitsData.flatMap(_.employmentSupportAllowances),
          TaskTitle.ESA,
          esaUrl
        )
        Seq(
          TaskListSection(SectionTitle.JsaTitle, jsaTask),
          TaskListSection(SectionTitle.EsaTitle, esaTask)
        )
      case None => Seq(
        TaskListSection(SectionTitle.JsaTitle, None),
        TaskListSection(SectionTitle.EsaTitle, None)
      )
    }
  }

  private def getStateBenefitTasks(hmrcAdded: Option[Set[StateBenefit]],
                          customerAdded: Option[Set[CustomerAddedStateBenefit]],
                          taskTitle: TaskTitle,
                          url: String): Option[Seq[TaskListSectionItem]] = {

    val hmrcSubmittedOn: Long =
      hmrcAdded.flatMap(_.headOption.flatMap(_.submittedOn.map(_.getEpochSecond)))
        .getOrElse(0)

    val customerSubmittedOn: Long =
      customerAdded.flatMap(_.headOption.flatMap(_.submittedOn.map(_.getEpochSecond)))
        .getOrElse(0)

    val hasCustomerData: Boolean = customerAdded.exists(_.nonEmpty)

    (hmrcSubmittedOn >= customerSubmittedOn, hasCustomerData) match {
      case (true, _) => Some(Seq(TaskListSectionItem(taskTitle, TaskStatus.CheckNow, Some(url))))
      case (false, true) => Some(Seq(TaskListSectionItem(taskTitle, TaskStatus.Completed, Some(url))))
      case (_, _) => None
    }
  }
}
