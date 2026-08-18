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

package uk.gov.hmrc.disaaccount.utils

import play.api.http.Status.{OK, UNAUTHORIZED}
import uk.gov.hmrc.disaaccount.utils.WiremockHelper.stubPost

trait CommonStubs {

  def stubAuth(): Unit = stubPost(url = "/auth/authorise", status = OK, responseBody = "{}")

  def stubAuthFail(): Unit = stubPost(url = "/auth/authorise", status = UNAUTHORIZED, responseBody = "{}")

  val testHeaders: Seq[(String, String)] = Seq("Authorization" -> "mock-bearer-token")

}
