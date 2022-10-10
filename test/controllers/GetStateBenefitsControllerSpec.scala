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

package controllers

import akka.actor.ActorSystem
import connectors.errors.{ApiError, SingleErrorBody}
import play.api.http.Status.{BAD_REQUEST, NO_CONTENT, OK}
import play.api.libs.json.Json
import play.api.test.Helpers.{status, stubMessagesControllerComponents}
import support.UnitTest
import support.builders.api.AllStateBenefitsDataBuilder.anAllStateBenefitsData
import support.mocks.{MockAuthorisedAction, MockStateBenefitsService}
import support.providers.{FakeRequestProvider, ResultBodyConsumerProvider}

import scala.concurrent.ExecutionContext.Implicits.global

class GetStateBenefitsControllerSpec extends UnitTest
  with MockStateBenefitsService
  with MockAuthorisedAction
  with FakeRequestProvider
  with ResultBodyConsumerProvider {

  implicit val actorSystem: ActorSystem = ActorSystem()

  private val anyYear = 2022

  private val underTest = new GetStateBenefitsController(
    mockStateBenefitsService,
    mockAuthorisedAction,
    stubMessagesControllerComponents()
  )

  ".getAllStateBenefitsData" should {
    "return NoContent when stateBenefitsService returns Right(None)" in {
      mockAsync()
      mockGetAllStateBenefitsData(anyYear, "some-nino", Right(None))

      val result = underTest.getAllStateBenefitsData("some-nino", anyYear)(fakeGetRequest)

      status(result) shouldBe NO_CONTENT
    }

    "return allStateBenefitsData when stateBenefitsService returns Right(allStateBenefitsData)" in {
      mockAsync()
      mockGetAllStateBenefitsData(anyYear, "some-nino", Right(Some(anAllStateBenefitsData)))

      val result = await(underTest.getAllStateBenefitsData("some-nino", anyYear)(fakeGetRequest))

      result.header.status shouldBe OK
      Json.parse(consumeBody(result)) shouldBe Json.toJson(anAllStateBenefitsData)
    }

    "return error when stateBenefitsService returns Left(errorModel)" in {
      val error = ApiError(status = BAD_REQUEST, body = SingleErrorBody("some-code", "some-reason"))

      mockAsync()
      mockGetAllStateBenefitsData(anyYear, "some-nino", Left(error))

      val result = await(underTest.getAllStateBenefitsData("some-nino", anyYear)(fakeGetRequest))

      result.header.status shouldBe BAD_REQUEST
      Json.parse(consumeBody(result)) shouldBe error.toJson
    }
  }
}
