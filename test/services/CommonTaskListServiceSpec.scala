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

import models.api._
import models.errors.ApiServiceError
import models.tasklist._
import org.scalamock.scalatest.proxy.MockFactory
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import support.UnitTest
import support.mocks.MockStateBenefitsService
import support.providers.AppConfigStubProvider
import uk.gov.hmrc.http.HeaderCarrier

import java.time.{Instant, LocalDate}
import java.util.UUID
import scala.concurrent.ExecutionContext

class CommonTaskListServiceSpec extends UnitTest with MockFactory with AppConfigStubProvider with MockStateBenefitsService {

  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val hc: HeaderCarrier = HeaderCarrier()

  private val stateBenefitsService = mockStateBenefitsService

  private val service: CommonTaskListService = new CommonTaskListService(appConfigStub, stateBenefitsService)

  private val nino: String = "12345678"
  private val taxYear: Int = 1234

  val stateBenefits: Option[StateBenefitsData] = Some(StateBenefitsData(
    incapacityBenefits = None,
    statePension = None,
    statePensionLumpSum = None,
    employmentSupportAllowances = Some(Set(StateBenefit(
      benefitId = UUID.fromString("a1e8057e-fbbc-47a8-a8b4-78d9f015c937"),
      startDate = LocalDate.parse(s"${taxYear - 1}-09-23"),
      endDate = Some(LocalDate.parse(s"$taxYear-08-23")),
      dateIgnored = None,
      submittedOn = Some(Instant.parse(s"$taxYear-11-13T19:23:00Z")),
      amount = Some(44545.43),
      taxPaid = Some(35343.23)
    ))),
    jobSeekersAllowances = Some(Set(StateBenefit(
      benefitId = UUID.fromString("a1e8057e-fbbc-47a8-a8b4-78d9f015c938"),
      startDate = LocalDate.parse(s"${taxYear - 1}-09-19"),
      endDate = Some(LocalDate.parse(s"$taxYear-09-23")),
      dateIgnored = None,
      submittedOn = Some(Instant.parse(s"$taxYear-07-10T18:23:00Z")),
      amount = Some(33223.12),
      taxPaid = Some(44224.56)
    ))),
    bereavementAllowance = None,
    other = None
  ))

  val customerAddedStateBenefit: Option[CustomerAddedStateBenefitsData] = Some(CustomerAddedStateBenefitsData(
    incapacityBenefits = None,
    statePensions = None,
    statePensionLumpSums = None,
    employmentSupportAllowances = Some(Set(CustomerAddedStateBenefit(
      benefitId = UUID.fromString("a1e8057e-fbbc-47a8-a8b4-78d9f015c988"),
      startDate = LocalDate.parse(s"${taxYear - 1}-09-11"),
      endDate = Some(LocalDate.parse(s"$taxYear-06-13")),
      submittedOn = Some(Instant.parse(s"$taxYear-02-10T11:20:00Z")),
      amount = Some(45424.23),
      taxPaid = Some(23232.34)
    ))),
    jobSeekersAllowances = Some(Set(CustomerAddedStateBenefit(
      benefitId = UUID.fromString("a1e8057e-fbbc-47a8-a8b4-78d9f015c990"),
      startDate = LocalDate.parse(s"${taxYear - 1}-07-10"),
      endDate = Some(LocalDate.parse(s"$taxYear-05-11")),
      submittedOn = Some(Instant.parse(s"$taxYear-05-13T14:23:00Z")),
      amount = Some(34343.78),
      taxPaid = Some(3433.56)
    ))),
    bereavementAllowances = None,
    otherStateBenefits = None
  ))

  val latestCustomerAddedStateBenefit: Option[CustomerAddedStateBenefitsData] = Some(CustomerAddedStateBenefitsData(
    incapacityBenefits = None,
    statePensions = None,
    statePensionLumpSums = None,
    employmentSupportAllowances = Some(Set(CustomerAddedStateBenefit(
      benefitId = UUID.fromString("a1e8057e-fbbc-47a8-a8b4-78d9f015c988"),
      startDate = LocalDate.parse(s"${taxYear - 1}-09-11"),
      endDate = Some(LocalDate.parse(s"$taxYear-06-13")),
      submittedOn = Some(Instant.parse(s"$taxYear-12-10T11:20:00Z")),
      amount = Some(45424.23),
      taxPaid = Some(23232.34)
    ))),
    jobSeekersAllowances = Some(Set(CustomerAddedStateBenefit(
      benefitId = UUID.fromString("a1e8057e-fbbc-47a8-a8b4-78d9f015c990"),
      startDate = LocalDate.parse(s"${taxYear - 1}-07-10"),
      endDate = Some(LocalDate.parse(s"$taxYear-05-11")),
      submittedOn = Some(Instant.parse(s"$taxYear-12-10T14:23:00Z")),
      amount = Some(34343.78),
      taxPaid = Some(3433.56)
    ))),
    bereavementAllowances = None,
    otherStateBenefits = None
  ))

