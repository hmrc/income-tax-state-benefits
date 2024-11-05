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

package controllers

import models.taskList.{SectionTitle, TaskListSection}
import org.scalamock.handlers.CallHandler5
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.http.Status.OK
import play.api.test.Helpers.status
import services.CommonTaskListService
import support.ControllerUnitTest
import support.mocks.MockAuthorisedAction
import support.providers.FakeRequestProvider
import support.utils.TaxYearUtils
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class CommonTaskListControllerSpec extends ControllerUnitTest with MockAuthorisedAction with FakeRequestProvider {
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  val nino: String = "123456789"
  val taxYear: Int = TaxYearUtils.taxYear

  val commonTaskListService: CommonTaskListService = mock[CommonTaskListService]
  val controller = new CommonTaskListController(commonTaskListService, mockAuthorisedAction, cc)

  def mockStateBenefitsService(): CallHandler5[Int, String, String, ExecutionContext, HeaderCarrier, Future[Seq[TaskListSection]]] = {
    (commonTaskListService.get(_: Int, _: String, _: String)(_: ExecutionContext, _: HeaderCarrier))
      .expects(*, *, *, *, *)
      .returning(Future.successful(Seq(TaskListSection(SectionTitle.EsaTitle, None))))
  }

  ".getCommonTaskList" should {
    "return a task list section model" in {
      val result = {
        mockAuthorisation()
        mockStateBenefitsService()
        controller.getCommonTaskList(taxYear, nino)(fakeRequest)
      }

      status(result) mustBe OK
    }
  }
}
