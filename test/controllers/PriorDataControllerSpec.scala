/*
 * Copyright 2023 HM Revenue & Customs
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

import models.IncomeTaxUserData
import models.errors.ApiServiceError
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NOT_FOUND, OK}
import play.api.libs.json.Json
import play.api.test.Helpers.status
import support.ControllerUnitTest
import support.builders.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import support.builders.UserBuilder.aUser
import support.builders.api.AllStateBenefitsDataBuilder.anAllStateBenefitsData
import support.mocks.{MockAuthorisedAction, MockStateBenefitsService}
import support.providers.FakeRequestProvider

import scala.concurrent.ExecutionContext.Implicits.global

class PriorDataControllerSpec extends ControllerUnitTest
  with MockStateBenefitsService
  with MockAuthorisedAction
  with FakeRequestProvider {

  private val anyYear = 2022

  private val underTest = new PriorDataController(
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
      mockAuthorisation()
      mockGetPriorData(anyYear, nino = "some-nino", aUser.mtditid, Left(ApiServiceError("some-error")))

      val result = await(underTest.getPriorData(nino = "some-nino", anyYear)(fakeGetRequest))

      result.header.status shouldBe INTERNAL_SERVER_ERROR
    }
  }
}
