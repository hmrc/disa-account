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
import org.mockito.Mockito.*
import play.api.libs.json.Json
import play.api.mvc.Results.Ok
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.retrieve.EmptyRetrieval
import uk.gov.hmrc.disaaccount.controllers.actions.*
import uk.gov.hmrc.disaaccount.models.StoredAgentPreferences
import uk.gov.hmrc.disaaccount.utils.Constants.*
import utils.UnitSpecBase

import java.time.*
import scala.concurrent.Future

class AgentPreferencesGetActionSpec extends UnitSpecBase {
  private val now       = Instant.parse("2026-11-13T10:00:00Z")
  private val action    =
    new AgentPreferencesGetActionImpl(
      new AgentDelegatedEnrolmentAuthoriser(mockAuthConnector),
      mockAgentPreferencesService,
      Clock.fixed(now, ZoneOffset.UTC)
    )
  private val predicate =
    Enrolment(disaEnrolment).withIdentifier(zReferenceIdentifier, testZref).withDelegatedAuthRule(delegatedAuthRule)
  private val request   = AgentRequest(FakeRequest(GET, "/disa-account/agents/preferences"), "auth-session")

  private def get() =
    action.invokeBlock(request, identified => Future.successful(Ok(identified.zReference)))

  private val relationshipError = Json.obj(
    errorCodeField    -> agentClientRelationshipNotEstablishedCode,
    errorMessageField -> agentClientRelationshipNotEstablishedMessage
  )

  "AgentPreferencesGetAction" should {
    "load a preference through the service and check delegated access" in {
      when(mockAgentPreferencesService.get(request.sessionId))
        .thenReturn(Future.successful(Some(StoredAgentPreferences(request.sessionId, testZref, now.plusSeconds(60)))))
      when(mockAuthConnector.authorise(eqTo(predicate), eqTo(EmptyRetrieval))(any, any))
        .thenReturn(Future.successful(()))

      contentAsString(get()) shouldBe testZref
    }

    "return 404 without a delegated check when no preference exists" in {
      when(mockAgentPreferencesService.get(request.sessionId)).thenReturn(Future.successful(None))

      val result = get()
      status(result)        shouldBe NOT_FOUND
      contentAsJson(result) shouldBe Json.obj(
        errorCodeField    -> agentPreferencesNotFoundCode,
        errorMessageField -> s"$agentPreferencesNotFoundMessage${request.sessionId}"
      )
      verify(mockAuthConnector, never()).authorise(eqTo(predicate), eqTo(EmptyRetrieval))(any, any)
    }

    "return 503 without a delegated check when the preference lookup fails" in {
      when(mockAgentPreferencesService.get(request.sessionId)).thenReturn(Future.failed(new RuntimeException()))

      val result = get()
      status(result)        shouldBe SERVICE_UNAVAILABLE
      contentAsJson(result) shouldBe Json.obj(
        errorCodeField    -> serviceUnavailableCode,
        errorMessageField -> serviceUnavailableMessage
      )
      verify(mockAuthConnector, never()).authorise(eqTo(predicate), eqTo(EmptyRetrieval))(any, any)
    }

    "reject a preference that expires during the delegated check" in {
      when(mockAgentPreferencesService.get(request.sessionId))
        .thenReturn(Future.successful(Some(StoredAgentPreferences(request.sessionId, testZref, now))))
      when(mockAuthConnector.authorise(eqTo(predicate), eqTo(EmptyRetrieval))(any, any))
        .thenReturn(Future.successful(()))

      status(get()) shouldBe NOT_FOUND
    }

    "reject GET when delegated access is denied" in {
      when(mockAgentPreferencesService.get(request.sessionId))
        .thenReturn(Future.successful(Some(StoredAgentPreferences(request.sessionId, testZref, now.plusSeconds(60)))))
      when(mockAuthConnector.authorise(eqTo(predicate), eqTo(EmptyRetrieval))(any, any))
        .thenReturn(Future.failed(InsufficientEnrolments()))

      val result = get()
      status(result)        shouldBe FORBIDDEN
      contentAsJson(result) shouldBe relationshipError
    }
  }
}
