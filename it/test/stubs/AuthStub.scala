/*
 * Copyright 2025 HM Revenue & Customs
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

package stubs

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status.OK
import play.api.libs.json.{JsObject, Json}
import support.WireMockMethods

object AuthStub extends WireMockMethods {

  def authorised(nino: String, mtdItId: String): StubMapping = {
    when(method = POST, uri = authoriseUri)
      .withRequestBody(initialRetrievalsRequestBody)
      .thenReturn(
        status = OK,
        body = Json.obj("affinityGroup" -> "Individual")
      )

    when(method = POST, uri = authoriseUri)
      .withRequestBody(individualsRetrievalRequestBody)
      .thenReturn(
        status = OK,
        body = Json.obj(
          "confidenceLevel" -> 250,
          "allEnrolments" -> individualEnrolments(nino, mtdItId)
        )
      )
  }

  private def individualEnrolments(nino: String, mtdItId: String): Seq[JsObject] = List(
    Json.obj(
      "key" -> "HMRC-MTD-IT",
      "identifiers" -> Json.arr(
        Json.obj(
          "key"   -> "MTDITID",
          "value" -> s"$mtdItId"
        )
      )
    ),
    Json.obj(
      "key" -> "HMRC-NI",
      "identifiers" -> Json.arr(
        Json.obj(
          "key"   -> "NINO",
          "value" -> s"$nino"
        )
      )
    )
  )

  private val authoriseUri: String = "/auth/authorise"

  private val initialRetrievalsRequestBody =
    Json.parse(
      """
        |{
        | "authorise": [],
        | "retrieve": [
        |   "affinityGroup"
        | ]
        |}
      """.stripMargin
    )

  private val individualsRetrievalRequestBody =
    Json.parse(
      """
        |{
        | "authorise": [],
        | "retrieve": [
        |   "allEnrolments",
        |   "confidenceLevel"
        | ]
        |}
      """.stripMargin
    )

}
