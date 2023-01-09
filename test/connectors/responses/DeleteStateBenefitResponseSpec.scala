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

package connectors.responses

import connectors.errors.{ApiError, SingleErrorBody}
import connectors.responses.DeleteStateBenefitResponse.deleteStateBenefitResponseReads
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json}
import support.UnitTest
import uk.gov.hmrc.http.HttpResponse

class DeleteStateBenefitResponseSpec extends UnitTest {

  private val anyHeaders: Map[String, Seq[String]] = Map.empty
  private val anyMethod: String = "DELETE"
  private val anyUrl = "/any-url"
  private val singleErrorBodyJson: JsValue = Json.toJson(SingleErrorBody("some-code", "some-reason"))

  private val underTest = deleteStateBenefitResponseReads

  "deleteStateBenefitResponseReads" should {
    "convert JsValue to DeleteStateBenefitResponse" when {
      "status is NO_CONTENT and any jsValue" in {
        val httpResponse = HttpResponse.apply(NO_CONTENT, "", anyHeaders)

        underTest.read(anyMethod, anyUrl, httpResponse) shouldBe DeleteStateBenefitResponse(
          httpResponse,
          Right(())
        )
      }

      "status is BAD_REQUEST and jsValue" in {
        val httpResponse = HttpResponse.apply(BAD_REQUEST, singleErrorBodyJson, anyHeaders)

        underTest.read(anyMethod, anyUrl, httpResponse) shouldBe DeleteStateBenefitResponse(
          httpResponse,
          Left(ApiError(BAD_REQUEST, SingleErrorBody("some-code", "some-reason")))
        )
      }

      "status is FORBIDDEN and jsValue" in {
        val httpResponse = HttpResponse.apply(FORBIDDEN, singleErrorBodyJson, anyHeaders)

        underTest.read(anyMethod, anyUrl, httpResponse) shouldBe DeleteStateBenefitResponse(
          httpResponse,
          Left(ApiError(FORBIDDEN, SingleErrorBody("some-code", "some-reason")))
        )
      }

      "status is NOT_FOUND and jsValue" in {
        val httpResponse = HttpResponse.apply(NOT_FOUND, singleErrorBodyJson, anyHeaders)

        underTest.read(anyMethod, anyUrl, httpResponse) shouldBe DeleteStateBenefitResponse(
          httpResponse,
          Left(ApiError(NOT_FOUND, SingleErrorBody("some-code", "some-reason")))
        )
      }

      "status is INTERNAL_SERVER_ERROR and jsValue" in {
        val httpResponse = HttpResponse.apply(INTERNAL_SERVER_ERROR, singleErrorBodyJson, anyHeaders)

        underTest.read(anyMethod, anyUrl, httpResponse) shouldBe DeleteStateBenefitResponse(
          httpResponse,
          Left(ApiError(INTERNAL_SERVER_ERROR, SingleErrorBody("some-code", "some-reason")))
        )
      }

      "status is SERVICE_UNAVAILABLE and jsValue" in {
        val httpResponse = HttpResponse.apply(SERVICE_UNAVAILABLE, singleErrorBodyJson, anyHeaders)

        underTest.read(anyMethod, anyUrl, httpResponse) shouldBe DeleteStateBenefitResponse(
          httpResponse,
          Left(ApiError(SERVICE_UNAVAILABLE, SingleErrorBody("some-code", "some-reason")))
        )
      }

      "status is INTERNAL_SERVER_ERROR for any other error and jsValue for error" in {
        val httpResponse = HttpResponse.apply(GATEWAY_TIMEOUT, singleErrorBodyJson, anyHeaders)

        underTest.read(anyMethod, anyUrl, httpResponse) shouldBe DeleteStateBenefitResponse(
          httpResponse,
          Left(ApiError(INTERNAL_SERVER_ERROR, SingleErrorBody("some-code", "some-reason")))
        )
      }
    }
  }
}
