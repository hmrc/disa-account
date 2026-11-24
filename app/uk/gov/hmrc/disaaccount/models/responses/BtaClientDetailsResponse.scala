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

package uk.gov.hmrc.disaaccount.models.responses

import java.time.Instant
import play.api.libs.json.{Json, OWrites, Writes}
import uk.gov.hmrc.disaaccount.models.registrationDetails.*
import uk.gov.hmrc.disaaccount.models.registrationDetails.orgdetails.OrganisationDetails
import uk.gov.hmrc.disaaccount.models.registrationDetails.isaProducts.IsaProducts
import uk.gov.hmrc.disaaccount.models.registrationDetails.certificatesOfAuthority.CertificatesOfAuthority
import uk.gov.hmrc.disaaccount.models.registrationDetails.liaisonofficers.{LiaisonOfficer, LiaisonOfficers}
import uk.gov.hmrc.disaaccount.models.registrationDetails.signatories.{Signatories, Signatory}
import uk.gov.hmrc.disaaccount.models.registrationDetails.thirdparty.{ThirdParty, ThirdPartyOrganisations}
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

case class BusinessVerificationResponse(
  ctUtr: Option[String],
  companyName: Option[String],
  companyNumber: Option[String],
  businessPartnerId: Option[String]
)

object BusinessVerificationResponse {
  implicit val writes: OWrites[BusinessVerificationResponse] = Json.writes[BusinessVerificationResponse]

  def from(details: BusinessVerification): BusinessVerificationResponse =
    BusinessVerificationResponse(
      ctUtr = details.ctUtr,
      companyName = details.companyName,
      companyNumber = details.companyNumber,
      businessPartnerId = details.businessPartnerId
    )
}

case class CorrespondenceAddressResponse(
  addressLine1: Option[String],
  addressLine2: Option[String],
  addressLine3: Option[String],
  postCode: Option[String]
)

object CorrespondenceAddressResponse {
  implicit val writes: OWrites[CorrespondenceAddressResponse] = Json.writes[CorrespondenceAddressResponse]

  def from(details: CorrespondenceAddress): CorrespondenceAddressResponse =
    CorrespondenceAddressResponse(
      addressLine1 = details.addressLine1,
      addressLine2 = details.addressLine2,
      addressLine3 = details.addressLine3,
      postCode = details.postCode
    )
}

case class OrganisationDetailsResponse(
  zRefNumber: Option[String],
  tradingName: Option[String],
  fcaNumber: Option[String],
  correspondenceAddress: Option[CorrespondenceAddressResponse],
  orgTelephoneNumber: Option[String]
)

object OrganisationDetailsResponse {
  implicit val writes: OWrites[OrganisationDetailsResponse] = Json.writes[OrganisationDetailsResponse]

  def from(details: OrganisationDetails): OrganisationDetailsResponse =
    OrganisationDetailsResponse(
      zRefNumber = details.zRefNumber,
      tradingName = details.tradingName,
      fcaNumber = details.fcaNumber,
      correspondenceAddress = details.correspondenceAddress.map(CorrespondenceAddressResponse.from),
      orgTelephoneNumber = details.orgTelephoneNumber
    )
}

case class OrganisationEmailResponse(
  organisationEmail: Option[String],
  verified: Option[Boolean]
)

object OrganisationEmailResponse {
  implicit val writes: OWrites[OrganisationEmailResponse] = Json.writes[OrganisationEmailResponse]

  def from(details: OrganisationEmail): OrganisationEmailResponse =
    OrganisationEmailResponse(
      organisationEmail = details.organisationEmail,
      verified = details.verified
    )
}

case class IsaProductsResponse(
  isaProducts: Option[Seq[String]],
  innovativeFinancialProducts: Option[Seq[String]],
  p2pPlatform: Option[String],
  p2pPlatformNumber: Option[String],
  underReview: Boolean
)

object IsaProductsResponse {
  implicit val writes: OWrites[IsaProductsResponse] = Json.writes[IsaProductsResponse]

  def from(details: IsaProducts): IsaProductsResponse =
    IsaProductsResponse(
      isaProducts = details.isaProducts.map(_.map(_.toString)),
      innovativeFinancialProducts = details.innovativeFinancialProducts.map(_.map(_.toString)),
      p2pPlatform = details.p2pPlatform,
      p2pPlatformNumber = details.p2pPlatformNumber,
      underReview = details.underReview
    )
}

case class CertificatesOfAuthorityResponse(
  certificatesYesNo: Option[String],
  fcaArticles: Option[Seq[String]],
  financialOrganisation: Option[Seq[String]]
)

object CertificatesOfAuthorityResponse {
  implicit val writes: OWrites[CertificatesOfAuthorityResponse] = Json.writes[CertificatesOfAuthorityResponse]

  def from(details: CertificatesOfAuthority): CertificatesOfAuthorityResponse =
    CertificatesOfAuthorityResponse(
      certificatesYesNo = details.certificatesYesNo.map(_.toString),
      fcaArticles = details.fcaArticles.map(_.map(_.toString)),
      financialOrganisation = details.financialOrganisation.map(_.map(_.toString))
    )
}

case class LiaisonOfficerResponse(
  id: String,
  fullName: Option[String],
  phoneNumber: Option[String],
  communication: Set[String],
  email: Option[String]
)

