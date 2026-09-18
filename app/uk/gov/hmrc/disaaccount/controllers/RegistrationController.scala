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

import play.api.Logging
import play.api.http.Status.NOT_FOUND
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.disaaccount.connectors.EtmpConnector
import uk.gov.hmrc.disaaccount.models.registrationDetails.UpdateRegistrationDetailsRequest
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.ExecutionContext
import scala.util.control.NonFatal

class RegistrationController @Inject() (
  cc: ControllerComponents,
  etmpConnector: EtmpConnector,
  val authConnector: AuthConnector
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with AuthorisedFunctions
    with Logging {

  def retrieveRegistrationDetails(zref: String): Action[AnyContent] = Action.async { implicit request =>
    authorised() {
      etmpConnector
        .getRegistrationDetails(zref)
        .map {
          case Right(registrationDetails)               =>
            Ok(Json.toJson(registrationDetails))
          case Left(err) if err.statusCode == NOT_FOUND =>
            logger.info(
              s"[RegistrationController][retrieveRegistrationDetails] No registration details found in ETMP for zref: [$zref]"
            )
            NotFound
          case Left(err)                                =>
            logger.error(
              s"[RegistrationController][retrieveRegistrationDetails] Unexpected error retrieving registration details from ETMP for zref: [$zref], status: [${err.statusCode}]"
            )
            InternalServerError
        }
        .recover { case NonFatal(e) =>
          logger.error(
            s"[RegistrationController][retrieveRegistrationDetails] Unexpected error retrieving registration details for zref: [$zref]",
            e
          )
          InternalServerError
        }
    }
  }

  def updateRegistrationDetails(zref: String): Action[UpdateRegistrationDetailsRequest] =
    Action.async(parse.json[UpdateRegistrationDetailsRequest]) { implicit request =>
      authorised() {
        etmpConnector
          .updateRegistrationDetails(zref, request.body)
          .map {
            case Right(_)                                 => Ok
            case Left(err) if err.statusCode == NOT_FOUND => NotFound
            case Left(_)                                  => InternalServerError
          }
          .recover { case NonFatal(e) =>
            logger.error(
              s"[RegistrationController][updateRegistrationDetails] Unexpected error updating registration details for zref: [$zref]",
              e
            )
            InternalServerError
          }
      }
    }
}
