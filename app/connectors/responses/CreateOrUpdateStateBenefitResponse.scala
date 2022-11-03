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

import connectors.Parser
import connectors.errors.ApiError
import play.api.http.Status._
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

case class CreateOrUpdateStateBenefitResponse(httpResponse: HttpResponse, result: Either[ApiError, Unit])

object CreateOrUpdateStateBenefitResponse {

  implicit val createOrUpdateStateBenefitResponseReads: HttpReads[CreateOrUpdateStateBenefitResponse] =
    new HttpReads[CreateOrUpdateStateBenefitResponse] with Parser {
      override protected[connectors] val parserName: String = this.getClass.getSimpleName

      override def read(method: String, url: String, response: HttpResponse): CreateOrUpdateStateBenefitResponse = response.status match {
        case NO_CONTENT => CreateOrUpdateStateBenefitResponse(response, Right(()))
        case INTERNAL_SERVER_ERROR | SERVICE_UNAVAILABLE | BAD_REQUEST | NOT_FOUND | UNPROCESSABLE_ENTITY =>
          CreateOrUpdateStateBenefitResponse(response, handleError(response, response.status))
        case _ => CreateOrUpdateStateBenefitResponse(response, handleError(response, INTERNAL_SERVER_ERROR))
      }
    }
}
