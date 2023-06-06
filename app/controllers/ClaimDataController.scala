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

import actions.AuthorisedAction
import models.errors.DataNotFoundError
import models.mongo.StateBenefitsUserData
import models.requests.AuthorisationRequest
import play.api.Logging
import play.api.libs.json.JsSuccess
import play.api.mvc.{Action, AnyContent, ControllerComponents, Result}
import services.StateBenefitsService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.util.UUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ClaimDataController @Inject()(authorisedAction: AuthorisedAction,
                                    stateBenefitsService: StateBenefitsService,
                                    cc: ControllerComponents)
                                   (implicit ec: ExecutionContext) extends BackendController(cc) with Logging {

  private val invalidRequestLogMessage = "[ClaimDataController][saveUserData] Save state benefits request is invalid"

  def save(nino: String, sessionDataId: UUID): Action[AnyContent] = authorisedAction.async { implicit authRequest =>
    performSave(nino, Some(sessionDataId), authRequest)
  }

  def saveByData(nino: String): Action[AnyContent] = authorisedAction.async { implicit authRequest =>
    performSave(nino, None, authRequest)
  }

  def remove(nino: String, sessionDataId: UUID): Action[AnyContent] = authorisedAction.async { implicit request =>
    stateBenefitsService.removeClaim(nino, sessionDataId).map {
      case Left(DataNotFoundError) => NotFound
      case Left(_) => InternalServerError
      case Right(_) => NoContent
    }
  }

  def restore(nino: String, sessionDataId: UUID): Action[AnyContent] = authorisedAction.async { implicit request =>
    stateBenefitsService.restoreClaim(nino, sessionDataId).map {
      case Left(_) => InternalServerError
      case Right(_) => NoContent
    }
  }

  private def handleSaveUserData(nino: String, sessionDataId: Option[UUID], userData: StateBenefitsUserData)
                                (implicit hc: HeaderCarrier): Future[Result] = {
    if (userData.nino != nino || !userData.sessionDataId.equals(sessionDataId)) {
      logger.warn(invalidRequestLogMessage)
      Future.successful(BadRequest)
    } else {
      stateBenefitsService.saveClaim(userData, sessionDataId.nonEmpty).map {
        case Left(_) => InternalServerError
        case Right(_) => NoContent
      }
    }
  }
  private def performSave(nino: String, sessionDataId: Option[UUID], authRequest: AuthorisationRequest[AnyContent])(implicit hc:HeaderCarrier) = {
    authRequest.request.body.asJson.map(_.validate[StateBenefitsUserData]) match {
      case Some(data: JsSuccess[StateBenefitsUserData]) => handleSaveUserData(nino, sessionDataId, data.value)
      case _ =>
        logger.warn(invalidRequestLogMessage)
        Future.successful(BadRequest)
    }
  }
}
