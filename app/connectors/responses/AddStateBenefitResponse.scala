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
import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

import java.util.UUID

case class AddStateBenefitResponse(httpResponse: HttpResponse, result: Either[ApiError, UUID])

object AddStateBenefitResponse {
  private case class BenefitId(benefitId: UUID)

  implicit val addStateBenefitResponseReads: HttpReads[AddStateBenefitResponse] = new HttpReads[AddStateBenefitResponse] with Parser {
    override protected[connectors] val parserName: String = this.getClass.getSimpleName

    override def read(method: String, url: String, response: HttpResponse): AddStateBenefitResponse = response.status match {
      case OK => AddStateBenefitResponse(response, extractResult(response))
      case BAD_REQUEST | CONFLICT | UNPROCESSABLE_ENTITY | INTERNAL_SERVER_ERROR | SERVICE_UNAVAILABLE =>
        AddStateBenefitResponse(response, handleError(response, response.status))
      case _ => AddStateBenefitResponse(response, handleError(response, INTERNAL_SERVER_ERROR))
    }

    private def extractResult(response: HttpResponse): Either[ApiError, UUID] = {
      implicit val format: OFormat[BenefitId] = Json.format[BenefitId]
      response.json.validate[BenefitId]
        .fold[Either[ApiError, UUID]](_ => badSuccessJsonResponse, parsedModel => Right(parsedModel.benefitId))
    }
  }
}

