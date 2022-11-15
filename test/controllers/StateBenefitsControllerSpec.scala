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
import play.api.http.Status.{BAD_REQUEST, FORBIDDEN, NO_CONTENT, OK}
import play.api.libs.json.Json
import play.api.test.Helpers.status
import support.ControllerUnitTest
import support.builders.api.AllStateBenefitsDataBuilder.anAllStateBenefitsData
import support.builders.api.StateBenefitDetailOverrideBuilder.aStateBenefitDetailOverride
import support.mocks.{MockAuthorisedAction, MockStateBenefitsService}
import support.providers.FakeRequestProvider

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global

class StateBenefitsControllerSpec extends ControllerUnitTest
  with MockStateBenefitsService
  with MockAuthorisedAction
  with FakeRequestProvider {

  private val anyYear = 2022
  private val benefitId = UUID.randomUUID()

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
      val error = ApiError(status = BAD_REQUEST, body = SingleErrorBody("some-code", "some-reason"))

      mockAuthorisation()
      mockGetAllStateBenefitsData(anyYear, "some-nino", Left(error))

      val result = await(underTest.getAllStateBenefitsData("some-nino", anyYear)(fakeGetRequest))

      result.header.status shouldBe BAD_REQUEST
      Json.parse(consumeBody(result)) shouldBe error.toJson
    }
  }

  ".createOrUpdateStateBenefitDetailOverride" should {
    "return NoContent when stateBenefitsService returns Right(_)" in {
      mockAuthorisation()
      mockCreateOrUpdateStateBenefitDetailOverride(anyYear, "some-nino", benefitId, aStateBenefitDetailOverride, Right(()))

      val request = fakePutRequest.withJsonBody(Json.toJson(aStateBenefitDetailOverride))
      val result = underTest.createOrUpdateStateBenefitDetailOverride("some-nino", anyYear, benefitId)(request)

      status(result) shouldBe NO_CONTENT
    }

    "return error when stateBenefitsService returns Left(errorModel)" in {
      val error = ApiError(status = FORBIDDEN, body = SingleErrorBody("some-code", "some-reason"))
      val request = fakePutRequest.withJsonBody(Json.toJson(aStateBenefitDetailOverride))

      mockAuthorisation()
      mockCreateOrUpdateStateBenefitDetailOverride(anyYear, "some-nino", benefitId, aStateBenefitDetailOverride, Left(error))

      val result = await(underTest.createOrUpdateStateBenefitDetailOverride("some-nino", anyYear, benefitId)(request))

      result.header.status shouldBe FORBIDDEN
      Json.parse(consumeBody(result)) shouldBe error.toJson
    }

    "return BadRequest when JSON in request is in wrong format" in {
      val request = fakePutRequest.withJsonBody(Json.parse("""{"wrong-key": "wrong-value"}"""))

      mockAuthorisation()

      val result = await(underTest.createOrUpdateStateBenefitDetailOverride("some-nino", anyYear, benefitId)(request))

      result.header.status shouldBe BAD_REQUEST
    }
  }

  ".deleteStateBenefit" should {
    "return NoContent when stateBenefitsService returns Right(_)" in {
      mockAuthorisation()
      mockDeleteStateBenefit(anyYear, "some-nino", benefitId, Right(()))

      val result = underTest.deleteStateBenefit("some-nino", anyYear, benefitId)(fakeDeleteRequest)

      status(result) shouldBe NO_CONTENT
    }

    "return error when stateBenefitsService returns Left(errorModel)" in {
      val error = ApiError(status = FORBIDDEN, body = SingleErrorBody("some-code", "some-reason"))

      mockAuthorisation()
      mockDeleteStateBenefit(anyYear, "some-nino", benefitId, Left(error))

      val result = await(underTest.deleteStateBenefit("some-nino", anyYear, benefitId)(fakeDeleteRequest))

      result.header.status shouldBe FORBIDDEN
      Json.parse(consumeBody(result)) shouldBe error.toJson
    }
  }
}
