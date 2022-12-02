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

package connectors.responses

import connectors.errors.{ApiError, SingleErrorBody}
import connectors.responses.AddStateBenefitResponse.addStateBenefitResponseReads
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json}
import support.UnitTest
import uk.gov.hmrc.http.HttpResponse

import java.util.UUID

class AddStateBenefitResponseSpec extends UnitTest {

  private val anyHeaders: Map[String, Seq[String]] = Map.empty
  private val anyMethod: String = "POST"
  private val anyUrl = "/any-url"
  private val benefitId = UUID.randomUUID()
  private val singleErrorBodyJson: JsValue = Json.toJson(SingleErrorBody("some-code", "some-reason"))

  private val underTest = addStateBenefitResponseReads

  "addStateBenefitResponseReads" should {
    "convert JsValue to AddStateBenefitResponse" when {
      "status is OK and any jsValue" in {
        val jsValue: JsValue = Json.parse(
          s"""
             |{"benefitId": "$benefitId"}
             |""".stripMargin)

        val httpResponse: HttpResponse = HttpResponse.apply(OK, jsValue, anyHeaders)

        underTest.read(anyMethod, anyUrl, httpResponse) shouldBe AddStateBenefitResponse(httpResponse, Right(benefitId))
      }

      "status is BAD_REQUEST and jsValue" in {
        val httpResponse = HttpResponse.apply(BAD_REQUEST, singleErrorBodyJson, anyHeaders)

        underTest.read(anyMethod, anyUrl, httpResponse) shouldBe AddStateBenefitResponse(
          httpResponse,
          Left(ApiError(BAD_REQUEST, SingleErrorBody("some-code", "some-reason")))
        )
      }

      "status is CONFLICT and jsValue" in {
        val httpResponse = HttpResponse.apply(CONFLICT, singleErrorBodyJson, anyHeaders)

        underTest.read(anyMethod, anyUrl, httpResponse) shouldBe AddStateBenefitResponse(
          httpResponse,
          Left(ApiError(CONFLICT, SingleErrorBody("some-code", "some-reason")))
        )
      }

      "status is UNPROCESSABLE_ENTITY and jsValue" in {
        val httpResponse = HttpResponse.apply(UNPROCESSABLE_ENTITY, singleErrorBodyJson, anyHeaders)

        underTest.read(anyMethod, anyUrl, httpResponse) shouldBe AddStateBenefitResponse(
          httpResponse,
          Left(ApiError(UNPROCESSABLE_ENTITY, SingleErrorBody("some-code", "some-reason")))
        )
      }

      "status is INTERNAL_SERVER_ERROR and jsValue" in {
        val httpResponse = HttpResponse.apply(INTERNAL_SERVER_ERROR, singleErrorBodyJson, anyHeaders)

        underTest.read(anyMethod, anyUrl, httpResponse) shouldBe AddStateBenefitResponse(
          httpResponse,
          Left(ApiError(INTERNAL_SERVER_ERROR, SingleErrorBody("some-code", "some-reason")))
        )
      }

      "status is SERVICE_UNAVAILABLE and jsValue" in {
        val httpResponse = HttpResponse.apply(SERVICE_UNAVAILABLE, singleErrorBodyJson, anyHeaders)

        underTest.read(anyMethod, anyUrl, httpResponse) shouldBe AddStateBenefitResponse(
          httpResponse,
          Left(ApiError(SERVICE_UNAVAILABLE, SingleErrorBody("some-code", "some-reason")))
        )
      }

      "status is INTERNAL_SERVER_ERROR for any other error and jsValue for error" in {
        val httpResponse: HttpResponse = HttpResponse.apply(FAILED_DEPENDENCY, singleErrorBodyJson, anyHeaders)

        underTest.read(anyMethod, anyUrl, httpResponse) shouldBe AddStateBenefitResponse(
          httpResponse,
          Left(ApiError(INTERNAL_SERVER_ERROR, SingleErrorBody("some-code", "some-reason")))
        )
      }
    }
  }
}
