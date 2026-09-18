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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatest.matchers.must.Matchers.mustBe
import play.api.http.Status.{NOT_FOUND, OK}
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.disaaccount.models.registrationDetails.{RegistrationDetails, UpdateRegistrationDetailsRequest}
import uk.gov.hmrc.disaaccount.models.registrationDetails.isaProducts.IsaProducts
import uk.gov.hmrc.http.{HttpResponse, StringContextOps, UpstreamErrorResponse}
import utils.BaseUnitSpec

import scala.concurrent.Future

class EtmpConnectorSpec extends BaseUnitSpec {

  trait TestSetup {
    val connector: EtmpConnector = new EtmpConnector(mockHttpClient, mockAppConfig, retryConfig, actorSystem)

    val testUrl: String = "http://localhost:1201"
    when(mockAppConfig.etmpBaseUrl).thenReturn(testUrl)

    when(mockHttpClient.get(url"$testUrl/etmp/registration/$testZref"))
      .thenReturn(mockRequestBuilder)
  }

  "EtmpConnector.getRegistrationDetails" should {

    "return Right(RegistrationDetails) when the call succeeds" in new TestSetup {
      when(mockRequestBuilder.execute[Either[UpstreamErrorResponse, RegistrationDetails]](any(), any()))
        .thenReturn(Future.successful(Right(testJourneyData)))

      val result: Either[UpstreamErrorResponse, RegistrationDetails] =
        connector.getRegistrationDetails(testZref).futureValue

      result mustBe Right(testJourneyData)
    }

    "surface isaProducts.underReview as the top-level isaProductsChangeUnderReview flag" in new TestSetup {
      val etmpResponse: RegistrationDetails = testJourneyData.copy(
        isaProducts = Some(
          IsaProducts(
            isaProducts = None,
            innovativeFinancialProducts = None,
            p2pPlatform = None,
            p2pPlatformNumber = None,
            underReview = true
          )
        )
      )

      when(mockRequestBuilder.execute[Either[UpstreamErrorResponse, RegistrationDetails]](any(), any()))
        .thenReturn(Future.successful(Right(etmpResponse)))

      val result: Either[UpstreamErrorResponse, RegistrationDetails] =
        connector.getRegistrationDetails(testZref).futureValue

      result.map(_.isaProductsChangeUnderReview) mustBe Right(true)
    }

    "return Left(UpstreamErrorResponse) when ETMP returns a 404" in new TestSetup {
      val notFound: UpstreamErrorResponse = UpstreamErrorResponse(
        message = "Not found",
        statusCode = NOT_FOUND,
        reportAs = NOT_FOUND,
        headers = Map.empty
      )

      when(mockRequestBuilder.execute[Either[UpstreamErrorResponse, RegistrationDetails]](any(), any()))
        .thenReturn(Future.successful(Left(notFound)))

      val result: Either[UpstreamErrorResponse, RegistrationDetails] =
        connector.getRegistrationDetails(testZref).futureValue

      result mustBe Left(notFound)
    }

    "propagate Throwable when the call fails with an unexpected exception" in new TestSetup {
      val ex = new RuntimeException("Connection timeout")

      when(mockRequestBuilder.execute[Either[UpstreamErrorResponse, RegistrationDetails]](any(), any()))
        .thenReturn(Future.failed(ex))

      val thrown: Throwable = connector.getRegistrationDetails(testZref).failed.futureValue

      thrown mustBe ex
    }
  }

  "EtmpConnector.updateRegistrationDetails" should {
    "send the typed request and return Right when the call succeeds" in new TestSetup {
      val details = UpdateRegistrationDetailsRequest(tradingName = Some("Updated name"))
      when(mockHttpClient.put(url"$testUrl/etmp/registration/$testZref")).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(Json.toJson(details))).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](any(), any()))
        .thenReturn(Future.successful(Right(HttpResponse(OK, ""))))

      connector.updateRegistrationDetails(testZref, details).futureValue mustBe Right(())

      verify(mockRequestBuilder).withBody(Json.toJson(details))
    }

    "return Left when ETMP cannot find the registration" in new TestSetup {
      val details  = UpdateRegistrationDetailsRequest()
      val notFound = UpstreamErrorResponse("Not found", NOT_FOUND, NOT_FOUND, Map.empty)
      when(mockHttpClient.put(url"$testUrl/etmp/registration/$testZref")).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(Json.toJson(details))).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](any(), any()))
        .thenReturn(Future.successful(Left(notFound)))

      connector.updateRegistrationDetails(testZref, details).futureValue mustBe Left(notFound)
    }
  }
}
