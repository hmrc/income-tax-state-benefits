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

import connectors.Parser
import connectors.errors.ApiError
import play.api.http.Status._
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

case class IgnoreStateBenefitResponse(httpResponse: HttpResponse, result: Either[ApiError, Unit])

object IgnoreStateBenefitResponse {

  implicit val ignoreStateBenefitResponseReads: HttpReads[IgnoreStateBenefitResponse] = new HttpReads[IgnoreStateBenefitResponse] with Parser {
    override protected[connectors] val parserName: String = this.getClass.getSimpleName

    override def read(method: String, url: String, response: HttpResponse): IgnoreStateBenefitResponse = response.status match {
      case CREATED => IgnoreStateBenefitResponse(response, Right(()))
      case BAD_REQUEST | FORBIDDEN | NOT_FOUND | UNPROCESSABLE_ENTITY | INTERNAL_SERVER_ERROR | SERVICE_UNAVAILABLE =>
        IgnoreStateBenefitResponse(response, handleError(response, response.status))
      case _ => IgnoreStateBenefitResponse(response, handleError(response, INTERNAL_SERVER_ERROR))
    }
  }
}
