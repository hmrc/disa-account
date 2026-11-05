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

package uk.gov.hmrc.disaaccount

import org.apache.pekko.Done
import play.api.Logger
import uk.gov.hmrc.disaaccount.config.{InternalAuthTokenInitialiser, NoOpInternalAuthTokenInitialiser}
import uk.gov.hmrc.play.bootstrap.tools.LogCapturing
import utils.BaseUnitSpec
import scala.concurrent.Future

class AppInitialiserSpec extends BaseUnitSpec with LogCapturing {
  private def initialiser(result: Future[Done]) = new InternalAuthTokenInitialiser {
    override protected def initialise(): Future[Done] = result
  }

  "AppInitialiser" should {
    "complete eagerly using the no-op binding without contacting internal-auth" in {
      app.injector.instanceOf[InternalAuthTokenInitialiser]           shouldBe a[NoOpInternalAuthTokenInitialiser]
      app.injector.instanceOf[AppInitialiser].initialised.futureValue shouldBe Done
    }
    "log successful initialisation" in
      withCaptureOfLoggingFrom(Logger(classOf[AppInitialiser])) { events =>
        new AppInitialiser(initialiser(Future.successful(Done))).initialised.futureValue shouldBe Done
        events.exists(_.getMessage.contains("initialiser completed"))                    shouldBe true
      }
    "log and propagate initialisation failure" in {
      val error = new RuntimeException("auth unavailable")
      withCaptureOfLoggingFrom(Logger(classOf[AppInitialiser])) { events =>
        intercept[RuntimeException](new AppInitialiser(initialiser(Future.failed(error)))) shouldBe error
        events.exists(_.getMessage.contains("initialiser failed"))                         shouldBe true
      }
    }
  }
}
