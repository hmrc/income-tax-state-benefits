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

import play.api.http.Status.{BAD_REQUEST, NOT_FOUND, OK}
import play.api.libs.json.Json
import play.api.test.Helpers.status
import support.ControllerUnitTest
import support.builders.mongo.StateBenefitsUserDataBuilder.aStateBenefitsUserData
import support.mocks.{MockAuthorisedAction, MockStateBenefitsService}
import support.providers.FakeRequestProvider

import java.util.UUID

class UserSessionDataControllerSpec extends ControllerUnitTest
  with MockAuthorisedAction
  with MockStateBenefitsService
  with FakeRequestProvider {

  private val sessionDataId = UUID.randomUUID()

  private val underTest = new UserSessionDataController(
    authorisedAction = mockAuthorisedAction,
    stateBenefitsService = mockStateBenefitsService,
    cc = cc
  )

  ".getStateBenefitsUserData" should {
    "return NotFound when data with given UUID does not exist" in {
      mockAuthorisation()
      mockGetStateBenefitsUserData(sessionDataId, None)

      val result = underTest.getStateBenefitsUserData(sessionDataId)(fakeGetRequest)

      status(result) shouldBe NOT_FOUND
    }

    "return StateBenefitsUserData when data with given UUID exists" in {
      mockAuthorisation()
      mockGetStateBenefitsUserData(sessionDataId, Some(aStateBenefitsUserData))

      val result = await(underTest.getStateBenefitsUserData(sessionDataId)(fakeGetRequest))

      result.header.status shouldBe OK
      Json.parse(consumeBody(result)) shouldBe Json.toJson(aStateBenefitsUserData)
    }
  }

  ".createOrUpdate" should {
    "return BadRequest when data received is in invalid format" in {
      mockAuthorisation()

      val result = underTest.createOrUpdate()(fakeGetRequest)

      status(result) shouldBe BAD_REQUEST
    }

    "return UUID when data received is in valid format" in {
      mockAuthorisation()
      mockCreateOrUpdateStateBenefitsUserData(aStateBenefitsUserData, sessionDataId)

      val result = await(underTest.createOrUpdate()(fakePostRequest.withJsonBody(Json.toJson(aStateBenefitsUserData))))

      result.header.status shouldBe OK
      Json.parse(consumeBody(result)) shouldBe Json.toJson(sessionDataId)
    }
  }
}
