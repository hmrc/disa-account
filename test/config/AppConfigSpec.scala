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

package config

import uk.gov.hmrc.disaaccount.config.AppConfig
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import utils.UnitSpecBase

import java.time.Duration

class AppConfigSpec extends UnitSpecBase {
  "AppConfig" should {
    "use a four-hour default for agent preferences" in {
      new AppConfig(app.configuration, mock[ServicesConfig]).agentPreferencesTtl shouldBe Duration.ofHours(4)
    }
  }
}
