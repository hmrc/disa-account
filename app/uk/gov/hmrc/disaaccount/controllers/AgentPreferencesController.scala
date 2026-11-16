/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.disaaccount.controllers

import com.mongodb.{MongoSocketException, MongoTimeoutException}
import play.api.Logging
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.*
import uk.gov.hmrc.disaaccount.controllers.actions.*
import uk.gov.hmrc.disaaccount.models.{AgentPreferences, ErrorResponse}
import uk.gov.hmrc.disaaccount.services.AgentPreferencesService
import uk.gov.hmrc.disaaccount.utils.Constants.*
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class AgentPreferencesController @Inject() (
  cc: ControllerComponents,
  service: AgentPreferencesService,
  agentAuthAction: AgentAuthAction,
  agentPreferencesGetAction: AgentPreferencesGetAction,
  agentPreferencesSetAction: AgentPreferencesSetAction
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  def getAgentPreferences: Action[AnyContent] = (agentAuthAction andThen agentPreferencesGetAction).async { request =>
    logger.debug(
      s"[AgentPreferencesController][getAgentPreferences] Returning agent preferences for session ID [${request.sessionId}]"
    )
    Future.successful(Ok(Json.toJson(AgentPreferences(request.zReference))))
  }

  def setAgentPreferences(): Action[JsValue] =
    (agentAuthAction andThen agentPreferencesSetAction).async(parse.json) { request =>
      service
        .set(request.sessionId, request.zReference)
        .map {
          case true  =>
            logger.debug(
              s"[AgentPreferencesController][setAgentPreferences] Agent preferences saved for session ID [${request.sessionId}]"
            )
            NoContent
          case false =>
            logger.warn("[AgentPreferencesController][setAgentPreferences] Database write was not acknowledged")
            InternalServerError(Json.toJson(ErrorResponse(internalServerErrorCode, internalServerErrorMessage)))
        }
        .recover {
          case exception @ (_: MongoTimeoutException | _: MongoSocketException) =>
            logger.error("[AgentPreferencesController][setAgentPreferences] Failed to connect to MongoDB", exception)
            ServiceUnavailable(Json.toJson(ErrorResponse(serviceUnavailableCode, serviceUnavailableMessage)))
          case NonFatal(exception)                                              =>
            logger.error(
              "[AgentPreferencesController][setAgentPreferences] Failed to save agent preferences",
              exception
            )
            InternalServerError(Json.toJson(ErrorResponse(internalServerErrorCode, internalServerErrorMessage)))
        }
    }
}
