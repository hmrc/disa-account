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

package models.responses

import java.time.Instant
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.disaaccount.models.YesNoAnswer
import uk.gov.hmrc.disaaccount.models.registrationDetails.*
import uk.gov.hmrc.disaaccount.models.registrationDetails.certificatesOfAuthority.*
import uk.gov.hmrc.disaaccount.models.registrationDetails.isaProducts.*
import uk.gov.hmrc.disaaccount.models.registrationDetails.liaisonofficers.*
import uk.gov.hmrc.disaaccount.models.registrationDetails.signatories.*
import uk.gov.hmrc.disaaccount.models.registrationDetails.thirdparty.*
import uk.gov.hmrc.disaaccount.models.responses.BtaClientDetailsResponse
import utils.UnitSpecBase

class BtaClientDetailsResponseSpec extends UnitSpecBase {
  "BtaClientDetailsResponse" should {
    "preserve all registration fields, nested collections, enum values and timestamp representation" in {
      val details  = testJourneyData.copy(
        organisationDetails = Some(
          testOrganisationDetails.copy(
            correspondenceAddress =
              Some(CorrespondenceAddress(Some("1 Test Street"), Some("Town"), Some("County"), Some("AA1 1AA"))),
            orgTelephoneNumber = Some("01234567890")
          )
        ),
        organisationEmail = Some(OrganisationEmail(Some("org@example.com"), Some(true))),
        isaProducts = Some(
          IsaProducts(
            Some(IsaProduct.values),
            Some(InnovativeFinancialProduct.values),
            Some("Platform"),
            Some("001234"),
            true
          )
        ),
        certificatesOfAuthority = Some(
          CertificatesOfAuthority(
            Some(CertificatesOfAuthorityYesNo.Yes),
            Some(FcaArticles.values),
            Some(FinancialOrganisation.values)
          )
        ),
        liaisonOfficers = Some(
          LiaisonOfficers(
            Seq(
              LiaisonOfficer(
                "officer",
                Some("Officer"),
                Some("01234567890"),
                Set(LiaisonOfficerCommunication.ByEmail),
                Some("officer@example.com")
              )
            )
          )
        ),
        signatories = Some(
          Signatories(Seq(Signatory("signatory", Some("Signatory"), Some("Director"), Some("signatory@example.com"))))
        ),
        thirdPartyOrganisations = Some(
          ThirdPartyOrganisations(
            Some(YesNoAnswer.Yes),
            Seq(
              ThirdParty(
                "third-party",
                Some("Third Party"),
                Some("001234"),
                Some(YesNoAnswer.Yes),
                Some(YesNoAnswer.No),
                Some("25")
              )
            ),
            Seq("Connected organisation")
          )
        ),
        lastUpdated = Some(Instant.parse("2026-11-24T10:00:00.123Z")),
        isaProductsChangeUnderReview = true
      )
      val response = Json.toJson(BtaClientDetailsResponse.from(details))
      response                                                                  shouldBe Json.toJson(details)
      (response \ "isaProductsChangeUnderReview").as[Boolean]                   shouldBe true
      (response \ "isaProducts" \ "isaProducts").as[Seq[String]]                shouldBe IsaProduct.values.map(_.toString)
      (response \ "thirdPartyOrganisations" \ "managedByThirdParty").as[String] shouldBe "yes"
    }

    "preserve absent optional sections without introducing fields or requiring client known facts" in {
      val details = RegistrationDetails("group", "enrolment")
      Json.toJson(BtaClientDetailsResponse.from(details)) shouldBe Json.obj(
        "groupId"                      -> "group",
        "enrolmentId"                  -> "enrolment",
        "isaProductsChangeUnderReview" -> false
      )
    }

    "preserve empty collections and sparse nested sections" in {
      val details = testJourneyData.copy(
        businessVerification = Some(BusinessVerification(None, None, None, None)),
        organisationDetails = Some(testOrganisationDetails.copy(fcaNumber = None)),
        isaProducts = Some(IsaProducts(Some(Seq.empty), None, None, None)),
        certificatesOfAuthority = Some(CertificatesOfAuthority(Some(CertificatesOfAuthorityYesNo.No), None, None)),
        liaisonOfficers = Some(LiaisonOfficers(Seq.empty)),
        signatories = Some(Signatories()),
        thirdPartyOrganisations = Some(ThirdPartyOrganisations(Some(YesNoAnswer.No)))
      )
      Json.toJson(BtaClientDetailsResponse.from(details)) shouldBe Json.toJson(details)
    }
  }
}
