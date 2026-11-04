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
import scala.concurrent.duration.DurationInt
import play.api.Logger
import play.api.http.Status.*
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.test.Helpers.await
import uk.gov.hmrc.disaaccount.utils.BaseIntegrationSpec
import uk.gov.hmrc.play.bootstrap.tools.LogCapturing

class ClientDetailsControllerISpec extends BaseIntegrationSpec with LogCapturing {
  override def config: Map[String, Any] = super.config ++ Map("play.ws.timeout.request" -> "500ms")

  private val token = "client-details-test-token"
  private val etmpUrl = "/etmp/registration/Z1234"
  private val complete = Json.toJson(testJourneyData).as[JsObject]
  private val controllerLogger = Logger(classOf[ClientDetailsController])

  private def authorise(status: Int = OK): Unit =
    stubFor(post(urlEqualTo("/internal-auth/auth"))
      .withHeader("Authorization", equalTo(token))
      .willReturn(aResponse().withStatus(status).withHeader("Content-Type", "application/json")
        .withBody(if (status == OK) """{"retrievals":{}}""" else "")))

  private def etmp(body: JsValue = complete, status: Int = OK): Unit =
    stubFor(get(urlEqualTo(etmpUrl)).willReturn(aResponse().withStatus(status)
      .withHeader("Content-Type", "application/json").withBody(Json.stringify(body))))

  private def request(zref: String = "Z1234", credentials: Option[String] = Some(token)) = {
    val call = ws.url(s"http://localhost:$port/disa-account/client-details/$zref").withRequestTimeout(10.seconds)
    await(credentials.fold(call)(value => call.withHttpHeaders("Authorization" -> value)).get())
  }

  "GET /disa-account/client-details/:zref" should {
    "return only the authoritative company name and FCA number, preserving leading zeroes" in {
      authorise()
      etmp(complete ++ Json.obj("organisationDetails" -> Json.obj("fcaNumber" -> "0012345")))
      val result = request()
      result.status shouldBe OK
      result.json shouldBe Json.obj("businessName" -> "Test Isa Manager Ltd", "fcaNumber" -> "0012345")
      verify(postRequestedFor(urlEqualTo("/internal-auth/auth"))
        .withRequestBody(equalToJson("""{"predicate":{"resource":{"resourceType":"disa-account","resourceLocation":"client-details"},"action":"READ"},"retrieve":[]}""")))
    }

    Seq("12345", "X1234", "z1234", "Z123", "Z12345", "Z12A4", "Z1234%20", "%20Z1234").foreach { zref =>
      s"reject invalid identifier $zref without calling ETMP" in {
        authorise()
        request(zref).status shouldBe BAD_REQUEST
        verify(0, getRequestedFor(urlPathMatching("/etmp/.*")))
      }
    }

    "return 404 for a confirmed missing client" in {
      authorise() 
      etmp(status = NOT_FOUND)
      request().status shouldBe NOT_FOUND
      verify(1, getRequestedFor(urlEqualTo(etmpUrl)))
    }

    val incomplete = Seq(
      "missing businessVerification" -> (complete - "businessVerification"),
      "missing organisationDetails" -> (complete - "organisationDetails"),
      "missing company name with trading name present" -> (complete ++ Json.obj("businessVerification" -> Json.obj())),
      "missing FCA number" -> (complete ++ Json.obj("organisationDetails" -> Json.obj())),
      "null company name" -> (complete ++ Json.obj("businessVerification" -> Json.obj("companyName" -> Option.empty[String]))),
      "null FCA number" -> (complete ++ Json.obj("organisationDetails" -> Json.obj("fcaNumber" -> Option.empty[String])))
    ) ++ Seq("", "   ").flatMap { blank => Seq(
      s"blank company name [$blank]" -> (complete ++ Json.obj("businessVerification" -> Json.obj("companyName" -> blank))),
      s"blank FCA number [$blank]" -> (complete ++ Json.obj("organisationDetails" -> Json.obj("fcaNumber" -> blank)))
    ) }
    incomplete.foreach { case (description, body) =>
      s"return 500 and log $description" in {
        authorise(); etmp(body)
        withCaptureOfLoggingFrom(controllerLogger) { events =>
          request().status shouldBe INTERNAL_SERVER_ERROR
          events.exists(_.getMessage.contains("Incomplete ETMP client details")) shouldBe true
          events.exists(_.getMessage.contains("123456")) shouldBe false
        }
      }
    }

    Seq(UNAUTHORIZED, FORBIDDEN).foreach { status =>
      s"preserve internal-auth failure $status before validation and ETMP" in {
        authorise(status)
        request().status shouldBe status
        request("bad").status shouldBe status
        verify(0, getRequestedFor(urlPathMatching("/etmp/.*")))
      }
    }

    "reject missing credentials without calling ETMP" in {
      request(credentials = None).status shouldBe UNAUTHORIZED
      verify(0, getRequestedFor(urlPathMatching("/etmp/.*")))
    }

    Seq(BAD_REQUEST, INTERNAL_SERVER_ERROR, SERVICE_UNAVAILABLE).foreach { status =>
      s"return 500 and log upstream $status" in {
        authorise(); etmp(status = status)
        withCaptureOfLoggingFrom(controllerLogger) { events =>
          request().status shouldBe INTERNAL_SERVER_ERROR
          events.exists(_.getMessage.contains("ETMP lookup failed")) shouldBe true
        }
        verify(if (status >= 500) 4 else 1, getRequestedFor(urlEqualTo(etmpUrl)))
      }
    }

    "recover when an upstream server error succeeds on retry" in {
      authorise()
      stubFor(get(urlEqualTo(etmpUrl)).inScenario("retry").whenScenarioStateIs(STARTED)
        .willSetStateTo("ready").willReturn(aResponse().withStatus(SERVICE_UNAVAILABLE)))
      stubFor(get(urlEqualTo(etmpUrl)).inScenario("retry").whenScenarioStateIs("ready")
        .willReturn(okJson(Json.stringify(complete))))
      request().status shouldBe OK
      verify(2, getRequestedFor(urlEqualTo(etmpUrl)))
    }

    "return 500 and log malformed JSON without logging the response" in {
      authorise()
      stubFor(get(urlEqualTo(etmpUrl)).willReturn(okJson("private-invalid-json")))
      withCaptureOfLoggingFrom(controllerLogger) { events =>
        request().status shouldBe INTERNAL_SERVER_ERROR
        events.exists(_.getMessage.contains("ETMP lookup failed")) shouldBe true
        events.exists(_.getMessage.contains("private-invalid-json")) shouldBe false
      }
    }

    "return 500 and log a connection failure" in {
      authorise()
      stubFor(get(urlEqualTo(etmpUrl)).willReturn(aResponse()
        .withFault(com.github.tomakehurst.wiremock.http.Fault.CONNECTION_RESET_BY_PEER)))
      withCaptureOfLoggingFrom(controllerLogger) { events =>
        request().status shouldBe INTERNAL_SERVER_ERROR
        events.exists(_.getMessage.contains("ETMP lookup failed")) shouldBe true
      }
    }

    "return 500 and log a bounded upstream timeout" in {
      authorise()
      stubFor(get(urlEqualTo(etmpUrl)).willReturn(okJson(Json.stringify(complete)).withFixedDelay(1500)))
      withCaptureOfLoggingFrom(controllerLogger) { events =>
        request().status shouldBe INTERNAL_SERVER_ERROR
        events.exists(_.getMessage.contains("ETMP lookup failed")) shouldBe true
      }
    }
  }
}
