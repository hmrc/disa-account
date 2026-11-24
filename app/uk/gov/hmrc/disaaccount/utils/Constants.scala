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

object Constants {
  val agentEnrolment       = "HMRC-AS-AGENT"
  val delegatedAuthRule    = "disa-auth"
  val disaEnrolment        = "HMRC-DISA-ORG"
  val zReferenceIdentifier = "ZREF"

  val errorCodeField    = "code"
  val errorMessageField = "message"

  val unauthorizedCode               = "UNAUTHORIZED"
  val unauthorizedMessage            = "Authentication is required"
  val userIsNotAnOrganisationCode    = "USER_IS_NOT_AN_ORGANISATION"
  val userIsNotAnOrganisationMessage = "User is not an Organisation"
  val disaEnrolmentNotFoundCode      = "DISA_ENROLMENT_NOT_FOUND"
  val disaEnrolmentNotFoundMessage   = "An active DISA enrolment matching the requested Z-Reference is required"
  val clientDetailsNotFoundCode      = "CLIENT_DETAILS_NOT_FOUND"
  val clientDetailsNotFoundMessage   = "Client details were not found"

  val agentClientRelationshipNotEstablishedCode    = "AGENT_CLIENT_RELATIONSHIP_NOT_ESTABLISHED"
  val agentClientRelationshipNotEstablishedMessage = "Agent to client relationship not established"
  val agentPreferencesNotFoundCode                 = "AGENT_PREFERENCES_NOT_FOUND"
  val agentPreferencesNotFoundMessage              = "Agent preferences were not found for session ID: "
  val internalServerErrorCode                      = "INTERNAL_SERVER_ERROR"
  val internalServerErrorMessage                   = "Internal server error"
  val invalidZReferenceFormatCode                  = "INVALID_ZREFERENCE_FORMAT"
  val invalidZReferenceFormatMessage               = "Z-Reference format is invalid"
  val serviceUnavailableCode                       = "SERVICE_UNAVAILABLE"
  val serviceUnavailableMessage                    = "Service is currently unavailable"
  val sessionIdNotAvailableCode                    = "SESSION_ID_NOT_AVAILABLE"
  val sessionIdNotAvailableMessage                 = "Session ID is not available"
  val userIsNotAnAgentCode                         = "USER_IS_NOT_AN_AGENT"
  val userIsNotAnAgentMessage                      = "User is not an Agent"

  val agentPreferencesTtlIndex = "expiresAtTtlIdx"
  val mongoExpiresAtField      = "expiresAt"
  val mongoIdField             = "_id"
}
