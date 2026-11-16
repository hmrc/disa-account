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

package controllers.actions

import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.when
import play.api.libs.json.Json
import play.api.mvc.BodyParsers
import play.api.mvc.Results.Ok
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.retrieve.MdtpInformation
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.mdtpInformation
import uk.gov.hmrc.disaaccount.controllers.actions.AgentAuthActionImpl
import uk.gov.hmrc.disaaccount.utils.Constants.*
import utils.UnitSpecBase

import scala.concurrent.Future

class AgentAuthActionSpec extends UnitSpecBase {
  private val action    = new AgentAuthActionImpl(mockAuthConnector, inject[BodyParsers.Default])
  private val predicate = AffinityGroup.Agent and Enrolment(agentEnrolment)
  private val request   = FakeRequest(GET, "/disa-account/agents/preferences")

  private def invoke() =
    action.invokeBlock(request, agent => Future.successful(Ok(agent.sessionId)))

  "AgentAuthAction" should {
    "pass the session ID retrieved from Auth to the next action" in {
      when(mockAuthConnector.authorise(eqTo(predicate), eqTo(mdtpInformation))(any, any))
        .thenReturn(Future.successful(Some(MdtpInformation("device-1", "auth-session"))))

      contentAsString(invoke()) shouldBe "auth-session"
    }

    "reject a missing or blank Auth session ID" in
      Seq(None, Some("  ")).foreach { session =>
        when(mockAuthConnector.authorise(eqTo(predicate), eqTo(mdtpInformation))(any, any))
          .thenReturn(Future.successful(session.map(MdtpInformation("device-1", _))))

        val result = invoke()
        status(result)        shouldBe FORBIDDEN
        contentAsJson(result) shouldBe Json.obj(
          errorCodeField    -> sessionIdNotAvailableCode,
          errorMessageField -> sessionIdNotAvailableMessage
        )
      }

    "reject users without Agent affinity or an active agent enrolment" in
      Seq(UnsupportedAffinityGroup(), InsufficientEnrolments()).foreach { failure =>
        when(mockAuthConnector.authorise(eqTo(predicate), eqTo(mdtpInformation))(any, any))
          .thenReturn(Future.failed(failure))

        val result = invoke()
        status(result)        shouldBe FORBIDDEN
        contentAsJson(result) shouldBe Json.obj(
          errorCodeField    -> userIsNotAnAgentCode,
          errorMessageField -> userIsNotAnAgentMessage
        )
      }

    "return 401 when there is no active Auth session" in {
      when(mockAuthConnector.authorise(eqTo(predicate), eqTo(mdtpInformation))(any, any))
        .thenReturn(Future.failed(MissingBearerToken()))

      status(invoke()) shouldBe UNAUTHORIZED
    }
  }
}
