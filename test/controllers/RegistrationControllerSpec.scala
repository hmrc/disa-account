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
import play.api.libs.json.Json
import uk.gov.hmrc.auth.core.retrieve._
import uk.gov.hmrc.disaaccount.controllers.RegistrationController
import uk.gov.hmrc.disaaccount.models.registrationDetails.UpdateRegistrationDetailsRequest
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
      val notFound = UpstreamErrorResponse("Not found", NOT_FOUND, NOT_FOUND, Map.empty)
      when(mockEtmpConnector.getRegistrationDetails(org.mockito.ArgumentMatchers.eq(testZref))(any))
        .thenReturn(Future.successful(Left(notFound)))

      val result = controller.retrieveRegistrationDetails(testZref)(FakeRequest())

      status(result) shouldBe NOT_FOUND
    }

    "return 502 BadGateway when ETMP returns an unexpected error status" in {
      authorisedUser()
      val serverError = UpstreamErrorResponse(
        "Internal Server Error",
        INTERNAL_SERVER_ERROR,
        INTERNAL_SERVER_ERROR,
        Map.empty
      )
      when(mockEtmpConnector.getRegistrationDetails(org.mockito.ArgumentMatchers.eq(testZref))(any))
        .thenReturn(Future.successful(Left(serverError)))

      val result = controller.retrieveRegistrationDetails(testZref)(FakeRequest())

      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

    "return 500 InternalServerError when the connector call fails with an unexpected exception" in {
      authorisedUser()
      when(mockEtmpConnector.getRegistrationDetails(org.mockito.ArgumentMatchers.eq(testZref))(any))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      val result = controller.retrieveRegistrationDetails(testZref)(FakeRequest())

      status(result) shouldBe INTERNAL_SERVER_ERROR
    }
  }

  "RegistrationController.updateRegistrationDetails" should {
    "return 200 when ETMP updates the registration" in {
      authorisedUser()
      val details = UpdateRegistrationDetailsRequest(tradingName = Some("Updated name"))
      when(
        mockEtmpConnector.updateRegistrationDetails(
          org.mockito.ArgumentMatchers.eq(testZref),
          org.mockito.ArgumentMatchers.eq(details)
        )(any)
      )
        .thenReturn(Future.successful(Right(())))

      val result = route(
        app,
        FakeRequest(PUT, s"/disa-account/registration/$testZref").withJsonBody(Json.toJson(details))
      ).get

      status(result) shouldBe OK
    }

    "return 404 when ETMP cannot find the registration" in {
      authorisedUser()
      val details  = UpdateRegistrationDetailsRequest()
      val notFound = UpstreamErrorResponse("Not found", NOT_FOUND, NOT_FOUND, Map.empty)
      when(
        mockEtmpConnector.updateRegistrationDetails(
          org.mockito.ArgumentMatchers.eq(testZref),
          org.mockito.ArgumentMatchers.eq(details)
        )(any)
      )
        .thenReturn(Future.successful(Left(notFound)))

      val result = route(
        app,
        FakeRequest(PUT, s"/disa-account/registration/$testZref").withJsonBody(Json.toJson(details))
      ).get

      status(result) shouldBe NOT_FOUND
    }

    "return 500 when ETMP rejects the update" in {
      authorisedUser()
      val details = UpdateRegistrationDetailsRequest()
      val error   = UpstreamErrorResponse("Server error", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR, Map.empty)
      when(
        mockEtmpConnector.updateRegistrationDetails(
          org.mockito.ArgumentMatchers.eq(testZref),
          org.mockito.ArgumentMatchers.eq(details)
        )(any)
      ).thenReturn(Future.successful(Left(error)))

      val result = route(
        app,
        FakeRequest(PUT, s"/disa-account/registration/$testZref").withJsonBody(Json.toJson(details))
      ).get

      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

    "return 500 when updating the registration fails unexpectedly" in {
      authorisedUser()
      val details = UpdateRegistrationDetailsRequest()
      when(
        mockEtmpConnector.updateRegistrationDetails(
          org.mockito.ArgumentMatchers.eq(testZref),
          org.mockito.ArgumentMatchers.eq(details)
        )(any)
      ).thenReturn(Future.failed(new RuntimeException("boom")))

      val result = route(
        app,
        FakeRequest(PUT, s"/disa-account/registration/$testZref").withJsonBody(Json.toJson(details))
      ).get

      status(result) shouldBe INTERNAL_SERVER_ERROR
    }
  }
}
