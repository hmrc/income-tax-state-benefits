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
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, NOT_FOUND, NO_CONTENT}
import play.api.libs.json.Json
import play.api.test.Helpers.status
import support.ControllerUnitTest
import support.builders.mongo.StateBenefitsUserDataBuilder.aStateBenefitsUserData
import support.mocks.{MockAuthorisedAction, MockStateBenefitsService}
import support.providers.FakeRequestProvider

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global

class ClaimDataControllerSpec extends ControllerUnitTest
  with MockAuthorisedAction
  with MockStateBenefitsService
  with FakeRequestProvider {

  private val sessionDataId = aStateBenefitsUserData.sessionDataId.get
  private val nino = aStateBenefitsUserData.nino

  private val underTest = new ClaimDataController(
    authorisedAction = mockAuthorisedAction,
    stateBenefitsService = mockStateBenefitsService,
    cc = cc
  )

  ".save" should {
    for (saveType <- Seq("save by sessionId", "save by userData")) {

      val userData = if (saveType.contains("save by sessionId")) aStateBenefitsUserData else aStateBenefitsUserData.copy(sessionDataId = None)

      s"$saveType" should {

        "return BadRequest" when {
          s"when data received is in invalid format" in {
            mockAuthorisation()

            val request = fakePutRequest.withJsonBody(Json.parse("""{"wrongFormat": "wrong-value"}"""))
            val result =
              if (saveType.contains("save by sessionId")) underTest.save(nino, sessionDataId)(request) else underTest.saveByData(nino)(request)

            status(result) shouldBe BAD_REQUEST
          }

          s"when nino is different" in {
            mockAuthorisation()

            val request = fakePostRequest.withJsonBody(Json.toJson(userData))
            val result =
              if (saveType.contains("save by sessionId")) underTest.save("some-nino", sessionDataId)(request) else underTest.saveByData("some-nino")(request)

            status(result) shouldBe BAD_REQUEST
          }

          if (saveType.contains("save by sessionId")) {
            "when sessionDataId is different" in {
              mockAuthorisation()

              val result = underTest.save(nino, UUID.randomUUID())(fakePostRequest.withJsonBody(Json.toJson(userData)))

              status(result) shouldBe BAD_REQUEST
            }
          }
        }

        s"should return INTERNAL_SERVER_ERROR when saveUserData returns an error" in {
          mockAuthorisation()
          mockSaveUserData(userData, Left(DataNotUpdatedError))

          val request = fakePutRequest.withJsonBody(Json.toJson(userData))
          val result =
            if (saveType.contains("save by sessionId")) underTest.save(nino, sessionDataId)(request) else underTest.saveByData(nino)(request)

          status(result) shouldBe INTERNAL_SERVER_ERROR
        }

        s"should return NoContent when stateBenefitsService returns Right(None)" in {
          mockAuthorisation()
          mockSaveUserData(userData, Right(()))

          val request = fakePutRequest.withJsonBody(Json.toJson(userData))
          val result =
            if (saveType.contains("save by sessionId")) underTest.save(nino, sessionDataId)(request) else underTest.saveByData(nino)(request)

          status(result) shouldBe NO_CONTENT
        }
      }
    }
  }

  ".remove" should {
    "return NotFound when stateBenefitsService.removeClaim(...) returns DataNotFoundError" in {
      mockAuthorisation()
      mockRemoveClaim(nino, sessionDataId, Left(DataNotFoundError))

      val result = underTest.remove(nino, sessionDataId)(fakeDeleteRequest)

      status(result) shouldBe NOT_FOUND
    }

    "return InternalServerError when stateBenefitsService.removeClaim(...) returns any error different than DataNotFoundError" in {
      mockAuthorisation()
      mockRemoveClaim(nino, sessionDataId, Left(DataNotUpdatedError))

      val result = underTest.remove(nino, sessionDataId)(fakeDeleteRequest)

      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

    "return NoContent when stateBenefitsService.removeClaim(...) returns success" in {
      mockAuthorisation()
      mockRemoveClaim(nino, sessionDataId, Right(()))

      val result = underTest.remove(nino, sessionDataId)(fakeDeleteRequest)

      status(result) shouldBe NO_CONTENT
    }
  }

  ".restore" should {
    val userData = aStateBenefitsUserData

    "return INTERNAL_SERVER_ERROR when restoreClaim returns an error" in {
      mockAuthorisation()
      mockRestoreClaim(userData.nino, sessionDataId, Left(DataNotUpdatedError))

      val result = underTest.restore(userData.nino, sessionDataId)(fakeDeleteRequest)

      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

    "return NoContent when restoreClaim returns Right(None)" in {
      mockAuthorisation()
      mockRestoreClaim(userData.nino, sessionDataId, Right(()))

      val result = underTest.restore(userData.nino, sessionDataId)(fakeDeleteRequest)

      status(result) shouldBe NO_CONTENT
    }
  }
}
