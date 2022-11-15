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
import connectors.errors.ApiError
import models.api.StateBenefitDetailOverride
import play.api.Logging
import play.api.libs.json.{JsSuccess, Json}
import play.api.mvc.{Action, AnyContent, ControllerComponents, Result}
import services.StateBenefitsService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.util.UUID
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
      case Left(errorModel: ApiError) => Status(errorModel.status)(errorModel.toJson)
    }
  }

  def createOrUpdateStateBenefitDetailOverride(nino: String,
                                               taxYear: Int,
                                               benefitId: UUID): Action[AnyContent] = authorisedAction.async { implicit authRequest =>
    authRequest.request.body.asJson.map(_.validate[StateBenefitDetailOverride]) match {
      case Some(data: JsSuccess[StateBenefitDetailOverride]) => responseHandler(nino, taxYear, benefitId, data.value)
      case _ =>
        val logMessage = "[CreateOrUpdateStateBenefitDetailOverrideController][createOrUpdateStateBenefitDetailOverride] " +
          "Create or update state benefit detail override request is invalid"
        logger.warn(logMessage)
        Future.successful(BadRequest)
    }
  }

  def deleteStateBenefit(nino: String, taxYear: Int, benefitId: UUID): Action[AnyContent] = authorisedAction.async { implicit request =>
    stateBenefitsService.deleteStateBenefit(taxYear, nino, benefitId).map {
      case Left(errorModel) => Status(errorModel.status)(errorModel.toJson)
      case Right(_) => NoContent
    }
  }

  private def responseHandler(nino: String,
                              taxYear: Int,
                              benefitId: UUID,
                              stateBenefitDetailOverride: StateBenefitDetailOverride)
                             (implicit hc: HeaderCarrier): Future[Result] = {
    stateBenefitsService.createOrUpdateStateBenefitDetailOverride(taxYear, nino, benefitId, stateBenefitDetailOverride).map {
      case Left(errorModel: ApiError) => Status(errorModel.status)(errorModel.toJson)
      case Right(_) => NoContent
    }
  }
}
