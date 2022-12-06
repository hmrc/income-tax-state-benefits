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
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class StateBenefitsController @Inject()(stateBenefitsService: StateBenefitsService,
                                        authorisedAction: AuthorisedAction,
                                        cc: ControllerComponents)
                                       (implicit ec: ExecutionContext) extends BackendController(cc) with Logging {

  def getAllStateBenefitsData(nino: String, taxYear: Int): Action[AnyContent] = authorisedAction.async { implicit request =>
    stateBenefitsService.getAllStateBenefitsData(taxYear, nino).map {
      case Right(None) => NoContent
      case Right(Some(allStateBenefitsData)) => Ok(Json.toJson(allStateBenefitsData))
      case Left(_) => InternalServerError
    }
  }

  def saveUserData(): Action[AnyContent] = authorisedAction.async { implicit authRequest =>
    authRequest.request.body.asJson.map(_.validate[StateBenefitsUserData]) match {
      case Some(data: JsSuccess[StateBenefitsUserData]) => handleSaveUserData(data.value)
      case _ =>
        val logMessage = "[StateBenefitsController][saveUserData] Save state benefits request is invalid"
        logger.warn(logMessage)
        Future.successful(BadRequest)
    }
  }

  private def handleSaveUserData(userData: StateBenefitsUserData)
                                (implicit hc: HeaderCarrier): Future[Result] = {
    stateBenefitsService.saveUserData(userData).map {
      case Left(_) => InternalServerError
      case Right(_) => NoContent
    }
  }
}
