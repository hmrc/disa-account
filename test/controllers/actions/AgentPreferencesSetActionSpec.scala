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
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Results.Ok
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.retrieve.EmptyRetrieval
import uk.gov.hmrc.disaaccount.controllers.actions.*
import uk.gov.hmrc.disaaccount.utils.Constants.*
import utils.UnitSpecBase

import scala.concurrent.Future

class AgentPreferencesSetActionSpec extends UnitSpecBase {
  private val action    = new AgentPreferencesSetActionImpl(
    new AgentDelegatedEnrolmentAuthoriser(mockAuthConnector)
  )
  private val predicate =
    Enrolment(disaEnrolment).withIdentifier(zReferenceIdentifier, testZref).withDelegatedAuthRule(delegatedAuthRule)
  private val validBody = Json.obj("zReference" -> testZref)

  private def invoke(body: JsValue = validBody) =
    action.invokeBlock(
      AgentRequest(FakeRequest(PUT, "/disa-account/agents/preferences").withBody(body), "auth-session"),
      identified => Future.successful(Ok(identified.zReference))
    )

  private val relationshipError = Json.obj(
    errorCodeField    -> agentClientRelationshipNotEstablishedCode,
    errorMessageField -> agentClientRelationshipNotEstablishedMessage
  )

  "AgentPreferencesSetAction" should {
    "allow Auth approval without retrieving agent code or enrolments" in {
      when(mockAuthConnector.authorise(eqTo(predicate), eqTo(EmptyRetrieval))(any, any))
        .thenReturn(Future.successful(()))

      contentAsString(invoke()) shouldBe testZref
    }

    "reject an invalid PUT body before checking delegated access" in {
      val result = invoke(Json.obj("zReference" -> "invalid"))
      status(result)        shouldBe BAD_REQUEST
      contentAsJson(result) shouldBe Json.obj(
        errorCodeField    -> invalidZReferenceFormatCode,
        errorMessageField -> invalidZReferenceFormatMessage
      )
      verify(mockAuthConnector, never()).authorise(eqTo(predicate), eqTo(EmptyRetrieval))(any, any)
    }

    "reject a non-JSON body when invoked directly" in {
      val request = AgentRequest(FakeRequest(PUT, "/disa-account/agents/preferences").withBody("not-json"), "auth-session")
      val result  = action.invokeBlock(request, identified => Future.successful(Ok(identified.zReference)))

      status(result)        shouldBe BAD_REQUEST
      contentAsJson(result) shouldBe Json.obj(
        errorCodeField    -> invalidZReferenceFormatCode,
        errorMessageField -> invalidZReferenceFormatMessage
      )
      verify(mockAuthConnector, never()).authorise(eqTo(predicate), eqTo(EmptyRetrieval))(any, any)
    }

    "reject a failed delegated check" in
      Seq(InsufficientEnrolments(), FailedRelationship()).foreach { failure =>
        when(mockAuthConnector.authorise(eqTo(predicate), eqTo(EmptyRetrieval))(any, any))
          .thenReturn(Future.failed(failure))

        val result = invoke()
        status(result)        shouldBe FORBIDDEN
        contentAsJson(result) shouldBe relationshipError
      }

    "return 401 when there is no active Auth session" in {
      when(mockAuthConnector.authorise(eqTo(predicate), eqTo(EmptyRetrieval))(any, any))
        .thenReturn(Future.failed(MissingBearerToken()))

      status(invoke()) shouldBe UNAUTHORIZED
    }
  }
}
