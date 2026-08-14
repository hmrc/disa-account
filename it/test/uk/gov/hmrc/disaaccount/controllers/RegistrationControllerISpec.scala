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

import play.api.http.Status._
import play.api.libs.json.Json
import play.api.test.Helpers.await
import uk.gov.hmrc.disaaccount.utils.BaseIntegrationSpec
import uk.gov.hmrc.disaaccount.utils.WiremockHelper.stubGet

class RegistrationControllerISpec extends BaseIntegrationSpec {

  val etmpRegistrationUrl: String = s"/etmp/registration/$testZref"

  def retrieveRegistrationDetailsRequest(
    zref: String = testZref,
    headers: Seq[(String, String)] = testHeaders
  ) = {
    stubAuth()
    await(
      ws.url(s"http://localhost:$port/disa-account/registration/$zref")
        .withHttpHeaders(headers: _*)
        .get()
    )
  }

  "GET /disa-account/registration/:zref" should {

    "return 200 OK with the registration details when ETMP finds a match" in {
      stubGet(etmpRegistrationUrl, OK, Json.stringify(Json.toJson(testJourneyData)))

      val result = retrieveRegistrationDetailsRequest()

      result.status shouldBe OK
      result.json   shouldBe Json.toJson(testJourneyData)
    }

    "return 404 Not Found when ETMP has no registration for the zref" in {
      stubGet(etmpRegistrationUrl, NOT_FOUND, """{"statusCode":404,"message":"Not found"}""")

      val result = retrieveRegistrationDetailsRequest()

      result.status shouldBe NOT_FOUND
    }

    "return 502 BadGateway when ETMP returns an unexpected error" in {
      stubGet(etmpRegistrationUrl, INTERNAL_SERVER_ERROR, """{"statusCode":500,"message":"Boom"}""")

      val result = retrieveRegistrationDetailsRequest()

      result.status shouldBe BAD_GATEWAY
    }

    "return 401 Unauthorized for an unauthorised request" in {
      stubAuthFail()

      val result = await(
        ws.url(s"http://localhost:$port/disa-account/registration/$testZref")
          .withHttpHeaders(testHeaders: _*)
          .get()
      )

      result.status shouldBe UNAUTHORIZED
    }
  }
}
