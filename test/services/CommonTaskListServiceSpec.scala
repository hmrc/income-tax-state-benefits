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
import models.mongo.JourneyAnswers
import models.taskList._
import org.scalamock.scalatest.proxy.MockFactory
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.libs.json.{JsObject, Json}
import support.UnitTest
import support.mocks.{MockJourneyAnswersRepository, MockStateBenefitsService}
import support.providers.AppConfigStubProvider
import uk.gov.hmrc.http.HeaderCarrier

import java.time.{Instant, LocalDate}
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class CommonTaskListServiceSpec extends UnitTest
  with MockFactory
  with AppConfigStubProvider
  with MockStateBenefitsService
  with MockJourneyAnswersRepository {

  private trait Test extends CommonTaskListServiceFixture {
    implicit val ec: ExecutionContext = ExecutionContext.global
    implicit val hc: HeaderCarrier = HeaderCarrier()

    val service: CommonTaskListService = new CommonTaskListService(
      appConfig = appConfigStub,
      service = mockStateBenefitsService,
      repository = mockJourneyAnswersRepo
    )

    val nino: String = "12345678"
    val taxYear: Int = 1234
    val mtdItId = "12345"
  }

  "CommonTaskListService" when {
    "an error occurs while retrieving benefits data from IF" should {
      "return an empty task list when IF returns an API error" in new Test {
        mockGetAllStateBenefitsData(
          taxYear = taxYear,
          nino = nino,
          result = Left(ApiServiceError("Dummy error"))
        )

        val underTest: Future[Seq[TaskListSection]] = service.get(
          taxYear = taxYear,
          nino = nino,
          mtdItId = mtdItId
        )

        await(underTest) mustBe emptyTaskSections
      }

      "handle appropriately when an exception occurs" in new Test {
        mockGetAllStateBenefitsDataException(
          taxYear = taxYear,
          nino = nino,
          result = new RuntimeException("Dummy Exception")
        )

        val underTest: Future[Seq[TaskListSection]] = service.get(
          taxYear = taxYear,
          nino = nino,
          mtdItId = mtdItId
        )

        assertThrows[RuntimeException](await(underTest))
      }
    }

    "an error occurs while retrieving ESA Journey Answers from the Journey Answers repository" should {
      "handle appropriately" in new Test {
        mockGetAllStateBenefitsData(
          taxYear = taxYear,
          nino = nino,
          result = fullStateBenefitsResult(taxYear)
        )

        mockGetJourneyAnswersException(
          mtdItId = mtdItId,
          taxYear = taxYear,
          journey = "employmentSupportAllowance",
          result = new RuntimeException("Dummy exception")
        )

        val underTest: Future[Seq[TaskListSection]] = service.get(
          taxYear = taxYear,
          nino = nino,
          mtdItId = mtdItId
        )

        assertThrows[RuntimeException](await(underTest))
      }
    }

    "an error occurs while retrieving JSA Journey Answers from the Journey Answers repository" should {
      "handle appropriately" in new Test {
        mockGetAllStateBenefitsData(
          taxYear = taxYear,
          nino = nino,
          result = fullStateBenefitsResult(taxYear)
        )

        mockGetJourneyAnswers(
          mtdItId = mtdItId,
          taxYear = taxYear,
          journey = "employmentSupportAllowance",
          result = Some(
            JourneyAnswers(
              mtdItId = mtdItId,
              taxYear = taxYear,
              journey = "employmentSupportAllowance",
              data = JsObject.empty,
              lastUpdated = Instant.ofEpochMilli(10)
            )
          )
        )

        mockGetJourneyAnswersException(
          mtdItId = mtdItId,
          taxYear = taxYear,
          journey = "jobSeekersAllowance",
          result = new RuntimeException("Dummy exception")
        )

        val underTest: Future[Seq[TaskListSection]] = service.get(
          taxYear = taxYear,
          nino = nino,
          mtdItId = mtdItId
        )

        assertThrows[RuntimeException](await(underTest))
      }
    }

    "the call to IF for benefits data returns a successful, but empty, response" should {
      "return an empty task list" in new Test {
        mockGetAllStateBenefitsData(
          taxYear = taxYear,
          nino = nino,
          result = Right(None)
        )

        mockGetJourneyAnswers(
          mtdItId = mtdItId,
          taxYear = taxYear,
          journey = "employmentSupportAllowance",
          result = Some(
            JourneyAnswers(
              mtdItId = mtdItId,
              taxYear = taxYear,
              journey = "employmentSupportAllowance",
              data = JsObject.empty,
              lastUpdated = Instant.ofEpochMilli(10)
            )
          )
        )

        mockGetJourneyAnswers(
          mtdItId = mtdItId,
          taxYear = taxYear,
          journey = "jobSeekersAllowance",
          result = Some(
            JourneyAnswers(
              mtdItId = mtdItId,
              taxYear = taxYear,
              journey = "jobSeekersAllowance",
              data = JsObject.empty,
              lastUpdated = Instant.ofEpochMilli(10)
            )
          )
        )

        val underTest: Future[Seq[TaskListSection]] = service.get(
          taxYear = taxYear,
          nino = nino,
          mtdItId = mtdItId
        )

        await(underTest) shouldBe emptyTaskSections
      }
    }

    "ESA and JSA have no Journey Answers defined" should {
      "return an empty task list when no ESA or JSA benefits data exists in IF" in new Test {
        mockGetAllStateBenefitsData(
          taxYear = taxYear,
          nino = nino,
          result = Right(Some(AllStateBenefitsData(
            stateBenefitsData = Some(StateBenefitsData(
              incapacityBenefits = Some(Set(
                StateBenefit(
                  benefitId = UUID.randomUUID(),
                  startDate = LocalDate.MIN
                )
              ))
            )
            ),
            customerAddedStateBenefitsData = Some(CustomerAddedStateBenefitsData(
              incapacityBenefits = Some(Set(
                CustomerAddedStateBenefit(
                  benefitId = UUID.randomUUID(),
                  startDate = LocalDate.MIN
                )
              ))
            ))
          )))
        )

        mockGetJourneyAnswers(
          mtdItId = mtdItId,
          taxYear = taxYear,
          journey = "employmentSupportAllowance",
          result = None
        )

        mockGetJourneyAnswers(
          mtdItId = mtdItId,
          taxYear = taxYear,
          journey = "jobSeekersAllowance",
          result = None
        )

        val underTest: Future[Seq[TaskListSection]] = service.get(
          taxYear = taxYear,
          nino = nino,
          mtdItId = mtdItId
        )

        await(underTest) shouldBe emptyTaskSections
      }

      "return 'Completed' status when only customer added ESA and JSA data exists in IF" in new Test {
        mockGetAllStateBenefitsData(
          taxYear = taxYear,
          nino = nino,
          result = Right(Some(AllStateBenefitsData(
            stateBenefitsData = None,
            customerAddedStateBenefitsData = customerAddedStateBenefit(taxYear)
          )))
        )

        mockGetJourneyAnswers(
          mtdItId = mtdItId,
          taxYear = taxYear,
          journey = "employmentSupportAllowance",
          result = None
        )

        mockGetJourneyAnswers(
          mtdItId = mtdItId,
          taxYear = taxYear,
          journey = "jobSeekersAllowance",
          result = None
        )

        val underTest: Future[Seq[TaskListSection]] = service.get(
          taxYear = taxYear,
          nino = nino,
          mtdItId = mtdItId
        )

        await(underTest) shouldBe completedTaskSections
      }

      "return 'Completed' status when customer added ESA and JSA data is newer than HMRC held data in IF" in new Test {
        mockGetAllStateBenefitsData(
          taxYear = taxYear,
          nino = nino,
          result = customerLatestResult(taxYear)
        )

        mockGetJourneyAnswers(
          mtdItId = mtdItId,
          taxYear = taxYear,
          journey = "employmentSupportAllowance",
          result = None
        )

        mockGetJourneyAnswers(
          mtdItId = mtdItId,
          taxYear = taxYear,
          journey = "jobSeekersAllowance",
          result = None
        )

        val underTest: Future[Seq[TaskListSection]] = service.get(
          taxYear = taxYear,
          nino = nino,
          mtdItId = mtdItId
        )

        await(underTest) shouldBe completedTaskSections
      }
    }

    "ESA and JSA have Journey Answers defined" should {
      "use Journey Answers when customer added ESA and JSA data is newer than HMRC held data in IF" in new Test {
        mockGetAllStateBenefitsData(
          taxYear = taxYear,
          nino = nino,
          result = customerLatestResult(taxYear)
        )

        mockGetJourneyAnswers(
          mtdItId = mtdItId,
          taxYear = taxYear,
          journey = "employmentSupportAllowance",
          result = Some(
            JourneyAnswers(
              mtdItId = mtdItId,
              taxYear = taxYear,
              journey = "employmentSupportAllowance",
              data = Json.parse(
                """
                  |{
                  | "status": "completed"
                  |}
                """.stripMargin).as[JsObject],
              lastUpdated = Instant.ofEpochMilli(10)
            )
          )
        )

        mockGetJourneyAnswers(
          mtdItId = mtdItId,
          taxYear = taxYear,
          journey = "jobSeekersAllowance",
          result = Some(
            JourneyAnswers(
              mtdItId = mtdItId,
              taxYear = taxYear,
              journey = "jobSeekersAllowance",
              data = Json.parse(
                """
                  |{
                  | "status": "inProgress"
                  |}
                """.stripMargin).as[JsObject],
              lastUpdated = Instant.ofEpochMilli(10)
            )
          )
        )

        val underTest: Future[Seq[TaskListSection]] = service.get(
          taxYear = taxYear,
          nino = nino,
          mtdItId = mtdItId
        )

        await(underTest) shouldBe splitTaskSections
      }

      "return 'Not Started' status when Journey Answers are used, but contain non-valid statues" in new Test {
        mockGetAllStateBenefitsData(
          taxYear = taxYear,
          nino = nino,
          result = Right(Some(AllStateBenefitsData(
            stateBenefitsData = Some(StateBenefitsData(
              employmentSupportAllowances = Some(Set(StateBenefit(
                benefitId = UUID.fromString("a1e8057e-fbbc-47a8-a8b4-78d9f015c937"),
                startDate = LocalDate.parse(s"${taxYear - 1}-09-23"),
                endDate = Some(LocalDate.parse(s"$taxYear-08-23")),
                dateIgnored = None,
                submittedOn = Some(Instant.parse(s"$taxYear-11-13T19:23:00Z")),
                amount = Some(44545.43),
                taxPaid = Some(35343.23)
              )))
            )),
            customerAddedStateBenefitsData = latestCustomerAddedStateBenefit(taxYear)
          )))
        )

        mockGetJourneyAnswers(
          mtdItId = mtdItId,
          taxYear = taxYear,
          journey = "employmentSupportAllowance",
          result = Some(
            JourneyAnswers(
              mtdItId = mtdItId,
              taxYear = taxYear,
              journey = "employmentSupportAllowance",
              data = Json.parse(
                """
                  |{
                  | "status": "somethingNonValid"
                  |}
                """.stripMargin).as[JsObject],
              lastUpdated = Instant.ofEpochMilli(10)
            )
          )
        )

        mockGetJourneyAnswers(
          mtdItId = mtdItId,
          taxYear = taxYear,
          journey = "jobSeekersAllowance",
          result = Some(
            JourneyAnswers(
              mtdItId = mtdItId,
              taxYear = taxYear,
              journey = "jobSeekersAllowance",
              data = Json.parse(
                """
                  |{
                  | "status": "somethingNonValid"
                  |}
                """.stripMargin).as[JsObject],
              lastUpdated = Instant.ofEpochMilli(10)
            )
          )
        )

        val underTest: Future[Seq[TaskListSection]] = service.get(
          taxYear = taxYear,
          nino = nino,
          mtdItId = mtdItId
        )

        await(underTest) shouldBe nonValidStatusTaskSections
      }

      "return 'Check' status when HMRC held ESA and JSA data is newer than customer added data in IF" in new Test {
        mockGetAllStateBenefitsData(
          taxYear = taxYear,
          nino = nino,
          result = fullStateBenefitsResult(taxYear)
        )

        mockGetJourneyAnswers(
          mtdItId = mtdItId,
          taxYear = taxYear,
          journey = "employmentSupportAllowance",
          result = None
        )

        mockGetJourneyAnswers(
          mtdItId = mtdItId,
          taxYear = taxYear,
          journey = "jobSeekersAllowance",
          result = None
        )

        val underTest: Future[Seq[TaskListSection]] = service.get(
          taxYear = taxYear,
          nino = nino,
          mtdItId = mtdItId
        )

        await(underTest) shouldBe checkNowTaskSections
      }
    }
  }
}
