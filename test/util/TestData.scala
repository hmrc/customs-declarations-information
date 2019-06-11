/*
 * Copyright 2019 HM Revenue & Customs
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

package util

import java.util.UUID
import java.util.UUID.fromString

import com.google.inject.AbstractModule
import org.joda.time.DateTime
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.http.HeaderNames._
import play.api.inject.guice.GuiceableModule
import play.api.test.FakeRequest
import uk.gov.hmrc.customs.api.common.logging.CdsLogger
import uk.gov.hmrc.customs.declarations.information.model._
import uk.gov.hmrc.customs.declarations.information.model.actionbuilders.ActionBuilderModelHelper._
import uk.gov.hmrc.customs.declarations.information.model.actionbuilders.{ConversationIdRequest, ExtractedHeadersImpl}
import uk.gov.hmrc.customs.declarations.information.services.{UniqueIdsService, UuidService}
import unit.logging.StubInformationLogger

object TestData {
  val conversationIdValue = "38400000-8cf0-11bd-b23e-10b96e4ef00d"
  val conversationIdUuid: UUID = fromString(conversationIdValue)
  val conversationId: ConversationId = ConversationId(conversationIdUuid)

  val correlationIdValue = "e61f8eee-812c-4b8f-b193-06aedc60dca2"
  val correlationIdUuid: UUID = fromString(correlationIdValue)
  val correlationId = CorrelationId(correlationIdUuid)

  val dmirIdValue = "1b0a48a8-1259-42c9-9d6a-e797b919eb16"
  val dmirIdUuid: UUID = fromString(dmirIdValue)
  val dmirId = DeclarationManagementInformationRequestId(dmirIdUuid)

  val mrnValue = "theMrn"
  val mrn = Mrn(mrnValue)

  val date: DateTime = DateTime.parse("2018-09-11T10:28:54.128Z")

  val subscriptionFieldsIdString: String = "b82f31c6-2239-4253-b6f5-ed75e37ab7a5"
  val subscriptionFieldsIdUuid: UUID = fromString("b82f31c6-2239-4253-b6f5-ed75e37ab7a5")

  val clientSubscriptionIdString: String = "327d9145-4965-4d28-a2c5-39dedee50334"

  val validBadgeIdentifierValue = "BADGEID123"
  val invalidBadgeIdentifierValue = "INVALIDBADGEID123456789"
  val invalidBadgeIdentifier: BadgeIdentifier = BadgeIdentifier(invalidBadgeIdentifierValue)
  val badgeIdentifier: BadgeIdentifier = BadgeIdentifier(validBadgeIdentifierValue)

  val cspBearerToken = "CSP-Bearer-Token"
  val invalidBearerToken = "InvalidBearerToken"

  type EmulatedServiceFailure = UnsupportedOperationException
  val emulatedServiceFailure = new EmulatedServiceFailure("Emulated service failure.")

  lazy val mockUuidService: UuidService = mock[UuidService]

  lazy val stubDeclarationsLogger = new StubInformationLogger(mock[CdsLogger])

  object TestModule extends AbstractModule {
    def configure(): Unit = {
      bind(classOf[UuidService]) toInstance mockUuidService
    }

    def asGuiceableModule: GuiceableModule = GuiceableModule.guiceable(this)
  }

  // note we can not mock service methods that return value classes - however using a simple stub IMHO it results in cleaner code (less mocking noise)
  lazy val stubUniqueIdsService: UniqueIdsService = new UniqueIdsService(mockUuidService) {
    override def conversation: ConversationId = conversationId

    override def correlation: CorrelationId = correlationId

    override def dmir: DeclarationManagementInformationRequestId = dmirId
  }

  val TestFakeRequest = FakeRequest().withHeaders(("Accept", "application/vnd.hmrc.1.0+xml"))

  val TestConversationIdRequest = ConversationIdRequest(conversationId, TestFakeRequest)
  val TestExtractedHeaders = ExtractedHeadersImpl(VersionOne, badgeIdentifier, ApiSubscriptionFieldsTestData.clientId)
  val TestValidatedHeadersRequest = TestConversationIdRequest.toValidatedHeadersRequest(TestExtractedHeaders)
  val TestAuthorisedRequest = TestValidatedHeadersRequest.toAuthorisedRequest

  val TestCspAuthorisedRequest = TestValidatedHeadersRequest.toAuthorisedRequest
  val TestValidatedHeadersRequestNoBadge = TestConversationIdRequest.toValidatedHeadersRequest(TestExtractedHeaders)

}

object RequestHeaders {

  val X_CONVERSATION_ID_NAME = "X-Conversation-ID"
  lazy val X_CONVERSATION_ID_HEADER: (String, String) = X_CONVERSATION_ID_NAME -> TestData.conversationId.toString

  val X_BADGE_IDENTIFIER_NAME = "X-Badge-Identifier"
  lazy val X_BADGE_IDENTIFIER_HEADER: (String, String) = X_BADGE_IDENTIFIER_NAME -> TestData.badgeIdentifier.value
  lazy val X_BADGE_IDENTIFIER_HEADER_INVALID_TOO_LONG: (String, String) = X_BADGE_IDENTIFIER_NAME -> TestData.invalidBadgeIdentifierValue
  val X_BADGE_IDENTIFIER_HEADER_INVALID_CHARS: (String, String) = X_BADGE_IDENTIFIER_NAME -> "Invalid^&&("
  val X_BADGE_IDENTIFIER_HEADER_INVALID_TOO_SHORT: (String, String) = X_BADGE_IDENTIFIER_NAME -> "12345"
  val X_BADGE_IDENTIFIER_HEADER_INVALID_LOWERCASE: (String, String) = X_BADGE_IDENTIFIER_NAME -> "BadgeId123"

  val X_CLIENT_ID_NAME = "X-Client-ID"
  val X_CLIENT_ID_HEADER: (String, String) = X_CLIENT_ID_NAME -> ApiSubscriptionFieldsTestData.xClientId
  val X_CLIENT_ID_HEADER_INVALID: (String, String) = X_CLIENT_ID_NAME -> "This is not a UUID"

  val ACCEPT_HMRC_XML = "application/vnd.hmrc.1.0+xml"
  val ACCEPT_HMRC_XML_HEADER: (String, String) = ACCEPT -> ACCEPT_HMRC_XML
  val ACCEPT_HEADER_INVALID: (String, String) = ACCEPT -> "invalid"

  val ValidHeaders: Map[String, String] = Map(
    ACCEPT_HMRC_XML_HEADER,
    X_CLIENT_ID_HEADER,
    X_BADGE_IDENTIFIER_HEADER
  )
}
