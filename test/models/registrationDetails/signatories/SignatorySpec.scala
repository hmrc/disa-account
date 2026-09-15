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

package models.registrationDetails.signatories

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json
import uk.gov.hmrc.disaaccount.models.registrationDetails.signatories.Signatory

class SignatorySpec extends AnyWordSpec with Matchers {

  "Signatory" should {

    "read and write an email address" in {
      val signatory = Signatory("1", Some("Jane Smith"), Some("Director"), Some("jane.smith@example.com"))

      Json.toJson(signatory).as[Signatory]          shouldBe signatory
      (Json.toJson(signatory) \ "email").as[String] shouldBe "jane.smith@example.com"
    }

    "read a payload without an email address" in {
      Json.obj("id" -> "1").as[Signatory] shouldBe Signatory("1")
    }
  }
}