  val fullStateBenefitsResult: Right[Nothing, Some[AllStateBenefitsData]] = Right(Some(AllStateBenefitsData(stateBenefits, customerAddedStateBenefit)))

  val customerLatestResult: Right[Nothing, Some[AllStateBenefitsData]] = Right(Some(AllStateBenefitsData(stateBenefits, latestCustomerAddedStateBenefit)))

  val emptyStateBenefitsResult: Left[ApiServiceError, Nothing] = Left(ApiServiceError(""))

  val checkNowTaskSections: List[TaskListSection] =
    List(
      TaskListSection(
        SectionTitle.JsaTitle,
        Some(List(TaskListSectionItem(TaskTitle.JSA, TaskStatus.CheckNow, Some("http://localhost:9376/1234/jobseekers-allowance/claims"))))),
      TaskListSection(
        SectionTitle.EsaTitle,
        Some(List(TaskListSectionItem(TaskTitle.ESA, TaskStatus.CheckNow, Some("http://localhost:9376/1234/employment-support-allowance/claims")))))
    )

  val completedTaskSections: List[TaskListSection] = List(
    TaskListSection(
      SectionTitle.JsaTitle,
      Some(List(TaskListSectionItem(TaskTitle.JSA, TaskStatus.Completed, Some("http://localhost:9376/1234/jobseekers-allowance/claims"))))),
    TaskListSection(
      SectionTitle.EsaTitle,
      Some(List(TaskListSectionItem(TaskTitle.ESA, TaskStatus.Completed, Some("http://localhost:9376/1234/employment-support-allowance/claims")))))
  )

  val emptyTaskSections: List[TaskListSection] = List(TaskListSection(SectionTitle.JsaTitle, None), TaskListSection(SectionTitle.EsaTitle, None))

  "CommonTaskListService.get" should {

    "return a full task list section model" in {

      mockGetAllStateBenefitsData(taxYear, nino, fullStateBenefitsResult)

      val underTest = service.get(taxYear, nino)

      await(underTest) mustBe checkNowTaskSections
    }

    "return a minimal task list section model" in {

      val result = Right(
        Some(AllStateBenefitsData(Some(
          StateBenefitsData(employmentSupportAllowances = Some(Set(StateBenefit(
            benefitId = UUID.fromString("a1e8057e-fbbc-47a8-a8b4-78d9f015c988"),
            startDate = LocalDate.parse(s"${taxYear - 1}-09-11"),
            endDate = Some(LocalDate.parse(s"$taxYear-06-13")),
            submittedOn = Some(Instant.parse(s"$taxYear-02-10T11:20:00Z")),
            amount = Some(45424.23),
            taxPaid = Some(23232.34)
          ))))
        )))
      )

      mockGetAllStateBenefitsData(taxYear, nino, result)

      val underTest = service.get(taxYear, nino)

      await(underTest) mustBe List(
        TaskListSection(SectionTitle.JsaTitle,
          Some(List(
            TaskListSectionItem(TaskTitle.JSA, TaskStatus.CheckNow, Some("http://localhost:9376/1234/jobseekers-allowance/claims")),
          ))),
        TaskListSection(SectionTitle.EsaTitle, None)
      )
    }

    "return an empty task list section model" in {

      mockGetAllStateBenefitsData(taxYear, nino, emptyStateBenefitsResult)

      val underTest = service.get(taxYear, nino)

      await(underTest) mustBe emptyTaskSections
    }

    "return tasks as completed when customer data is more recent than hmrc data" in {

      mockGetAllStateBenefitsData(taxYear, nino, customerLatestResult)

      val underTest = service.get(taxYear, nino)

      await(underTest) mustBe completedTaskSections
    }
  }
}
