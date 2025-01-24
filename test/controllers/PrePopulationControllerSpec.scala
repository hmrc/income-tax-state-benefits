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

package controllers

import models.errors.ApiServiceError
import models.prePopulation.PrePopulationResponse
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.Helpers.{contentAsJson, status}
import support.ControllerUnitTest
import support.mocks.{MockAuthorisedAction, MockPrePopulationService}
import support.providers.FakeRequestProvider
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class PrePopulationControllerSpec extends ControllerUnitTest
  with MockPrePopulationService
  with MockAuthorisedAction
  with FakeRequestProvider {

  trait Test {
    val taxYear: Int = 2024
    val nino: String = "AA111111A"

    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global

    val controller = new PrePopulationController(
      service = mockPrePopService,
      auth = mockAuthorisedAction,
      cc = cc
    )

    mockAuthorisation()
  }

  "get" when {
    "prePopulationService returns an exception" should {
      "handle gracefully" in new Test {
        mockGetPrePopException(taxYear, nino, new RuntimeException("An Error"))

        val result: Future[Result] = controller.get(nino, taxYear)(fakeGetRequest)
        status(result) shouldBe 500
      }
    }

    "prePopulationService returns an error" should {
      "return an error" in new Test {
        mockGetPrePop(taxYear, nino, Left(ApiServiceError("599")))

        val result: Future[Result] = controller.get(nino, taxYear)(fakeGetRequest)
        status(result) shouldBe 500
      }
    }

    "prePopulationService returns a pre pop response" should {
      "return it" in new Test {
        mockGetPrePop(taxYear, nino, Right(PrePopulationResponse(hasEsaPrePop = false, hasJsaPrePop = true)))

        val result: Future[Result] = controller.get(nino, taxYear)(fakeGetRequest)
        status(result) shouldBe 200
        contentAsJson(result) shouldBe
          Json.parse(
            """
              |{
              |   "hasEsaPrePop": false,
              |   "hasJsaPrePop": true
              |}
          """.stripMargin
          )
      }
    }
  }

}
