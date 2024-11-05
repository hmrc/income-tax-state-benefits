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
import models.tasklist._
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
    "call to IF for benefits data fails" should {
      "return an empty task list when an API error occurs" in new Test {
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

    "an error occurs while retrieving ESA journey answers from Mongo" should {
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

    "an error occurs while retrieving JSA answers from Mongo" should {
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

    "benefits response from IF is empty" should {
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

    "there is no data in IF, and no Journey Answers defined for ESA & JSA" should {
      "return empty task list" in new Test {
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
    }

    "only customer supplied data exists for ESA & JSA" should {
      "return status 'Completed' for both tasks" in new Test {
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
    }

    "HMRC held data is older, or omitted and Journey Answers are defined for ESA & JSA" should {
      "return status as defined in Journey Answers for each task" in new Test {
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

      "handle appropriately if Journey Answers status cannot be parsed" in new Test {
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
    }

    "HMRC held data is newer, or is defined while customer submitted data is not for ESA & JSA" should {
      "return 'Check' status for each task" in new Test {
        mockGetAllStateBenefitsData(
          taxYear = taxYear,
          nino = nino,
          result = Right(Some(
            AllStateBenefitsData(
              stateBenefitsData = stateBenefits(taxYear),
              customerAddedStateBenefitsData = Some(
                CustomerAddedStateBenefitsData(
                  employmentSupportAllowances = Some(Set(
                    CustomerAddedStateBenefit(
                      benefitId = UUID.fromString("a1e8057e-fbbc-47a8-a8b4-78d9f015c988"),
                      startDate = LocalDate.parse(s"${taxYear - 1}-09-11"),
                      endDate = Some(LocalDate.parse(s"$taxYear-06-13")),
                      submittedOn = Some(Instant.parse(s"$taxYear-02-10T11:20:00Z")),
                      amount = Some(45424.23),
                      taxPaid = Some(23232.34)
                    )
                  ))
                )
              )
            )
          ))
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

        println(Instant.parse(s"$taxYear-07-10T18:23:00Z").getEpochSecond)

        await(underTest) shouldBe checkNowTaskSections
      }
    }
  }
}
