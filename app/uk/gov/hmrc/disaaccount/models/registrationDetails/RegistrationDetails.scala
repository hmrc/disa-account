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

package uk.gov.hmrc.disaaccount.models.registrationDetails

import play.api.libs.json.{Format, Json, Reads, Writes}
import uk.gov.hmrc.disaaccount.models.registrationDetails.certificatesofauthority.CertificatesOfAuthority
import uk.gov.hmrc.disaaccount.models.registrationDetails.isaProducts.IsaProducts
import uk.gov.hmrc.disaaccount.models.registrationDetails.liaisonofficers.LiaisonOfficers
import uk.gov.hmrc.disaaccount.models.registrationDetails.orgdetails.OrganisationDetails
import uk.gov.hmrc.disaaccount.models.registrationDetails.signatories.Signatories
import uk.gov.hmrc.disaaccount.models.registrationDetails.thirdparty.ThirdPartyOrganisations
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.Instant
import java.util.UUID

case class RegistrationDetails(
  groupId: String,
  enrolmentId: String = UUID.randomUUID().toString,
  businessVerification: Option[BusinessVerification] = None,
  organisationDetails: Option[OrganisationDetails] = None,
  organisationEmail: Option[OrganisationEmail] = None,
  isaProducts: Option[IsaProducts] = None,
  certificatesOfAuthority: Option[CertificatesOfAuthority] = None,
  liaisonOfficers: Option[LiaisonOfficers] = None,
  signatories: Option[Signatories] = None,
  thirdPartyOrganisations: Option[ThirdPartyOrganisations] = None,
  lastUpdated: Option[Instant] = None
)

object RegistrationDetails {
  implicit val instantFormat: Format[Instant] =
    Format(MongoJavatimeFormats.instantReads, MongoJavatimeFormats.instantWrites)

  implicit val format: Format[RegistrationDetails] = Json.format[RegistrationDetails]

  case class TaskListJourney[A](reads: Reads[A], writes: Writes[A])

  val taskListJourneyHandlers: Map[String, TaskListJourney[_]] = Map(
    "businessVerification"    -> TaskListJourney(BusinessVerification.format, BusinessVerification.format),
    "organisationDetails"     -> TaskListJourney(OrganisationDetails.format, OrganisationDetails.format),
    "organisationEmail"       -> TaskListJourney(OrganisationEmail.format, OrganisationEmail.format),
    "isaProducts"             -> TaskListJourney(IsaProducts.format, IsaProducts.format),
    "certificatesOfAuthority" -> TaskListJourney(CertificatesOfAuthority.format, CertificatesOfAuthority.format),
    "liaisonOfficers"         -> TaskListJourney(LiaisonOfficers.format, LiaisonOfficers.format),
    "signatories"             -> TaskListJourney(Signatories.format, Signatories.format),
    "thirdPartyOrganisations" -> TaskListJourney(ThirdPartyOrganisations.format, ThirdPartyOrganisations.format)
  )
}
