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

package uk.gov.hmrc.disaaccount.controllers.actions

import play.api.Logging
import play.api.libs.json.Json
import play.api.mvc.*
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.EmptyRetrieval
import uk.gov.hmrc.disaaccount.models.ErrorResponse
import uk.gov.hmrc.disaaccount.utils.Constants.*
import uk.gov.hmrc.disaaccount.validators.ZReferenceValidator
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class BtaAuthAction @Inject() (authConnector: AuthConnector, bodyParser: BodyParsers.Default)(implicit
  ec: ExecutionContext
) extends Logging {

  def apply(zReference: String): ActionBuilder[Request, AnyContent] = new ActionBuilder[Request, AnyContent] {
    override val parser: BodyParser[AnyContent]               = bodyParser
    override protected val executionContext: ExecutionContext = ec

    override def invokeBlock[A](request: Request[A], block: Request[A] => Future[Result]): Future[Result] = {
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequest(request)

      authorise(AffinityGroup.Organisation, enrolmentCheck = false).flatMap {
        case Left(result)                                         => Future.successful(result)
        case Right(_) if !ZReferenceValidator.isValid(zReference) =>
          Future.successful(
            Results.BadRequest(Json.toJson(ErrorResponse(invalidZReferenceFormatCode, invalidZReferenceFormatMessage)))
          )
        case Right(_)                                             =>
          authorise(Enrolment(disaEnrolment).withIdentifier(zReferenceIdentifier, zReference), enrolmentCheck = true)
            .flatMap {
              case Left(result) => Future.successful(result)
              case Right(_)     => block(request)
            }
      }
    }
  }

  private def authorise(predicate: Predicate, enrolmentCheck: Boolean)(implicit
    hc: HeaderCarrier
  ): Future[Either[Result, Unit]] =
    authConnector.authorise(predicate, EmptyRetrieval).map[Either[Result, Unit]](_ => Right(())).recover {
      case _: InsufficientEnrolments if enrolmentCheck =>
        Left(Results.Forbidden(Json.toJson(ErrorResponse(disaEnrolmentNotFoundCode, disaEnrolmentNotFoundMessage))))
      case _: UnsupportedAffinityGroup                 =>
        Left(
          Results.Unauthorized(Json.toJson(ErrorResponse(userIsNotAnOrganisationCode, userIsNotAnOrganisationMessage)))
        )
      case _: NoActiveSession                          =>
        Left(Results.Unauthorized(Json.toJson(ErrorResponse(unauthorizedCode, unauthorizedMessage))))
      case NonFatal(error)                             =>
        logger.error(s"[BtaAuthAction] Auth failed; exception: ${error.getClass.getSimpleName}")
        Left(
          Results.InternalServerError(Json.toJson(ErrorResponse(internalServerErrorCode, internalServerErrorMessage)))
        )
    }
}