object LiaisonOfficerResponse {
  implicit val writes: OWrites[LiaisonOfficerResponse] = Json.writes[LiaisonOfficerResponse]

  def from(details: LiaisonOfficer): LiaisonOfficerResponse =
    LiaisonOfficerResponse(
      id = details.id,
      fullName = details.fullName,
      phoneNumber = details.phoneNumber,
      communication = details.communication.map(_.toString),
      email = details.email
    )
}

case class LiaisonOfficersResponse(
  liaisonOfficers: Seq[LiaisonOfficerResponse]
)

object LiaisonOfficersResponse {
  implicit val writes: OWrites[LiaisonOfficersResponse] = Json.writes[LiaisonOfficersResponse]

  def from(details: LiaisonOfficers): LiaisonOfficersResponse =
    LiaisonOfficersResponse(
      liaisonOfficers = details.liaisonOfficers.map(LiaisonOfficerResponse.from)
    )
}

case class SignatoryResponse(
  id: String,
  fullName: Option[String],
  jobTitle: Option[String],
  email: Option[String]
)

object SignatoryResponse {
  implicit val writes: OWrites[SignatoryResponse] = Json.writes[SignatoryResponse]

  def from(details: Signatory): SignatoryResponse =
    SignatoryResponse(
      id = details.id,
      fullName = details.fullName,
      jobTitle = details.jobTitle,
      email = details.email
    )
}

case class SignatoriesResponse(
  signatories: Seq[SignatoryResponse]
)

object SignatoriesResponse {
  implicit val writes: OWrites[SignatoriesResponse] = Json.writes[SignatoriesResponse]

  def from(details: Signatories): SignatoriesResponse =
    SignatoriesResponse(
      signatories = details.signatories.map(SignatoryResponse.from)
    )
}

case class ThirdPartyResponse(
  id: String,
  thirdPartyName: Option[String],
  thirdPartyFrn: Option[String],
  managingIsaReturns: Option[String],
  usingInvestorFunds: Option[String],
  investorFundsPercentage: Option[String]
)

object ThirdPartyResponse {
  implicit val writes: OWrites[ThirdPartyResponse] = Json.writes[ThirdPartyResponse]

  def from(details: ThirdParty): ThirdPartyResponse =
    ThirdPartyResponse(
      id = details.id,
      thirdPartyName = details.thirdPartyName,
      thirdPartyFrn = details.thirdPartyFrn,
      managingIsaReturns = details.managingIsaReturns.map(_.toString),
      usingInvestorFunds = details.usingInvestorFunds.map(_.toString),
      investorFundsPercentage = details.investorFundsPercentage
    )
}

case class ThirdPartyOrganisationsResponse(
  managedByThirdParty: Option[String],
  thirdParties: Seq[ThirdPartyResponse],
  connectedOrganisations: Seq[String]
)

object ThirdPartyOrganisationsResponse {
  implicit val writes: OWrites[ThirdPartyOrganisationsResponse] = Json.writes[ThirdPartyOrganisationsResponse]

  def from(details: ThirdPartyOrganisations): ThirdPartyOrganisationsResponse =
    ThirdPartyOrganisationsResponse(
      managedByThirdParty = details.managedByThirdParty.map(_.toString),
      thirdParties = details.thirdParties.map(ThirdPartyResponse.from),
      connectedOrganisations = details.connectedOrganisations
    )
}

case class BtaClientDetailsResponse(
  groupId: String,
  enrolmentId: String,
  businessVerification: Option[BusinessVerificationResponse],
  organisationDetails: Option[OrganisationDetailsResponse],
  organisationEmail: Option[OrganisationEmailResponse],
  isaProducts: Option[IsaProductsResponse],
  certificatesOfAuthority: Option[CertificatesOfAuthorityResponse],
  liaisonOfficers: Option[LiaisonOfficersResponse],
  signatories: Option[SignatoriesResponse],
  thirdPartyOrganisations: Option[ThirdPartyOrganisationsResponse],
  lastUpdated: Option[Instant],
  isaProductsChangeUnderReview: Boolean
)

object BtaClientDetailsResponse {
  implicit val instantWrites: Writes[Instant]            = MongoJavatimeFormats.instantWrites
  implicit val writes: OWrites[BtaClientDetailsResponse] = Json.writes[BtaClientDetailsResponse]

  def from(details: RegistrationDetails): BtaClientDetailsResponse =
    BtaClientDetailsResponse(
      groupId = details.groupId,
      enrolmentId = details.enrolmentId,
      businessVerification = details.businessVerification.map(BusinessVerificationResponse.from),
      organisationDetails = details.organisationDetails.map(OrganisationDetailsResponse.from),
      organisationEmail = details.organisationEmail.map(OrganisationEmailResponse.from),
      isaProducts = details.isaProducts.map(IsaProductsResponse.from),
      certificatesOfAuthority = details.certificatesOfAuthority.map(CertificatesOfAuthorityResponse.from),
      liaisonOfficers = details.liaisonOfficers.map(LiaisonOfficersResponse.from),
      signatories = details.signatories.map(SignatoriesResponse.from),
      thirdPartyOrganisations = details.thirdPartyOrganisations.map(ThirdPartyOrganisationsResponse.from),
      lastUpdated = details.lastUpdated,
      isaProductsChangeUnderReview = details.isaProductsChangeUnderReview
    )
}
