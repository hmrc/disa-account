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

package uk.gov.hmrc.disaaccount.controllers.actions

import play.api.Logging
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.*
import uk.gov.hmrc.disaaccount.models.{AgentPreferences, ErrorResponse}
import uk.gov.hmrc.disaaccount.utils.Constants.*
import uk.gov.hmrc.disaaccount.validators.ZReferenceValidator

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

trait AgentPreferencesSetAction extends ActionRefiner[AgentRequest, AgentPreferencesRequest]

class AgentPreferencesSetActionImpl @Inject() (authoriser: AgentDelegatedEnrolmentAuthoriser)(implicit
  val executionContext: ExecutionContext
) extends AgentPreferencesSetAction
    with Logging {

  override protected def refine[A](request: AgentRequest[A]): Future[Either[Result, AgentPreferencesRequest[A]]] = {
    val zReference = request.body match {
      case body: JsValue => body.asOpt[AgentPreferences].map(_.zReference)
      case _             => None
    }
    zReference.filter(ZReferenceValidator.isValid) match {
      case Some(value) =>
        logger.debug(
          s"[AgentPreferencesSetAction][refine] Valid preference submitted for session ID [${request.sessionId}]"
        )
        authoriser.authorise(request, value).map(_.toLeft(AgentPreferencesRequest(request, value)))
      case None        =>
        logger.warn("[AgentPreferencesSetAction][refine] Invalid agent preferences body")
        Future.successful(
          Left(
            Results.BadRequest(
              Json.toJson(ErrorResponse(invalidZReferenceFormatCode, invalidZReferenceFormatMessage))
            )
          )
        )
    }
  }
}
