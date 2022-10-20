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

import actions.AuthorisedAction
import models.mongo.StateBenefitsUserData
import play.api.Logging
import play.api.libs.json.{JsSuccess, Json}
import play.api.mvc.{Action, AnyContent, ControllerComponents, Result}
import services.StateBenefitsService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.util.UUID
import javax.inject.Inject
import scala.concurrent.Future

class UserSessionDataController @Inject()(authorisedAction: AuthorisedAction,
                                          stateBenefitsService: StateBenefitsService,
                                          cc: ControllerComponents) extends BackendController(cc) with Logging {

  def getStateBenefitsUserData(sessionDataId: UUID): Action[AnyContent] = authorisedAction.async { _ =>
    stateBenefitsService.getStateBenefitsUserData(sessionDataId) match {
      case Some(data) => Future.successful(Ok(Json.toJson(data)))
      case None => Future.successful(NotFound)
    }
  }

  def createOrUpdate(): Action[AnyContent] = authorisedAction.async { implicit authRequest =>
    authRequest.request.body.asJson.map(_.validate[StateBenefitsUserData]) match {
      case Some(data: JsSuccess[StateBenefitsUserData]) => responseHandler(data.value)
      case error =>
        logger.warn("[UserSessionDataController][createOrUpdate] Create update state benefits request is invalid" + error)
        Future.successful(BadRequest)
    }
  }

  private def responseHandler(stateBenefitsUserData: StateBenefitsUserData): Future[Result] = {
    val result: UUID = stateBenefitsService.createOrUpdateStateBenefitsUserData(stateBenefitsUserData)
    Future.successful(Ok(Json.toJson(result)))
  }
}
