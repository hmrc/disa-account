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

package uk.gov.hmrc.disaaccount.controllers

import com.github.tomakehurst.wiremock.client.WireMock.*
import org.mongodb.scala.{ObservableFuture, SingleObservableFuture}
import org.mongodb.scala.documentToUntypedDocument
import org.mongodb.scala.model.*
import play.api.Application
import play.api.http.Status.*
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.libs.ws.{WSResponse, writeableOf_String}
import play.api.test.Helpers.await
import uk.gov.hmrc.disaaccount.models.StoredAgentPreferences
import uk.gov.hmrc.disaaccount.repositories.AgentPreferencesRepository
import uk.gov.hmrc.disaaccount.utils.IntegrationSpecBase
import uk.gov.hmrc.disaaccount.utils.Constants.*
import uk.gov.hmrc.mongo.MongoComponent

import java.time.*
import java.time.temporal.ChronoUnit
import java.util.UUID

class AgentPreferencesControllerISpec extends IntegrationSpecBase {

  private lazy val now      = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MILLIS)
  private lazy val clock    = Clock.fixed(now, ZoneOffset.UTC)
  private lazy val mongo    = MongoComponent(s"mongodb://127.0.0.1:27017/disa-account-preferences-${UUID.randomUUID()}")
  private lazy val repo     = inject[AgentPreferencesRepository]
  private lazy val endpoint = s"http://localhost:$port/disa-account/agents/preferences"
  private val token         = "Bearer agent-token"
  private val secondToken   = "Bearer second-agent-token"
  private val sessionId     = "session-1"
  private val secondSession = "session-2"
  private val validBody     = s"""{"zReference":"$testZref"}"""

  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure(config + ("agent-preferences.ttl" -> "90 minutes"))
    .overrides(bind[MongoComponent].toInstance(mongo), bind[Clock].toInstance(clock))
    .build()

  override def beforeAll(): Unit = {
    super.beforeAll()
    await(repo.ensureIndexes())
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(repo.collection.deleteMany(Filters.empty()).toFuture())
  }

  override def afterAll(): Unit =
    try await(repo.collection.drop().toFuture())
    finally super.afterAll()

  private val agentAuthRequest =
    s"""{"authorise":[{"affinityGroup":"Agent"},{"enrolment":"$agentEnrolment","identifiers":[],"state":"Activated"}],"retrieve":["mdtpInformation"]}"""

  private def delegatedAuthRequest(reference: String) =
    s"""{"authorise":[{"enrolment":"$disaEnrolment","identifiers":[{"key":"$zReferenceIdentifier","value":"$reference"}],"state":"Activated","delegatedAuthRule":"$delegatedAuthRule"}],"retrieve":[]}"""

  private def stubAgent(
    bearer: String = token,
    session: Option[String] = Some(sessionId),
    failure: Option[String] = None
  ): Unit = {
    val response = failure match {
      case Some(reason) =>
        aResponse().withStatus(UNAUTHORIZED).withHeader("WWW-Authenticate", s"""MDTP detail="$reason"""")
      case None         =>
        val body = session.fold(Json.obj())(id =>
          Json.obj("mdtpInformation" -> Json.obj("deviceId" -> "device-1", "sessionId" -> id))
        )
        okJson(Json.stringify(body))
    }
    stubFor(
      post(urlEqualTo("/auth/authorise"))
        .withHeader("Authorization", equalTo(bearer))
        .withRequestBody(equalToJson(agentAuthRequest))
        .willReturn(response)
    )
  }

  private def stubDelegated(
    reference: String = testZref,
    bearer: String = token,
    failure: Option[String] = None
  ): Unit = {
    val response = failure match {
      case Some(reason) =>
        aResponse().withStatus(UNAUTHORIZED).withHeader("WWW-Authenticate", s"""MDTP detail="$reason"""")
      case None         => okJson("{}")
    }
    stubFor(
      post(urlEqualTo("/auth/authorise"))
        .withHeader("Authorization", equalTo(bearer))
        .withRequestBody(equalToJson(delegatedAuthRequest(reference)))
        .willReturn(response)
    )
  }

  private def get(bearer: String = token, extraHeaders: Seq[(String, String)] = Seq.empty): WSResponse =
    await(ws.url(endpoint).withHttpHeaders((Seq("Authorization" -> bearer) ++ extraHeaders): _*).get())

  private def put(
    body: String = validBody,
    bearer: String = token,
    extraHeaders: Seq[(String, String)] = Seq.empty
  ): WSResponse =
    await(
      ws.url(endpoint)
        .withHttpHeaders((Seq("Authorization" -> bearer, "Content-Type" -> "application/json") ++ extraHeaders): _*)
        .put(body)
    )

  private val relationshipError = Json.obj(
    errorCodeField    -> agentClientRelationshipNotEstablishedCode,
    errorMessageField -> agentClientRelationshipNotEstablishedMessage
  )

  "Agent preferences endpoints" should {
    "store, read and overwrite a preference without extending its expiry" in {
      stubAgent()
      stubDelegated()
      stubDelegated("Z5678")
      await(
        repo.collection.insertOne(StoredAgentPreferences(sessionId, "Z9999", now.plus(1, ChronoUnit.HOURS))).toFuture()
      )

      put(extraHeaders = Seq("X-Session-ID" -> "other-session")).status shouldBe NO_CONTENT
      val stored = await(repo.collection.find(Filters.equal(mongoIdField, sessionId)).headOption()).get
      stored.zReference                                                                      shouldBe testZref
      stored.expiresAt                                                                       shouldBe now.plus(1, ChronoUnit.HOURS)
      await(repo.collection.find(Filters.equal(mongoIdField, "other-session")).headOption()) shouldBe None

      val first = get()
      first.status shouldBe OK
      first.json   shouldBe Json.obj("zReference" -> testZref)

      put("""{"zReference":"Z5678"}""").status shouldBe NO_CONTENT
      val updated = await(repo.collection.find(Filters.equal(mongoIdField, sessionId)).headOption()).get
      updated.zReference                                 shouldBe "Z5678"
      updated.expiresAt                                  shouldBe stored.expiresAt
      get().json                                         shouldBe Json.obj("zReference" -> "Z5678")
      await(repo.collection.countDocuments().toFuture()) shouldBe 1L

      verify(
        postRequestedFor(urlEqualTo("/auth/authorise"))
          .withHeader("Authorization", equalTo(token))
          .withRequestBody(
            equalToJson(
              """{"authorise":[{"affinityGroup":"Agent"},{"enrolment":"HMRC-AS-AGENT","identifiers":[],"state":"Activated"}],"retrieve":["mdtpInformation"]}"""
            )
          )
      )
      verify(
        postRequestedFor(urlEqualTo("/auth/authorise"))
          .withHeader("Authorization", equalTo(token))
          .withRequestBody(
            equalToJson(
              s"""{"authorise":[{"enrolment":"HMRC-DISA-ORG","identifiers":[{"key":"ZREF","value":"$testZref"}],"state":"Activated","delegatedAuthRule":"disa-auth"}],"retrieve":[]}"""
            )
          )
      )
    }

    "keep two authenticated sessions separate even when a session header is supplied" in {
      stubAgent()
      stubAgent(secondToken, Some(secondSession))
      stubDelegated()
      stubDelegated("Z5678", secondToken)

      put().status shouldBe NO_CONTENT
      await(repo.collection.find(Filters.equal(mongoIdField, sessionId)).headOption()).get.expiresAt shouldBe
        now.plus(90, ChronoUnit.MINUTES)
      val response = get(secondToken, Seq("X-Session-ID" -> sessionId))
      response.status                                                                         shouldBe NOT_FOUND
      response.json                                                                           shouldBe Json.obj(
        errorCodeField    -> agentPreferencesNotFoundCode,
        errorMessageField -> s"$agentPreferencesNotFoundMessage$secondSession"
      )
      put("""{"zReference":"Z5678"}""", secondToken, Seq("X-Session-ID" -> sessionId)).status shouldBe NO_CONTENT
      get(secondToken).json                                                                   shouldBe Json.obj("zReference" -> "Z5678")
      get().json                                                                              shouldBe Json.obj("zReference" -> testZref)
    }

    "return 404 for an absent preference without a delegated check" in {
      stubAgent()
      val response = get()
      response.status shouldBe NOT_FOUND
      response.json   shouldBe Json.obj(
        errorCodeField    -> agentPreferencesNotFoundCode,
        errorMessageField -> s"$agentPreferencesNotFoundMessage$sessionId"
      )
      verify(
        0,
        postRequestedFor(urlEqualTo("/auth/authorise")).withRequestBody(equalToJson(delegatedAuthRequest(testZref)))
      )
    }

    "treat an expired record as missing even while it remains in Mongo" in {
      stubAgent()
      await(repo.collection.insertOne(StoredAgentPreferences(sessionId, testZref, now)).toFuture())
      await(repo.collection.find(Filters.equal(mongoIdField, sessionId)).headOption()).isDefined shouldBe true

      get().status                                                                               shouldBe NOT_FOUND
      await(repo.collection.find(Filters.equal(mongoIdField, sessionId)).headOption()).isDefined shouldBe true
      verify(
        0,
        postRequestedFor(urlEqualTo("/auth/authorise")).withRequestBody(equalToJson(delegatedAuthRequest(testZref)))
      )
    }

    "use a TTL index for cleanup and the Mongo _id index for unique sessions" in {
      val indexes = await(repo.collection.listIndexes().toFuture())
      val ttl     = indexes.find(_.getString("name") == agentPreferencesTtlIndex).get
      ttl.get("key").get.asDocument().getInt32("expiresAt").getValue shouldBe 1
      ttl.get("expireAfterSeconds").get.asNumber().longValue()       shouldBe 0L
      indexes.exists(_.getString("name") == "_id_")                  shouldBe true
    }

    "return 500 JSON when Mongo is online but rejects the preference write" in {
      stubAgent()
      stubDelegated()
      val index = "testUniqueZReference"
      await(
        repo.collection.createIndex(Indexes.ascending("zReference"), IndexOptions().unique(true).name(index)).toFuture()
      )
      try {
        await(repo.set(secondSession, testZref)) shouldBe true

        val response = put()
        response.status            shouldBe INTERNAL_SERVER_ERROR
        response.json              shouldBe Json.obj(
          errorCodeField    -> internalServerErrorCode,
          errorMessageField -> internalServerErrorMessage
        )
        await(repo.get(sessionId)) shouldBe None
      } finally await(repo.collection.dropIndex(index).toFuture())
    }

    "return 403 on GET and PUT if the delegated check fails, without replacing the preference" in {
      stubAgent()
      stubDelegated(failure = Some("InsufficientEnrolments"))
      await(repo.set(sessionId, testZref))

      val read = get()
      read.status shouldBe FORBIDDEN
      read.json   shouldBe relationshipError
      val write = put()
      write.status                                 shouldBe FORBIDDEN
      write.json                                   shouldBe relationshipError
      await(repo.get(sessionId)).map(_.zReference) shouldBe Some(testZref)
    }

    Seq("""{"zReference":"Z123"}""", """{"zReference":null}""").foreach { body =>
      s"return 400 without saving an invalid payload: $body" in {
        stubAgent()
        val response = put(body)
        response.status                                    shouldBe BAD_REQUEST
        response.json                                      shouldBe Json.obj(
          errorCodeField    -> invalidZReferenceFormatCode,
          errorMessageField -> invalidZReferenceFormatMessage
        )
        await(repo.collection.countDocuments().toFuture()) shouldBe 0L
      }
    }

    "return Play's default 400 for malformed JSON without saving" in {
      val response = put("not-json")
      response.status                                    shouldBe BAD_REQUEST
      await(repo.collection.countDocuments().toFuture()) shouldBe 0L
      verify(0, postRequestedFor(urlEqualTo("/auth/authorise")))
    }

    Seq("UnsupportedAffinityGroup", "InsufficientEnrolments").foreach { reason =>
      s"return the non-agent error on both endpoints when Auth returns $reason" in {
        stubAgent(failure = Some(reason))
        Seq(get(), put()).foreach { response =>
          response.status shouldBe FORBIDDEN
          response.json   shouldBe Json.obj(errorCodeField -> userIsNotAnAgentCode, errorMessageField -> userIsNotAnAgentMessage)
        }
        await(repo.collection.countDocuments().toFuture()) shouldBe 0L
      }
    }

    "reject both endpoints when Auth does not provide a session ID" in {
      stubAgent(session = None)
      Seq(get(), put()).foreach { response =>
        response.status shouldBe FORBIDDEN
        response.json   shouldBe Json.obj(
          errorCodeField    -> sessionIdNotAvailableCode,
          errorMessageField -> sessionIdNotAvailableMessage
        )
      }
    }

    "reject an invalid bearer token on both endpoints" in {
      stubAgent(failure = Some("InvalidBearerToken"))
      get().status shouldBe UNAUTHORIZED
      put().status shouldBe UNAUTHORIZED
    }
  }
}
