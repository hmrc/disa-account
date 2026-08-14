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

package utils

import uk.gov.hmrc.disaaccount.models.registrationDetails.orgdetails.OrganisationDetails
import uk.gov.hmrc.disaaccount.models.registrationDetails.{BusinessVerification, RegistrationDetails}

import java.util.UUID

trait TestData {
  val testZref: String    = "Z1234"
  val testGroupId: String = UUID.randomUUID().toString

  val testOrganisationDetails: OrganisationDetails =
    OrganisationDetails(
      zRefNumber = Some(testZref),
      tradingName = Some("Test Trading Name"),
      fcaNumber = Some("123456")
    )

  val testBusinessVerification: BusinessVerification = BusinessVerification(
    ctUtr = Some("1234567890"),
    companyName = Some("Test Isa Manager Ltd"),
    companyNumber = Some("12345678"),
    businessPartnerId = Some("XA0001234567890")
  )

  val testJourneyData: RegistrationDetails = RegistrationDetails(
    groupId = testGroupId,
    businessVerification = Some(testBusinessVerification),
    organisationDetails = Some(testOrganisationDetails)
  )
}
