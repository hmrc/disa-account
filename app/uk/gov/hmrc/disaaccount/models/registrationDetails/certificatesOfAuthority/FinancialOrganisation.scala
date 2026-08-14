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

package uk.gov.hmrc.disaaccount.models.registrationDetails.certificatesofauthority

import uk.gov.hmrc.disaaccount.models.{Enumerable, WithName}

sealed trait FinancialOrganisation

object FinancialOrganisation extends Enumerable.Implicits {

  case object EuropeanInstitution extends WithName("europeanInstitution") with FinancialOrganisation
  case object CreditUnion extends WithName("creditUnion") with FinancialOrganisation
  case object DirectorSavings extends WithName("directorSavings") with FinancialOrganisation
  case object BuildingSociety extends WithName("buildingSociety") with FinancialOrganisation
  case object Bank extends WithName("bank") with FinancialOrganisation
  case object InsuranceCompany extends WithName("insuranceCompany") with FinancialOrganisation
  case object IncorporatedFriendlySociety extends WithName("incorporatedFriendlySociety") with FinancialOrganisation
  case object RegisteredFriendlySociety extends WithName("registeredFriendlySociety") with FinancialOrganisation
  case object AssuranceUndertaking extends WithName("assuranceUndertaking") with FinancialOrganisation

  val values: Seq[FinancialOrganisation] = Seq(
    EuropeanInstitution,
    CreditUnion,
    DirectorSavings,
    BuildingSociety,
    Bank,
    InsuranceCompany,
    IncorporatedFriendlySociety,
    RegisteredFriendlySociety,
    AssuranceUndertaking
  )

  implicit val enumerable: Enumerable[FinancialOrganisation] =
    Enumerable(values.map(v => v.toString -> v): _*)
}
