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

package controllers

import com.mongodb.*
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.*
import play.api.libs.json.Json
import play.api.mvc.BodyParsers
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.retrieve.{EmptyRetrieval, MdtpInformation}
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.mdtpInformation
import uk.gov.hmrc.disaaccount.controllers.AgentPreferencesController
import uk.gov.hmrc.disaaccount.controllers.actions.*
import uk.gov.hmrc.disaaccount.models.StoredAgentPreferences
import uk.gov.hmrc.disaaccount.utils.Constants.*
import utils.UnitSpecBase

import java.time.*
import scala.concurrent.Future

class AgentPreferencesControllerSpec extends UnitSpecBase {

  private val now        = Instant.parse("2026-11-13T10:00:00Z")
  private val sessionId  = "session-1"
  private val service    = mockAgentPreferencesService
  private val cc         = stubControllerComponents()
  private val authoriser = new AgentDelegatedEnrolmentAuthoriser(mockAuthConnector)
  private val controller = new AgentPreferencesController(
    cc,
    service,
    new AgentAuthActionImpl(mockAuthConnector, inject[BodyParsers.Default]),
    new AgentPreferencesGetActionImpl(authoriser, service, Clock.fixed(now, ZoneOffset.UTC)),
    new AgentPreferencesSetActionImpl(authoriser)
  )

  private val agentPredicate                   = AffinityGroup.Agent and Enrolment(agentEnrolment)
  private def delegatedPredicate(zref: String) =
    Enrolment(disaEnrolment).withIdentifier(zReferenceIdentifier, zref).withDelegatedAuthRule(delegatedAuthRule)

  private def agent(session: Option[String] = Some(sessionId)): Unit =
    when(mockAuthConnector.authorise(eqTo(agentPredicate), eqTo(mdtpInformation))(any, any))
      .thenReturn(Future.successful(session.map(MdtpInformation("device-1", _))))

  private def delegated(reference: String = testZref): Unit =
    when(mockAuthConnector.authorise(eqTo(delegatedPredicate(reference)), eqTo(EmptyRetrieval))(any, any))
      .thenReturn(Future.successful(()))

  private def rejectDelegated(): Unit =
    when(mockAuthConnector.authorise(eqTo(delegatedPredicate(testZref)), eqTo(EmptyRetrieval))(any, any))
      .thenReturn(Future.failed(InsufficientEnrolments()))

  private def getAgentPreferences()             =
    controller.getAgentPreferences(FakeRequest(GET, "/disa-account/agents/preferences"))
  private def setAgentPreferences(body: String) =
    controller.setAgentPreferences()(FakeRequest(PUT, "/disa-account/agents/preferences").withBody(Json.parse(body)))

  private val validBody         = s"""{"zReference":"$testZref"}"""
  private val relationshipError = Json.obj(
    errorCodeField    -> agentClientRelationshipNotEstablishedCode,
    errorMessageField -> agentClientRelationshipNotEstablishedMessage
  )

  "GET agent preferences" should {
    "return the stored preference after checking delegated access" in {
      agent()
      delegated()
      when(service.get(sessionId))
        .thenReturn(Future.successful(Some(StoredAgentPreferences(sessionId, testZref, now.plusSeconds(60)))))

      val result = getAgentPreferences()
      status(result)        shouldBe OK
      contentAsJson(result) shouldBe Json.obj("zReference" -> testZref)
    }

    "return 404 for a missing preference without checking delegated access" in {
      agent()
      when(service.get(sessionId)).thenReturn(Future.successful(None))

      val result = getAgentPreferences()
      status(result)        shouldBe NOT_FOUND
      contentAsJson(result) shouldBe Json.obj(
        errorCodeField    -> agentPreferencesNotFoundCode,
        errorMessageField -> s"$agentPreferencesNotFoundMessage$sessionId"
      )
      verify(mockAuthConnector, never()).authorise(eqTo(delegatedPredicate(testZref)), eqTo(EmptyRetrieval))(any, any)
    }

    "return 403 when delegated access is denied" in {
      agent()
      rejectDelegated()
      when(service.get(sessionId))
        .thenReturn(Future.successful(Some(StoredAgentPreferences(sessionId, testZref, now.plusSeconds(60)))))

      val result = getAgentPreferences()
      status(result)        shouldBe FORBIDDEN
      contentAsJson(result) shouldBe relationshipError
    }

    "not return a preference which has expired before the response" in {
      agent()
      delegated()
      when(service.get(sessionId))
        .thenReturn(Future.successful(Some(StoredAgentPreferences(sessionId, testZref, now))))

      status(getAgentPreferences()) shouldBe NOT_FOUND
    }

    "return 503 JSON when Mongo cannot be read" in {
      agent()
      when(service.get(sessionId)).thenReturn(Future.failed(new MongoTimeoutException("unavailable")))

      val result = getAgentPreferences()
      status(result)        shouldBe SERVICE_UNAVAILABLE
      contentAsJson(result) shouldBe Json.obj(
        errorCodeField    -> serviceUnavailableCode,
        errorMessageField -> serviceUnavailableMessage
      )
    }
  }

