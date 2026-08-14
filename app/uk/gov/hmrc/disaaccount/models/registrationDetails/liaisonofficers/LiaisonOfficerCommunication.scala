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

package uk.gov.hmrc.disaaccount.models.registrationDetails.liaisonofficers

import uk.gov.hmrc.disaaccount.models.{Enumerable, WithName}

sealed trait LiaisonOfficerCommunication

object LiaisonOfficerCommunication extends Enumerable.Implicits {

  case object ByEmail extends WithName("byEmail") with LiaisonOfficerCommunication
  case object ByPhone extends WithName("byPhone") with LiaisonOfficerCommunication
  case object ByPost extends WithName("byPost") with LiaisonOfficerCommunication

  val values: Seq[LiaisonOfficerCommunication] = Seq(
    ByEmail,
    ByPhone,
    ByPost
  )

  implicit val enumerable: Enumerable[LiaisonOfficerCommunication] =
    Enumerable(values.map(v => v.toString -> v): _*)
}
