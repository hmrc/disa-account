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
import org.mockito.Mockito.when
import org.scalatest.matchers.must.Matchers.mustBe
import uk.gov.hmrc.disaaccount.models.registrationDetails.RegistrationDetails
import uk.gov.hmrc.http.{StringContextOps, UpstreamErrorResponse}
import utils.BaseUnitSpec

import scala.concurrent.Future

class EtmpConnectorSpec extends BaseUnitSpec {

  trait TestSetup {
    val connector: EtmpConnector = new EtmpConnector(mockHttpClient, mockAppConfig)

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

    "return Left(UpstreamErrorResponse) when ETMP returns a 404" in new TestSetup {
      val notFound: UpstreamErrorResponse = UpstreamErrorResponse(
        message = "Not found",
        statusCode = 404,
        reportAs = 404,
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
}
