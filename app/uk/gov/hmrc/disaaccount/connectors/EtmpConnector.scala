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

import com.typesafe.config.Config
import org.apache.pekko.actor.ActorSystem
import uk.gov.hmrc.disaaccount.config.AppConfig
import uk.gov.hmrc.disaaccount.models.registrationDetails.RegistrationDetails
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, Retries, StringContextOps, UpstreamErrorResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EtmpConnector @Inject() (
  http: HttpClientV2,
  appConfig: AppConfig,
  protected val configuration: Config,
  protected val actorSystem: ActorSystem
)(implicit ec: ExecutionContext)
    extends Retries {

  private val retryCondition: PartialFunction[Exception, Boolean] = {
    case UpstreamErrorResponse.Upstream5xxResponse(_) => true
  }

  def getRegistrationDetails(
    zref: String
  )(implicit hc: HeaderCarrier): Future[Either[UpstreamErrorResponse, RegistrationDetails]] = {
    val url = s"${appConfig.etmpBaseUrl}/etmp/registration/$zref"
    retryFor[RegistrationDetails]("get ETMP registration details")(retryCondition) {
      http
        .get(url"$url")
        .execute[Either[UpstreamErrorResponse, RegistrationDetails]]
        .flatMap {
          case Right(details) => Future.successful(details)
          case Left(error)    => Future.failed(error)
        }
    }.map(details => Right(withIsaProductsChangeUnderReview(details)))
      .recover { case error: UpstreamErrorResponse => Left(error) }
  }

  // ETMP flags a change under review on ISA product this surfaces it as a top-level flag for callers.
  private def withIsaProductsChangeUnderReview(details: RegistrationDetails): RegistrationDetails =
    details.copy(isaProductsChangeUnderReview = details.isaProducts.exists(_.underReview))
}
