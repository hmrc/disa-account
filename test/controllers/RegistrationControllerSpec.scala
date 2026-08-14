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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import play.api.test.Helpers._
import play.api.test._
import uk.gov.hmrc.auth.core.retrieve._
import uk.gov.hmrc.disaaccount.controllers.RegistrationController
import uk.gov.hmrc.http.UpstreamErrorResponse
import utils.BaseUnitSpec

import scala.concurrent.Future

class RegistrationControllerSpec extends BaseUnitSpec {

  val controller: RegistrationController = app.injector.instanceOf[RegistrationController]

  def authorisedUser(): Unit =
    when(mockAuthConnector.authorise(any, any[Retrieval[Unit]])(any, any))
      .thenReturn(Future.successful(()))

  "RegistrationController.retrieveRegistrationDetails" should {

    "return 200 OK with the registration details when found in ETMP" in {
      authorisedUser()
      when(mockEtmpConnector.getRegistrationDetails(org.mockito.ArgumentMatchers.eq(testZref))(any))
        .thenReturn(Future.successful(Right(testJourneyData)))

      val result = controller.retrieveRegistrationDetails(testZref)(FakeRequest())

      status(result)        shouldBe OK
      contentAsJson(result) shouldBe play.api.libs.json.Json.toJson(testJourneyData)
    }

    "return 404 Not Found when ETMP returns a 404" in {
      authorisedUser()
      val notFound = UpstreamErrorResponse("Not found", 404, 404, Map.empty)
      when(mockEtmpConnector.getRegistrationDetails(org.mockito.ArgumentMatchers.eq(testZref))(any))
        .thenReturn(Future.successful(Left(notFound)))

      val result = controller.retrieveRegistrationDetails(testZref)(FakeRequest())

      status(result) shouldBe NOT_FOUND
    }

    "return 502 BadGateway when ETMP returns an unexpected error status" in {
      authorisedUser()
      val serverError = UpstreamErrorResponse("Internal Server Error", 500, 500, Map.empty)
      when(mockEtmpConnector.getRegistrationDetails(org.mockito.ArgumentMatchers.eq(testZref))(any))
        .thenReturn(Future.successful(Left(serverError)))

      val result = controller.retrieveRegistrationDetails(testZref)(FakeRequest())

      status(result) shouldBe BAD_GATEWAY
    }

    "return 500 InternalServerError when the connector call fails with an unexpected exception" in {
      authorisedUser()
      when(mockEtmpConnector.getRegistrationDetails(org.mockito.ArgumentMatchers.eq(testZref))(any))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      val result = controller.retrieveRegistrationDetails(testZref)(FakeRequest())

      status(result) shouldBe INTERNAL_SERVER_ERROR
    }
  }
}
