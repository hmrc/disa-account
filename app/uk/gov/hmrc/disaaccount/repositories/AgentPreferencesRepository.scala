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

package uk.gov.hmrc.disaaccount.repositories

import org.mongodb.scala.model.*
import uk.gov.hmrc.disaaccount.config.AppConfig
import uk.gov.hmrc.disaaccount.models.StoredAgentPreferences
import uk.gov.hmrc.disaaccount.utils.Constants.*
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import java.time.{Clock, Instant}
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AgentPreferencesRepository @Inject() (mongoComponent: MongoComponent, clock: Clock, appConfig: AppConfig)(implicit
  ec: ExecutionContext
) extends PlayMongoRepository[StoredAgentPreferences](
      mongoComponent = mongoComponent,
      collectionName = "agent-preferences",
      domainFormat = StoredAgentPreferences.format,
      indexes = Seq(
        IndexModel(
          Indexes.ascending(mongoExpiresAtField),
          IndexOptions().name(agentPreferencesTtlIndex).expireAfter(0, TimeUnit.SECONDS)
        )
      ),
      replaceIndexes = true
    ) {

  def get(sessionId: String): Future[Option[StoredAgentPreferences]] =
    collection
      .find(Filters.equal(mongoIdField, sessionId))
      .headOption()
      .map(_.filter(_.expiresAt.isAfter(Instant.now(clock))))

  def set(sessionId: String, zReference: String): Future[Boolean] =
    collection
      .updateOne(
        Filters.equal(mongoIdField, sessionId),
        Updates.combine(
          Updates.set("zReference", zReference),
          Updates.setOnInsert(mongoExpiresAtField, Instant.now(clock).plus(appConfig.agentPreferencesTtl))
        ),
        UpdateOptions().upsert(true)
      )
      .toFuture()
      .map(_.wasAcknowledged())
}
