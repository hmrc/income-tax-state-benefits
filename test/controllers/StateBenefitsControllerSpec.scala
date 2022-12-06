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

import models.errors.{DataNotFoundError, DataNotUpdatedError}
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, NO_CONTENT, OK}
import play.api.libs.json.Json
import play.api.test.Helpers.status
import support.ControllerUnitTest
import support.builders.api.AllStateBenefitsDataBuilder.anAllStateBenefitsData
import support.builders.mongo.StateBenefitsUserDataBuilder.aStateBenefitsUserData
import support.mocks.{MockAuthorisedAction, MockStateBenefitsService}
import support.providers.FakeRequestProvider

import scala.concurrent.ExecutionContext.Implicits.global

class StateBenefitsControllerSpec extends ControllerUnitTest
  with MockStateBenefitsService
  with MockAuthorisedAction
  with FakeRequestProvider {

  private val anyYear = 2022

  private val underTest = new StateBenefitsController(
    mockStateBenefitsService,
    mockAuthorisedAction,
    cc
  )

  ".getAllStateBenefitsData" should {
    "return NoContent when stateBenefitsService returns Right(None)" in {
      mockAuthorisation()
      mockGetAllStateBenefitsData(anyYear, "some-nino", Right(None))

      val result = underTest.getAllStateBenefitsData("some-nino", anyYear)(fakeGetRequest)

      status(result) shouldBe NO_CONTENT
    }

    "return allStateBenefitsData when stateBenefitsService returns Right(allStateBenefitsData)" in {
      mockAuthorisation()
      mockGetAllStateBenefitsData(anyYear, "some-nino", Right(Some(anAllStateBenefitsData)))

      val result = await(underTest.getAllStateBenefitsData("some-nino", anyYear)(fakeGetRequest))

      result.header.status shouldBe OK
      Json.parse(consumeBody(result)) shouldBe Json.toJson(anAllStateBenefitsData)
    }

    "return error when stateBenefitsService returns Left(errorModel)" in {
      mockAuthorisation()
      mockGetAllStateBenefitsData(anyYear, "some-nino", Left(DataNotFoundError))

      val result = underTest.getAllStateBenefitsData("some-nino", anyYear)(fakeGetRequest)

      status(result) shouldBe INTERNAL_SERVER_ERROR
    }
  }

  ".saveUserData" should {
    "return BadRequest when data received is in invalid format" in {
      mockAuthorisation()

      val result = underTest.saveUserData()(fakePutRequest.withJsonBody(Json.parse("""{"wrongFormat": "wrong-value"}""")))

      status(result) shouldBe BAD_REQUEST
    }

    "return INTERNAL_SERVER_ERROR when saveUserData returns an error" in {
      mockAuthorisation()
      mockSaveUserData(aStateBenefitsUserData, Left(DataNotUpdatedError))

      val result = underTest.saveUserData()(fakePutRequest.withJsonBody(Json.toJson(aStateBenefitsUserData)))

      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

    "return NoContent when stateBenefitsService returns Right(None)" in {
      mockAuthorisation()
      mockSaveUserData(aStateBenefitsUserData, Right(()))

      val result = underTest.saveUserData()(fakePutRequest.withJsonBody(Json.toJson(aStateBenefitsUserData)))

      status(result) shouldBe NO_CONTENT
    }
  }
}