  "PUT agent preferences" should {
    "save the preference for the authenticated session" in {
      agent()
      delegated()
      when(service.set(sessionId, testZref)).thenReturn(Future.successful(true))

      status(setAgentPreferences(validBody)) shouldBe NO_CONTENT
      verify(service).set(sessionId, testZref)
    }

    Seq(
      """{"zReference":"Z123"}""",
      """{"zReference":"z1234"}""",
      """{"zReference":"Z12345"}""",
      """{"zReference":null}""",
      """{"zReference":1234}""",
      """{}"""
    ).foreach { body =>
      s"reject invalid body $body" in {
        agent()
        val result = setAgentPreferences(body)
        status(result)        shouldBe BAD_REQUEST
        contentAsJson(result) shouldBe Json.obj(
          errorCodeField    -> invalidZReferenceFormatCode,
          errorMessageField -> invalidZReferenceFormatMessage
        )
        verify(service, never()).set(any, any)
        verify(mockAuthConnector, never())
          .authorise(eqTo(delegatedPredicate(testZref)), eqTo(EmptyRetrieval))(any, any)
      }
    }

    "not save when delegated access is denied" in {
      agent()
      rejectDelegated()

      val result = setAgentPreferences(validBody)
      status(result)        shouldBe FORBIDDEN
      contentAsJson(result) shouldBe relationshipError
      verify(service, never()).set(any, any)
    }

    "not report success when Mongo does not acknowledge the write" in {
      agent()
      delegated()
      when(service.set(sessionId, testZref)).thenReturn(Future.successful(false))

      val result = setAgentPreferences(validBody)
      status(result)        shouldBe INTERNAL_SERVER_ERROR
      contentAsJson(result) shouldBe Json.obj(
        errorCodeField    -> internalServerErrorCode,
        errorMessageField -> internalServerErrorMessage
      )
    }

    Seq(new MongoTimeoutException("unavailable"), new MongoSocketException("unavailable", new ServerAddress())).foreach {
      exception =>
        s"return 503 when the preference write fails to connect: ${exception.getClass.getSimpleName}" in {
          agent()
          delegated()
          when(service.set(sessionId, testZref)).thenReturn(Future.failed(exception))

          val result = setAgentPreferences(validBody)
          status(result)        shouldBe SERVICE_UNAVAILABLE
          contentAsJson(result) shouldBe Json.obj(
            errorCodeField    -> serviceUnavailableCode,
            errorMessageField -> serviceUnavailableMessage
          )
          verify(service).set(sessionId, testZref)
        }
    }

    "return 500 when the preference write fails for another reason" in {
      agent()
      delegated()
      when(service.set(sessionId, testZref)).thenReturn(Future.failed(new RuntimeException()))

      val result = setAgentPreferences(validBody)
      status(result)        shouldBe INTERNAL_SERVER_ERROR
      contentAsJson(result) shouldBe Json.obj(
        errorCodeField    -> internalServerErrorCode,
        errorMessageField -> internalServerErrorMessage
      )
    }
  }

  "Both endpoints" should {
    Seq(UnsupportedAffinityGroup(), InsufficientEnrolments()).foreach { failure =>
      s"reject an account without agent affinity or an active agent enrolment: $failure" in {
        when(mockAuthConnector.authorise(eqTo(agentPredicate), eqTo(mdtpInformation))(any, any))
          .thenReturn(Future.failed(failure))

        Seq(getAgentPreferences(), setAgentPreferences(validBody)).foreach { result =>
          status(result)        shouldBe FORBIDDEN
          contentAsJson(result) shouldBe Json.obj(
            errorCodeField    -> userIsNotAnAgentCode,
            errorMessageField -> userIsNotAnAgentMessage
          )
        }
        verifyNoInteractions(service)
      }
    }

    "reject missing or blank session information from Auth" in {
      Seq(None, Some(""), Some("  ")).foreach { session =>
        agent(session)
        Seq(getAgentPreferences(), setAgentPreferences(validBody)).foreach { result =>
          status(result)        shouldBe FORBIDDEN
          contentAsJson(result) shouldBe Json.obj(
            errorCodeField    -> sessionIdNotAvailableCode,
            errorMessageField -> sessionIdNotAvailableMessage
          )
        }
      }
      verifyNoInteractions(service)
    }

    "return 401 for an expired or missing bearer token" in {
      when(mockAuthConnector.authorise(eqTo(agentPredicate), eqTo(mdtpInformation))(any, any))
        .thenReturn(Future.failed(MissingBearerToken()))

      status(getAgentPreferences())          shouldBe UNAUTHORIZED
      status(setAgentPreferences(validBody)) shouldBe UNAUTHORIZED
      verifyNoInteractions(service)
    }
  }
}
