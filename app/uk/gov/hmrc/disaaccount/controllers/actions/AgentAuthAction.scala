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
import play.api.libs.json.Json
import play.api.mvc.*
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.mdtpInformation
import uk.gov.hmrc.disaaccount.models.ErrorResponse
import uk.gov.hmrc.disaaccount.utils.Constants.*
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

case class AgentRequest[A](request: Request[A], sessionId: String) extends WrappedRequest[A](request)

trait AgentAuthAction extends ActionBuilder[AgentRequest, AnyContent] with ActionFunction[Request, AgentRequest]

class AgentAuthActionImpl @Inject() (authConnector: AuthConnector, val parser: BodyParsers.Default)(implicit
  val executionContext: ExecutionContext
) extends AgentAuthAction
    with Logging {

  override def invokeBlock[A](request: Request[A], block: AgentRequest[A] => Future[Result]): Future[Result] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequest(request)

    logger.debug("[AgentAuthAction][invokeBlock] Checking agent identity")
    authConnector
      .authorise(AffinityGroup.Agent and Enrolment(agentEnrolment), mdtpInformation)
      .map {
        case Some(info) if info.sessionId.trim.nonEmpty =>
          logger.debug(s"[AgentAuthAction][invokeBlock] Agent identity confirmed for session ID [${info.sessionId}]")
          Right(info.sessionId)
        case _                                          =>
          logger.warn("[AgentAuthAction][invokeBlock] Auth did not provide a session ID")
          Left(Results.Forbidden(Json.toJson(ErrorResponse(sessionIdNotAvailableCode, sessionIdNotAvailableMessage))))
      }
      .recover {
        case _: UnsupportedAffinityGroup | _: InsufficientEnrolments =>
          logger.warn("[AgentAuthAction][invokeBlock] Agent authorisation denied")
          Left(Results.Forbidden(Json.toJson(ErrorResponse(userIsNotAnAgentCode, userIsNotAnAgentMessage))))
        case _: NoActiveSession                                      =>
          logger.warn("[AgentAuthAction][invokeBlock] No active Auth session")
          Left(Results.Unauthorized)
      }
      .flatMap {
        case Right(sessionId) => block(AgentRequest(request, sessionId))
        case Left(result)     => Future.successful(result)
      }
  }

}
