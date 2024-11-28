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

import cats.data.EitherT
import config.AppConfig
import models.api.{AllStateBenefitsData, CustomerAddedStateBenefit, StateBenefit}
import models.errors.ServiceError
import models.mongo.JourneyAnswers
import models.taskList.TaskStatus.{Completed, InProgress, NotStarted}
import models.taskList._
import play.api.Logging
import repositories.JourneyAnswersRepository
import uk.gov.hmrc.http.HeaderCarrier

import java.time.Instant
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CommonTaskListService @Inject()(appConfig: AppConfig,
                                      service: StateBenefitsService,
                                      repository: JourneyAnswersRepository) extends Logging {

  def get(taxYear: Int, nino: String, mtdItId: String)
         (implicit ec: ExecutionContext,hc: HeaderCarrier): Future[Seq[TaskListSection]] = {

    val jsaJourneyName: String = "jobseekers-allowance"
    val esaJourneyName: String = "employment-support-allowance"
    val jsaUrl: String = s"${appConfig.stateBenefitsFrontendUrl}/$taxYear/$jsaJourneyName/claims"
    val esaUrl: String = s"${appConfig.stateBenefitsFrontendUrl}/$taxYear/$esaJourneyName/claims"

    val emptyTaskList: Seq[TaskListSection] = Seq(
      TaskListSection(SectionTitle.JsaTitle, None),
      TaskListSection(SectionTitle.EsaTitle, None)
    )

    def result: EitherT[Future, ServiceError, Seq[TaskListSection]] = for {
      allStateBenefitsDataOpt <- EitherT(service.getAllStateBenefitsData(taxYear, nino))
      esaJourneyAnswersOpt <- EitherT.right(repository.get(mtdItId, taxYear, journey = esaJourneyName))
      jsaJourneyAnswersOpt <- EitherT.right(repository.get(mtdItId, taxYear, journey = jsaJourneyName))
      taskList = toTaskList(allStateBenefitsDataOpt, esaJourneyAnswersOpt, jsaJourneyAnswersOpt)
    } yield taskList

    def toTaskList(allStateBenefitsDataOpt: Option[AllStateBenefitsData],
                   esaJourneyAnswersOpt: Option[JourneyAnswers],
                   jsaJourneyAnswersOpt: Option[JourneyAnswers]): Seq[TaskListSection] = {
      allStateBenefitsDataOpt.fold(emptyTaskList)(allStateBenefitsData => {
        val jsaTask: Option[Seq[TaskListSectionItem]] = getStateBenefitTasks(
          hmrcAdded = allStateBenefitsData.stateBenefitsData.flatMap(_.jobSeekersAllowances),
          customerAdded = allStateBenefitsData.customerAddedStateBenefitsData.flatMap(_.jobSeekersAllowances),
          taskTitle = TaskTitle.JSA,
          url = jsaUrl,
          journeyAnswers = jsaJourneyAnswersOpt
        )
        val esaTask: Option[Seq[TaskListSectionItem]] = getStateBenefitTasks(
          hmrcAdded = allStateBenefitsData.stateBenefitsData.flatMap(_.employmentSupportAllowances),
          customerAdded = allStateBenefitsData.customerAddedStateBenefitsData.flatMap(_.employmentSupportAllowances),
          taskTitle = TaskTitle.ESA,
          url = esaUrl,
          journeyAnswers = esaJourneyAnswersOpt
        )
        Seq(
          TaskListSection(SectionTitle.JsaTitle, jsaTask),
          TaskListSection(SectionTitle.EsaTitle, esaTask)
        )
      })
    }

    result.leftMap(_ => emptyTaskList).merge
  }

  private def getStateBenefitTasks(hmrcAdded: Option[Set[StateBenefit]],
                                   customerAdded: Option[Set[CustomerAddedStateBenefit]],
                                   taskTitle: TaskTitle,
                                   url: String,
                                   journeyAnswers: Option[JourneyAnswers]): Option[Seq[TaskListSectionItem]] = {
    val hmrcSubmittedOn: Instant =
      hmrcAdded.flatMap(
        _.headOption.flatMap(
          _.submittedOn
        )
      ).getOrElse(Instant.MIN)

    val customerSubmittedOn: Instant =
      customerAdded.flatMap(
        _.headOption.flatMap(
          _.submittedOn
        )
      ).getOrElse(Instant.MIN)

    val hmrcHeldDataNewer: Boolean = !hmrcSubmittedOn.isBefore(customerSubmittedOn)

    (hmrcAdded, customerAdded, journeyAnswers) match {
      case (Some(_), _, _) if hmrcHeldDataNewer =>
        Some(Seq(TaskListSectionItem(taskTitle, TaskStatus.CheckNow, Some(url))))
      case (_, _, Some(journeyAnswers)) =>
        val status: TaskStatus = journeyAnswers.data.value("status").validate[TaskStatus].asOpt match {
          case Some(TaskStatus.Completed) => Completed
          case Some(TaskStatus.InProgress) => InProgress
          case _ =>
            logger.info("[CommonTaskListService][getStatus] status stored in an invalid format, setting as 'Not yet started'.")
            NotStarted
        }
        Some(Seq(TaskListSectionItem(taskTitle, status, Some(url))))
      case (_, Some(_), _) =>
        Some(Seq(TaskListSectionItem(taskTitle, TaskStatus.Completed, Some(url))))
        Some(Seq(TaskListSectionItem(taskTitle, if(appConfig.sectionCompletedQuestionEnabled) TaskStatus.InProgress else TaskStatus.Completed, Some(url))))

      case (_, _, _) => None
    }
  }
}
