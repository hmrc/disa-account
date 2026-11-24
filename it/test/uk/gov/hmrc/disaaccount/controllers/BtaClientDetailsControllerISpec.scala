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

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED
import play.api.Logger
import play.api.http.Status.*
import play.api.libs.json.*
import play.api.test.Helpers.await
import uk.gov.hmrc.disaaccount.utils.IntegrationSpecBase
import uk.gov.hmrc.disaaccount.utils.Constants.*
import uk.gov.hmrc.play.bootstrap.tools.LogCapturing

import scala.concurrent.duration.*

class BtaClientDetailsControllerISpec extends IntegrationSpecBase with LogCapturing {
  override def config: Map[String, Any] = super.config ++ Map("play.ws.timeout.request" -> "500ms")
  private val token = "Bearer organisation-token"
  private val etmpUrl = s"/etmp/registration/$testZref"
  private val complete = Json.toJson(testJourneyData)
  private val controllerLogger = Logger(classOf[ClientDetailsController])
  private val organisationPredicate = """{"authorise":[{"affinityGroup":"Organisation"}],"retrieve":[]}"""
  private val enrolmentPredicate =
    s"""{"authorise":[{"enrolment":"$disaEnrolment","identifiers":[{"key":"$zReferenceIdentifier","value":"$testZref"}],"state":"Activated"}],"retrieve":[]}"""

