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

import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{verify, verifyNoMoreInteractions, when}
import play.api.libs.json.Json
import play.api.mvc.BodyParsers
import play.api.mvc.Results.Ok
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.retrieve.EmptyRetrieval
import uk.gov.hmrc.disaaccount.controllers.actions.BtaAuthAction
import uk.gov.hmrc.disaaccount.utils.Constants.*
import uk.gov.hmrc.http.HeaderCarrier
import utils.UnitSpecBase

import scala.concurrent.Future

class BtaAuthActionSpec extends UnitSpecBase {
  private val action    = new BtaAuthAction(mockAuthConnector, inject[BodyParsers.Default])
  private val enrolment = Enrolment(disaEnrolment).withIdentifier(zReferenceIdentifier, testZref)
  private val request   = FakeRequest(GET, "/disa-account/client-details/business-tax-account/Z1234")
    .withHeaders("Authorization" -> "Bearer organisation-token")

  private def organisation(result: Future[Unit] = Future.successful(())): Unit =
    when(mockAuthConnector.authorise(eqTo(AffinityGroup.Organisation), eqTo(EmptyRetrieval))(any, any))
      .thenReturn(result)

  private def enrolled(result: Future[Unit] = Future.successful(())): Unit =
    when(mockAuthConnector.authorise(eqTo(enrolment), eqTo(EmptyRetrieval))(any, any)).thenReturn(result)

  private def invoke(reference: String = testZref) =
    action(reference).invokeBlock(request, _ => Future.successful(Ok("authorised")))

  "BtaAuthAction" should {
    "authorise Organisation affinity and the exact active principal enrolment using the incoming bearer token" in {
      organisation()
      enrolled()
      contentAsString(invoke()) shouldBe "authorised"
      val carrier = ArgumentCaptor.forClass(classOf[HeaderCarrier])
      val order   = org.mockito.Mockito.inOrder(mockAuthConnector)
      order
        .verify(mockAuthConnector)
        .authorise(eqTo(AffinityGroup.Organisation), eqTo(EmptyRetrieval))(carrier.capture(), any)
      order.verify(mockAuthConnector).authorise(eqTo(enrolment), eqTo(EmptyRetrieval))(any, any)
      carrier.getValue.authorization.map(_.value) shouldBe Some("Bearer organisation-token")
      verifyNoMoreInteractions(mockAuthConnector)
    }

    "reject non-organisation users before reference validation and enrolment authorisation" in {
      organisation(Future.failed(UnsupportedAffinityGroup()))
      val result = invoke("invalid")
      status(result)        shouldBe UNAUTHORIZED
      contentAsJson(result) shouldBe Json.obj(
        "code"    -> userIsNotAnOrganisationCode,
        "message" -> userIsNotAnOrganisationMessage
      )
      verify(mockAuthConnector).authorise(eqTo(AffinityGroup.Organisation), eqTo(EmptyRetrieval))(any, any)
      verifyNoMoreInteractions(mockAuthConnector)
    }

    Seq(MissingBearerToken(), InvalidBearerToken(), BearerTokenExpired()).foreach { error =>
      s"return a JSON 401 for ${error.getClass.getSimpleName}" in {
        organisation(Future.failed(error))
        val result = invoke()
        status(result)        shouldBe UNAUTHORIZED
        contentAsJson(result) shouldBe Json.obj("code" -> unauthorizedCode, "message" -> unauthorizedMessage)
      }
    }

    "validate the reference after Organisation authentication without checking an invalid enrolment" in {
      organisation()
      val result = invoke("z1234")
      status(result)        shouldBe BAD_REQUEST
      contentAsJson(result) shouldBe Json.obj(
        "code"    -> invalidZReferenceFormatCode,
        "message" -> invalidZReferenceFormatMessage
      )
      verify(mockAuthConnector).authorise(eqTo(AffinityGroup.Organisation), eqTo(EmptyRetrieval))(any, any)
      verifyNoMoreInteractions(mockAuthConnector)
    }

    "return 403 for a missing, inactive or mismatched DISA enrolment" in {
      organisation()
      enrolled(Future.failed(InsufficientEnrolments()))
      val result = invoke()
      status(result)        shouldBe FORBIDDEN
      contentAsJson(result) shouldBe Json.obj(
        "code"    -> disaEnrolmentNotFoundCode,
        "message" -> disaEnrolmentNotFoundMessage
      )
    }

    "return 401 if the session expires between Auth calls" in {
      organisation()
      enrolled(Future.failed(BearerTokenExpired()))
      val result = invoke()
      status(result)        shouldBe UNAUTHORIZED
      contentAsJson(result) shouldBe Json.obj("code" -> unauthorizedCode, "message" -> unauthorizedMessage)
    }

    "return 500 for an unexpected failure during either Auth call" in
      Seq(false, true).foreach { secondCall =>
        organisation(
          if (secondCall) Future.successful(()) else Future.failed(new RuntimeException("private auth failure"))
        )
        enrolled(Future.failed(new RuntimeException("private auth failure")))
        val result = invoke()
        status(result)        shouldBe INTERNAL_SERVER_ERROR
        contentAsJson(result) shouldBe Json.obj(
          "code"    -> internalServerErrorCode,
          "message" -> internalServerErrorMessage
        )
      }

    "leave downstream failures for the controller to handle" in {
      organisation()
      enrolled()
      val failure = new RuntimeException("downstream")
      val result  = action(testZref).invokeBlock(request, _ => Future.failed(failure))
      result.failed.futureValue shouldBe failure
    }
  }
}
