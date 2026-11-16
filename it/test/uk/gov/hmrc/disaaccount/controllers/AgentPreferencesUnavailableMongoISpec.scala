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
import com.mongodb.MongoTimeoutException
import org.mockito.Mockito.{mock, when}
import play.api.Application
import play.api.http.Status.SERVICE_UNAVAILABLE
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.libs.ws.writeableOf_String
import play.api.test.Helpers.await
import uk.gov.hmrc.disaaccount.services.AgentPreferencesService
import uk.gov.hmrc.disaaccount.utils.IntegrationSpecBase
import uk.gov.hmrc.disaaccount.utils.Constants.*

import scala.concurrent.Future

class AgentPreferencesUnavailableMongoISpec extends IntegrationSpecBase {
  private lazy val service = mock(classOf[AgentPreferencesService])

  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure(config)
    .overrides(bind[AgentPreferencesService].toInstance(service))
    .build()

  "Agent preferences endpoints" should {
    "return 503 JSON for GET and PUT when Mongo times out" in {
      when(service.get("session-1")).thenReturn(Future.failed(new MongoTimeoutException("unavailable")))
      when(service.set("session-1", testZref)).thenReturn(Future.failed(new MongoTimeoutException("unavailable")))
      stubFor(
        post(urlEqualTo("/auth/authorise"))
          .withHeader("Authorization", equalTo("Bearer agent-token"))
          .withRequestBody(containing("\"mdtpInformation\""))
          .willReturn(okJson("""{"mdtpInformation":{"deviceId":"device-1","sessionId":"session-1"}}"""))
      )
      stubFor(
        post(urlEqualTo("/auth/authorise"))
          .withHeader("Authorization", equalTo("Bearer agent-token"))
          .withRequestBody(containing("\"delegatedAuthRule\":\"disa-auth\""))
           .willReturn(okJson("{}"))
      )

      val endpoint = s"http://localhost:$port/disa-account/agents/preferences"
      val headers  = Seq("Authorization" -> "Bearer agent-token")
      val expected = Json.obj(errorCodeField -> serviceUnavailableCode, errorMessageField -> serviceUnavailableMessage)

      val read = await(ws.url(endpoint).withHttpHeaders(headers: _*).get())
      read.status shouldBe SERVICE_UNAVAILABLE
      read.json   shouldBe expected

      val write = await(
        ws.url(endpoint)
          .withHttpHeaders((headers :+ ("Content-Type" -> "application/json")): _*)
          .put(s"""{"zReference":"$testZref"}""")
      )
      write.status shouldBe SERVICE_UNAVAILABLE
      write.json shouldBe expected
      verify(1, postRequestedFor(urlEqualTo("/auth/authorise")).withRequestBody(containing("\"delegatedAuthRule\"")))
    }
  }
}
