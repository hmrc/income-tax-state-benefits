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

import connectors.errors.{ApiError, SingleErrorBody}
import models.IncomeTaxUserData
import play.api.http.Status.{BAD_REQUEST, NOT_FOUND, OK}
import play.api.libs.json.Json
import play.api.test.Helpers.status
import support.ControllerUnitTest
import support.builders.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import support.builders.UserBuilder.aUser
import support.builders.api.AllStateBenefitsDataBuilder.anAllStateBenefitsData
import support.mocks.{MockAuthorisedAction, MockStateBenefitsService}
import support.providers.FakeRequestProvider

import scala.concurrent.ExecutionContext.Implicits.global

class GetUserPriorDataControllerSpec extends ControllerUnitTest
  with MockStateBenefitsService
  with MockAuthorisedAction
  with FakeRequestProvider {

  private val anyYear = 2022

  private val underTest = new GetUserPriorDataController(
    mockStateBenefitsService,
    mockAuthorisedAction,
    cc
  )

  ".getPriorData" should {
    "return NotFound when stateBenefitsService.getPriorData(...) returns Right(IncomeTaxUserData(None))" in {
      mockAuthorisation()
      mockGetPriorData(anyYear, "some-nino", aUser.mtditid, Right(IncomeTaxUserData(None)))

      val result = underTest.getPriorData(nino = "some-nino", anyYear)(fakeGetRequest)

      status(result) shouldBe NOT_FOUND
    }

    "return allStateBenefitsData when stateBenefitsService.getPriorData(...) returns Right(anIncomeTaxUserData)" in {
      mockAuthorisation()
      mockGetPriorData(anyYear, nino = "some-nino", aUser.mtditid, Right(anIncomeTaxUserData))

      val result = await(underTest.getPriorData(nino = "some-nino", anyYear)(fakeGetRequest))

      result.header.status shouldBe OK
      Json.parse(consumeBody(result)) shouldBe Json.toJson(anAllStateBenefitsData)
    }

    "return error when stateBenefitsService.getPriorData(...) returns Left(errorModel)" in {
      val error = ApiError(status = BAD_REQUEST, body = SingleErrorBody("some-code", "some-reason"))

      mockAuthorisation()
      mockGetPriorData(anyYear, nino = "some-nino", aUser.mtditid, Left(error))

      val result = await(underTest.getPriorData(nino = "some-nino", anyYear)(fakeGetRequest))

      result.header.status shouldBe BAD_REQUEST
      Json.parse(consumeBody(result)) shouldBe error.toJson
    }
  }
}
