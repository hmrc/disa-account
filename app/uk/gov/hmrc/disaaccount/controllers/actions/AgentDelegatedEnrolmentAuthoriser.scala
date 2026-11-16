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
import play.api.mvc.{Result, Results}
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.retrieve.EmptyRetrieval
import uk.gov.hmrc.disaaccount.models.ErrorResponse
import uk.gov.hmrc.disaaccount.utils.Constants.*
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AgentDelegatedEnrolmentAuthoriser @Inject() (authConnector: AuthConnector)(implicit ec: ExecutionContext)
    extends Logging {
  def authorise[A](request: AgentRequest[A], zReference: String): Future[Option[Result]] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequest(request)

    val delegatedEnrolmentPredicate =
      Enrolment(disaEnrolment).withIdentifier(zReferenceIdentifier, zReference).withDelegatedAuthRule(delegatedAuthRule)

    logger.debug(s"[AgentDelegatedEnrolmentAuthoriser][authorise] Checking DISA enrolment access for session ID [${request.sessionId}]")
    authConnector
      .authorise(delegatedEnrolmentPredicate, EmptyRetrieval)
      .map[Option[Result]] { _ =>
        logger.debug(s"[AgentDelegatedEnrolmentAuthoriser][authorise] DISA enrolment access authorised for session ID [${request.sessionId}]")
        None
      }
      .recover {
        case _: InsufficientEnrolments | _: FailedRelationship =>
          logger.warn("[AgentDelegatedEnrolmentAuthoriser][authorise] DISA enrolment access denied")
          Some(agentClientRelationshipNotEstablished)
        case _: NoActiveSession                                =>
          logger.warn("[AgentDelegatedEnrolmentAuthoriser][authorise] No active Auth session")
          Some(Results.Unauthorized)
      }
  }

  private def agentClientRelationshipNotEstablished: Result =
    Results.Forbidden(Json.toJson(ErrorResponse(agentClientRelationshipNotEstablishedCode, agentClientRelationshipNotEstablishedMessage)))
}
