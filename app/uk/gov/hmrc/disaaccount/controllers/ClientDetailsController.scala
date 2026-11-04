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
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.disaaccount.connectors.EtmpConnector
import uk.gov.hmrc.disaaccount.models.ClientDetails
import uk.gov.hmrc.disaaccount.validators.ZReferenceValidator
import uk.gov.hmrc.internalauth.client.*
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class ClientDetailsController @Inject() (
  cc: ControllerComponents,
  etmpConnector: EtmpConnector,
  auth: BackendAuthComponents
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  def get(zref: String): Action[AnyContent] = auth.authorizedAction(readPermission).async { implicit request =>
    if (!ZReferenceValidator.isValid(zref)) Future.successful(BadRequest)
    else
      etmpConnector
        .getRegistrationDetails(zref)
        .map {
          case Right(details)                               =>
            val name = details.businessVerification.flatMap(_.companyName).filter(_.trim.nonEmpty)
            val fca  = details.organisationDetails.flatMap(_.fcaNumber).filter(_.trim.nonEmpty)
            (name, fca) match {
              case (Some(businessName), Some(fcaNumber)) => Ok(Json.toJson(ClientDetails(businessName, fcaNumber)))
              case _                                     =>
                val missing = Seq("businessName" -> name, "fcaNumber" -> fca).collect { case (field, None) => field }
                logger.error(
                  s"[ClientDetailsController][get] Incomplete ETMP client details; missing fields: ${missing.mkString(", ")}"
                )
                InternalServerError
            }
          case Left(error) if error.statusCode == NOT_FOUND => NotFound
          case Left(error)                                  =>
            logger.error(s"[ClientDetailsController][get] ETMP lookup failed; status: ${error.statusCode}")
            InternalServerError
        }
        .recover { case NonFatal(error) =>
          logger.error(s"[ClientDetailsController][get] ETMP lookup failed; exception: ${error.getClass.getSimpleName}")
          InternalServerError
        }
  }

  private val readPermission = Predicate.Permission(
    Resource(ResourceType("disa-account"), ResourceLocation("client-details")),
    IAAction("READ")
  )
}