  private def auth(predicate: String, failure: Option[String] = None, status: Int = OK): Unit = {
    val response = failure.fold(aResponse().withStatus(status).withHeader("Content-Type", "application/json").withBody("{}")) { reason =>
      aResponse().withStatus(UNAUTHORIZED).withHeader("WWW-Authenticate", s"""MDTP detail="$reason"""")
    }
    stubFor(post(urlEqualTo("/auth/authorise"))
      .withHeader("Authorization", equalTo(token))
      .withRequestBody(equalToJson(predicate))
      .willReturn(response))
  }

  private def authorised(): Unit = {
    auth(organisationPredicate)
    auth(enrolmentPredicate)
  }

  private def etmp(body: JsValue = complete, status: Int = OK): Unit =
    stubFor(get(urlEqualTo(etmpUrl)).willReturn(aResponse().withStatus(status).withHeader("Content-Type", "application/json").withBody(Json.stringify(body))))

  private def request(reference: String = testZref, credentials: Option[String] = Some(token)) = {
    val call = ws.url(s"http://localhost:$port/disa-account/client-details/business-tax-account/$reference").withRequestTimeout(10.seconds)
    await(credentials.fold(call)(value => call.withHttpHeaders("Authorization" -> value)).get())
  }

  private def noEtmp(): Unit = verify(0, getRequestedFor(urlPathMatching("/etmp/.*")))
  private def error(code: String, message: String) = Json.obj("code" -> code, "message" -> message)

  "GET /disa-account/client-details/business-tax-account/:zReference" should {
    "return registration details for the organisation's exact active DISA enrolment using its bearer token" in {
      authorised()
      etmp()
      val response = request()
      response.status shouldBe OK
      response.json shouldBe complete
      verify(1, postRequestedFor(urlEqualTo("/auth/authorise")).withHeader("Authorization", equalTo(token)).withRequestBody(equalToJson(organisationPredicate)))
      verify(1, postRequestedFor(urlEqualTo("/auth/authorise")).withHeader("Authorization", equalTo(token)).withRequestBody(equalToJson(enrolmentPredicate)))
      verify(0, postRequestedFor(urlEqualTo("/internal-auth/auth")))
      verify(1, getRequestedFor(urlEqualTo(etmpUrl)))
    }

    "return JSON 401 for missing bearer credentials without contacting Auth or ETMP" in {
      val response = request(credentials = None)
      response.status shouldBe UNAUTHORIZED
      response.json shouldBe error(unauthorizedCode, unauthorizedMessage)
      verify(0, postRequestedFor(urlEqualTo("/auth/authorise")))
      noEtmp()
    }

    Seq("InvalidBearerToken", "BearerTokenExpired", "SessionRecordNotFound").foreach { reason =>
      s"return JSON 401 for $reason before ETMP" in {
        auth(organisationPredicate, Some(reason))
        val response = request()
        response.status shouldBe UNAUTHORIZED
        response.json shouldBe error(unauthorizedCode, unauthorizedMessage)
        noEtmp()
      }
    }

    "return 401 for non-Organisation affinity before validating the reference" in {
      auth(organisationPredicate, Some("UnsupportedAffinityGroup"))
      val response = request("invalid")
      response.status shouldBe UNAUTHORIZED
      response.json shouldBe error(userIsNotAnOrganisationCode, userIsNotAnOrganisationMessage)
      verify(1, postRequestedFor(urlEqualTo("/auth/authorise")))
      noEtmp()
    }

    Seq("12345", "X1234", "z1234", "Z123", "Z12345", "Z12A4", "Z1234%20", "%20Z1234").foreach { reference =>
      s"return 400 for invalid reference $reference without checking enrolment or calling ETMP" in {
        auth(organisationPredicate)
        val response = request(reference)
        response.status shouldBe BAD_REQUEST
        response.json shouldBe error(invalidZReferenceFormatCode, invalidZReferenceFormatMessage)
        verify(1, postRequestedFor(urlEqualTo("/auth/authorise")))
        noEtmp()
      }
    }

    "return 403 when Auth rejects the active DISA enrolment for the requested reference" in {
      auth(organisationPredicate)
      auth(enrolmentPredicate, Some("InsufficientEnrolments"))
      val response = request()
      response.status shouldBe FORBIDDEN
      response.json shouldBe error(disaEnrolmentNotFoundCode, disaEnrolmentNotFoundMessage)
      noEtmp()
    }

    "return 401 when the session expires during the enrolment check" in {
      auth(organisationPredicate)
      auth(enrolmentPredicate, Some("BearerTokenExpired"))
      val response = request()
      response.status shouldBe UNAUTHORIZED
      response.json shouldBe error(unauthorizedCode, unauthorizedMessage)
      noEtmp()
    }

    Seq(organisationPredicate, enrolmentPredicate).foreach { predicate =>
      s"return JSON 500 for unexpected Auth failure at $predicate" in {
        authorised()
        auth(predicate, status = INTERNAL_SERVER_ERROR)
        val response = request()
        response.status shouldBe INTERNAL_SERVER_ERROR
        response.json shouldBe error(internalServerErrorCode, internalServerErrorMessage)
        noEtmp()
      }
    }

    Seq(NOT_FOUND, SERVICE_UNAVAILABLE, INTERNAL_SERVER_ERROR, BAD_REQUEST, UNAUTHORIZED, FORBIDDEN).foreach { upstream =>
      s"map ETMP status $upstream to the BTA error contract" in {
        authorised()
        etmp(status = upstream)
        val response = request()
        val (expectedStatus, code, message) = upstream match {
          case NOT_FOUND => (NOT_FOUND, clientDetailsNotFoundCode, clientDetailsNotFoundMessage)
          case SERVICE_UNAVAILABLE => (SERVICE_UNAVAILABLE, serviceUnavailableCode, serviceUnavailableMessage)
          case _ => (INTERNAL_SERVER_ERROR, internalServerErrorCode, internalServerErrorMessage)
        }
        response.status shouldBe expectedStatus
        response.json shouldBe error(code, message)
        verify(if (upstream >= 500) 4 else 1, getRequestedFor(urlEqualTo(etmpUrl)))
      }
    }

    "return 200 when ETMP recovers from 503 on retry" in {
      authorised()
      stubFor(get(urlEqualTo(etmpUrl)).inScenario("bta-retry").whenScenarioStateIs(STARTED).willSetStateTo("ready").willReturn(aResponse().withStatus(SERVICE_UNAVAILABLE)))
      stubFor(get(urlEqualTo(etmpUrl)).inScenario("bta-retry").whenScenarioStateIs("ready").willReturn(okJson(Json.stringify(complete))))
      request().status shouldBe OK
      verify(2, getRequestedFor(urlEqualTo(etmpUrl)))
    }

    Seq("malformed-json", "invalid-registration", "connection-reset", "timeout").foreach { failure =>
      s"return JSON 500 for $failure without logging response data" in {
        authorised()
        val responseDefinition = failure match {
          case "malformed-json" => okJson("private-invalid-json")
          case "invalid-registration" => okJson("""{"private":"invalid-registration"}""")
          case "connection-reset" => aResponse().withFault(com.github.tomakehurst.wiremock.http.Fault.CONNECTION_RESET_BY_PEER)
          case "timeout" => okJson(Json.stringify(complete)).withFixedDelay(1500)
        }
        stubFor(get(urlEqualTo(etmpUrl)).willReturn(responseDefinition))
        withCaptureOfLoggingFrom(controllerLogger) { events =>
          val response = request()
          response.status shouldBe INTERNAL_SERVER_ERROR
          response.json shouldBe error(internalServerErrorCode, internalServerErrorMessage)
          events.exists(_.getMessage.contains("ETMP lookup failed")) shouldBe true
          events.exists(_.getMessage.contains("private")) shouldBe false
          events.exists(_.getMessage.contains(token)) shouldBe false
        }
      }
    }
  }
}
