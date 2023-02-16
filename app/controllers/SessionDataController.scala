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
import play.api.Logging
import play.api.libs.json.{JsSuccess, Json}
import play.api.mvc.{Action, AnyContent, ControllerComponents, Result}
import services.StateBenefitsService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.util.UUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SessionDataController @Inject()(authorisedAction: AuthorisedAction,
                                      stateBenefitsService: StateBenefitsService,
                                      cc: ControllerComponents)
                                     (implicit ec: ExecutionContext) extends BackendController(cc) with Logging {

  lazy private val invalidCreateLogMessage = "[SessionDataController][create] Create state benefits request is invalid"
  lazy private val invalidUpdateLogMessage = "[SessionDataController][update] Update state benefits request is invalid"

  def getSessionData(nino: String, sessionDataId: UUID): Action[AnyContent] = authorisedAction.async { _ =>
    stateBenefitsService.getSessionData(nino, sessionDataId).map {
      case Right(data) => Ok(Json.toJson(data))
      case Left(DataNotFoundError) => NotFound
      case Left(_) => InternalServerError
    }
  }

  def create(): Action[AnyContent] = authorisedAction.async { implicit authRequest =>
    authRequest.request.body.asJson.map(_.validate[StateBenefitsUserData]) match {
      case Some(data: JsSuccess[StateBenefitsUserData]) => handleCreate(data.value)
      case _ =>
        logger.warn(invalidCreateLogMessage)
        Future.successful(BadRequest)
    }
  }

  def update(nino: String, sessionDataId: UUID): Action[AnyContent] = authorisedAction.async { implicit authRequest =>
    authRequest.request.body.asJson.map(_.validate[StateBenefitsUserData]) match {
      case Some(data: JsSuccess[StateBenefitsUserData]) => handleUpdate(nino, sessionDataId, data.value)
      case _ =>
        logger.warn(invalidUpdateLogMessage)
        Future.successful(BadRequest)
    }
  }

  private def handleCreate(stateBenefitsUserData: StateBenefitsUserData): Future[Result] = {
    stateBenefitsService.createSessionData(stateBenefitsUserData).map {
      case Left(_) => InternalServerError
      case Right(uuid) => Created(Json.toJson(uuid))
    }
  }

  private def handleUpdate(nino: String, sessionDataId: UUID, stateBenefitsUserData: StateBenefitsUserData): Future[Result] = {
    if (stateBenefitsUserData.nino != nino || !stateBenefitsUserData.sessionDataId.contains(sessionDataId)) {
      logger.warn(invalidUpdateLogMessage)
      Future.successful(BadRequest)
    } else {
      stateBenefitsService.updateSessionData(stateBenefitsUserData).map {
        case Left(_) => InternalServerError
        case Right(_) => NoContent
      }
    }
  }
}
