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

package uk.gov.hmrc.disaaccount.connectors

import play.api.http.Status.{NOT_FOUND, OK}
import play.api.libs.json.Json
import play.api.test.Helpers.await
import uk.gov.hmrc.disaaccount.models.registrationDetails.RegistrationDetails
import uk.gov.hmrc.disaaccount.utils.BaseIntegrationSpec
import uk.gov.hmrc.disaaccount.utils.WiremockHelper.stubGet
import uk.gov.hmrc.http.JsValidationException

class EtmpConnectorISpec extends BaseIntegrationSpec {

  val connector: EtmpConnector = app.injector.instanceOf[EtmpConnector]

  val registrationUrl: String = s"/etmp/registration/$testZref"

  "EtmpConnector.getRegistrationDetails" should {

    "return Right(RegistrationDetails) when the backend returns 200 OK with valid json" in {
      stubGet(registrationUrl, OK, Json.stringify(Json.toJson(testJourneyData)))

      val response = await(connector.getRegistrationDetails(testZref))

      response shouldBe Right(testJourneyData)
    }

    "return Left(UpstreamErrorResponse) when the backend returns a 404" in {
      stubGet(registrationUrl, NOT_FOUND, """{"statusCode":404,"message":"Not found"}""")

      val response = await(connector.getRegistrationDetails(testZref))

      response match {
        case Left(err) => err.statusCode shouldBe NOT_FOUND
        case Right(_)  => fail("Expected Left(UpstreamErrorResponse) but got Right")
      }
    }

    "propagate an exception when the backend returns invalid json" in {
      stubGet(registrationUrl, OK, """{"json":"bad"}""")

      val err = await(connector.getRegistrationDetails(testZref).failed)

      err shouldBe an[JsValidationException]
    }
  }
}
