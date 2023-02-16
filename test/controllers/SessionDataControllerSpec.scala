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

import models.errors.{DataNotFoundError, DataNotUpdatedError}
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.test.Helpers.status
import support.ControllerUnitTest
import support.builders.mongo.StateBenefitsUserDataBuilder.aStateBenefitsUserData
import support.mocks.{MockAuthorisedAction, MockStateBenefitsService}
import support.providers.FakeRequestProvider

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global

class SessionDataControllerSpec extends ControllerUnitTest
  with MockAuthorisedAction
  with MockStateBenefitsService
  with FakeRequestProvider {

  private val sessionDataId = aStateBenefitsUserData.sessionDataId.get
  private val nino = aStateBenefitsUserData.nino

  private val underTest = new SessionDataController(
    authorisedAction = mockAuthorisedAction,
    stateBenefitsService = mockStateBenefitsService,
    cc = cc
  )

  ".getStateBenefitsUserData" should {
    "return InternalServerError when stateBenefitsService returns DatabaseError different than DataNotFoundError" in {
      mockAuthorisation()
      mockGetStateBenefitsUserData(nino, sessionDataId, Left(DataNotUpdatedError))

      val result = underTest.getSessionData(nino, sessionDataId)(fakeGetRequest)

      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

    "return NotFound when data with given stateBenefitsService returns DataNotFoundError" in {
      mockAuthorisation()
      mockGetStateBenefitsUserData(nino, sessionDataId, Left(DataNotFoundError))

      val result = underTest.getSessionData(nino, sessionDataId)(fakeGetRequest)

      status(result) shouldBe NOT_FOUND
    }

    "return StateBenefitsUserData when stateBenefitsService returns data" in {
      mockAuthorisation()
      mockGetStateBenefitsUserData(nino, sessionDataId, Right(aStateBenefitsUserData))

      val result = await(underTest.getSessionData(nino, sessionDataId)(fakeGetRequest))

      result.header.status shouldBe OK
      Json.parse(consumeBody(result)) shouldBe Json.toJson(aStateBenefitsUserData)
    }
  }

  ".create" should {
    "return BadRequest when data received is in invalid format" in {
      mockAuthorisation()

      val result = underTest.create()(fakePutRequest.withJsonBody(Json.parse("""{"wrongFormat": "wrong-value"}""")))

      status(result) shouldBe BAD_REQUEST
    }

    "return UUID when data received is in valid format and successful response" in {
      mockAuthorisation()
      mockCreateSessionData(aStateBenefitsUserData, Right(sessionDataId))

      val result = await(underTest.create()(fakePostRequest.withJsonBody(Json.toJson(aStateBenefitsUserData))))

      result.header.status shouldBe CREATED
      Json.parse(consumeBody(result)) shouldBe Json.toJson(sessionDataId)
    }

    "return INTERNAL_SERVER_ERROR when createSessionData returns an error" in {
      mockAuthorisation()
      mockCreateSessionData(aStateBenefitsUserData, Left(DataNotUpdatedError))

      val result = underTest.create()(fakePostRequest.withJsonBody(Json.toJson(aStateBenefitsUserData)))

      status(result) shouldBe INTERNAL_SERVER_ERROR
    }
  }

  ".update" should {
    "return BadRequest when data received is in invalid format" in {
      mockAuthorisation()

      val result = underTest.update(nino, sessionDataId)(fakePutRequest.withJsonBody(Json.parse("""{"wrongFormat": "wrong-value"}""")))

      status(result) shouldBe BAD_REQUEST
    }

    "return NO_CONTENT when data received is in valid format and successful response" in {
      mockAuthorisation()
      mockUpdateSessionData(aStateBenefitsUserData, Right(sessionDataId))

      val result = await(underTest.update(nino, sessionDataId)(fakePostRequest.withJsonBody(Json.toJson(aStateBenefitsUserData))))

      result.header.status shouldBe NO_CONTENT
    }

    "return BAD_REQUEST when createSessionData returns an error" when {
      "when nino is different" in {
        mockAuthorisation()

        val result = underTest.update("some-nino", sessionDataId)(fakePostRequest.withJsonBody(Json.toJson(aStateBenefitsUserData)))

        status(result) shouldBe BAD_REQUEST
      }

      "when sessionDataId is different" in {
        mockAuthorisation()

        val result = underTest.update(nino, UUID.randomUUID())(fakePostRequest.withJsonBody(Json.toJson(aStateBenefitsUserData)))

        status(result) shouldBe BAD_REQUEST
      }
    }

    "return INTERNAL_SERVER_ERROR when updateSessionData returns an error" in {
      mockAuthorisation()
      mockUpdateSessionData(aStateBenefitsUserData, Left(DataNotUpdatedError))

      val result = underTest.update(nino, sessionDataId)(fakePostRequest.withJsonBody(Json.toJson(aStateBenefitsUserData)))

      status(result) shouldBe INTERNAL_SERVER_ERROR
    }
  }

  ".removeClaim" should {
    "return NotFound when stateBenefitsService.removeClaim(...) returns DataNotFoundError" in {
      mockAuthorisation()
      mockRemoveClaim(nino, sessionDataId, Left(DataNotFoundError))

      val result = underTest.removeClaim(nino, sessionDataId)(fakeDeleteRequest)

      status(result) shouldBe NOT_FOUND
    }

    "return InternalServerError when stateBenefitsService.removeClaim(...) returns any error different than DataNotFoundError" in {
      mockAuthorisation()
      mockRemoveClaim(nino, sessionDataId, Left(DataNotUpdatedError))

      val result = underTest.removeClaim(nino, sessionDataId)(fakeDeleteRequest)

      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

    "return NoContent when stateBenefitsService.removeClaim(...) returns success" in {
      mockAuthorisation()
      mockRemoveClaim(nino, sessionDataId, Right(()))

      val result = underTest.removeClaim(nino, sessionDataId)(fakeDeleteRequest)

      status(result) shouldBe NO_CONTENT
    }
  }

  ".restoreClaim" should {
    val userData = aStateBenefitsUserData

    "return INTERNAL_SERVER_ERROR when restoreClaim returns an error" in {
      mockAuthorisation()
      mockRestoreClaim(userData.nino, sessionDataId, Left(DataNotUpdatedError))

      val result = underTest.restoreClaim(userData.nino, sessionDataId)(fakeDeleteRequest)

      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

    "return NoContent when restoreClaim returns Right(None)" in {
      mockAuthorisation()
      mockRestoreClaim(userData.nino, sessionDataId, Right(()))

      val result = underTest.restoreClaim(userData.nino, sessionDataId)(fakeDeleteRequest)

      status(result) shouldBe NO_CONTENT
    }
  }
}
