/*
 * Copyright 2021 HM Revenue & Customs
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

import com.google.inject.AbstractModule
import org.joda.time.DateTime
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.http.HeaderNames._
import play.api.inject.guice.GuiceableModule
import play.api.mvc.{AnyContentAsEmpty, Headers}
import play.api.test.FakeRequest
import uk.gov.hmrc.customs.api.common.logging.CdsLogger
import uk.gov.hmrc.customs.declarations.information.model._
import uk.gov.hmrc.customs.declarations.information.model.actionbuilders.ActionBuilderModelHelper._
import uk.gov.hmrc.customs.declarations.information.model.actionbuilders.{ApiVersionRequest, ConversationIdRequest, ExtractedHeadersImpl}
import uk.gov.hmrc.customs.declarations.information.services.{UniqueIdsService, UuidService}
import unit.logging.StubInformationLogger
import util.RequestHeaders.{X_BADGE_IDENTIFIER_NAME, X_SUBMITTER_IDENTIFIER_NAME}
import util.TestData.declarantEori

import java.util.UUID
import java.util.UUID.fromString

object TestData {
  val conversationIdValue = "38400000-8cf0-11bd-b23e-10b96e4ef00d"
  val conversationIdUuid: UUID = fromString(conversationIdValue)
  val conversationId: ConversationId = ConversationId(conversationIdUuid)

  val correlationIdValue = "e61f8eee-812c-4b8f-b193-06aedc60dca2"
  val correlationIdUuid: UUID = fromString(correlationIdValue)
  val correlationId = CorrelationId(correlationIdUuid)

  val mrnValue = "theMrn"
  val mrn = Mrn(mrnValue)
  val invalidMrnTooLong = "theMrnThatIsTooLongToBeAcceptableToThisService"

  val ducrValue = "theDucr"
  val ducr = Ducr(ducrValue)
  val invalidDucrTooLong = "theDucrThatIsTooLongToBeAcceptableToThisServiceFarFarTooLongInFactToBeOfUse"

  val ucrValue = "theUcr"
  val ucr = Ucr(ucrValue)
  val invalidUcrTooLong = "theUcrThatIsTooLongToBeAcceptableToThisService"

  val inventoryReferenceValue = "theInventoryReference"
  val inventoryReference = InventoryReference(inventoryReferenceValue)
  val invalidInventoryReferenceTooLong = "theInventoryReferenceThatIsTooLongToBeAcceptableToThisServiceFarFarTooLongInFactToBeOfUse"

  val dateString = "2018-09-11T10:28:54.128Z"
  val date: DateTime = DateTime.parse("2018-09-11T10:28:54.128Z")

  val subscriptionFieldsIdString: String = "b82f31c6-2239-4253-b6f5-ed75e37ab7a5"
  val subscriptionFieldsIdUuid: UUID = fromString("b82f31c6-2239-4253-b6f5-ed75e37ab7a5")

  val clientSubscriptionIdString: String = "327d9145-4965-4d28-a2c5-39dedee50334"

  val validBadgeIdentifierValue = "BADGEID123"
  val invalidBadgeIdentifierValue = "INVALIDBADGEID123456789"
  val invalidBadgeIdentifier: BadgeIdentifier = BadgeIdentifier(invalidBadgeIdentifierValue)
  val badgeIdentifier: BadgeIdentifier = BadgeIdentifier(validBadgeIdentifierValue)

  val declarantEoriValue = "ZZ123456789000"
  val declarantEori = Eori(declarantEoriValue)
  val invalidDeclarantEoriValue = "ZZ123456789000123456789"
  val invalidDeclarantEori = Eori(invalidDeclarantEoriValue)

  val cspBearerToken = "CSP-Bearer-Token"
  val nonCspBearerToken = "Software-House-Bearer-Token"
  val invalidBearerToken = "InvalidBearerToken"

  type EmulatedServiceFailure = UnsupportedOperationException
  val emulatedServiceFailure = new EmulatedServiceFailure("Emulated service failure.")

  lazy val mockUuidService: UuidService = mock[UuidService]

  lazy val stubInformationLogger = new StubInformationLogger(mock[CdsLogger])

  object TestModule extends AbstractModule {
    override def configure(): Unit = {
      bind(classOf[UuidService]) toInstance mockUuidService
    }

    def asGuiceableModule: GuiceableModule = GuiceableModule.guiceable(this)
  }

  // note we can not mock service methods that return value classes - however using a simple stub IMHO it results in cleaner code (less mocking noise)
  lazy val stubUniqueIdsService: UniqueIdsService = new UniqueIdsService(mockUuidService) {
    override def conversation: ConversationId = conversationId

    override def correlation: CorrelationId = correlationId
  }

  val TestFakeRequestV1 = FakeRequest().withHeaders(("Accept", "application/vnd.hmrc.1.0+xml"))
  val TestFakeRequestV2 = FakeRequest().withHeaders(("Accept", "application/vnd.hmrc.2.0+xml"))

  def testFakeRequestWithMaybeBadgeIdEoriPair(maybeBadgeIdString: Option[String] = Some(badgeIdentifier.value),
                                              maybeEoriString: Option[String] = Some(declarantEori.value)): FakeRequest[AnyContentAsEmpty.type] = {
    val headers = Headers(maybeBadgeIdString.fold(("",""))(badgeId => (X_BADGE_IDENTIFIER_NAME, badgeId)), maybeEoriString.fold(("",""))(eori => (X_SUBMITTER_IDENTIFIER_NAME, eori)))
    FakeRequest().withHeaders(headers.remove("")) //better to not add empty string tuple in first place
  }

  val TestConversationIdRequest = ConversationIdRequest(conversationId, TestFakeRequestV1)
  val TestApiVersionRequestV1 = ApiVersionRequest(conversationId, VersionOne, TestFakeRequestV1)
  val TestApiVersionRequestV2 = ApiVersionRequest(conversationId, VersionTwo, TestFakeRequestV2)

  val TestConversationIdRequestWithV1Headers = ConversationIdRequest(conversationId, TestFakeRequestV1)
  val TestConversationIdRequestWithV2Headers = ConversationIdRequest(conversationId, TestFakeRequestV2)

  val TestExtractedHeaders = ExtractedHeadersImpl(ApiSubscriptionFieldsTestData.clientId)
  val TestValidatedHeadersRequest = TestApiVersionRequestV1.toValidatedHeadersRequest(TestExtractedHeaders)
  val TestInternalClientIdsRequest = TestValidatedHeadersRequest.toInternalClientIdsRequest(None)
  val TestInternalClientIdsRequestWithDeclarationSubmissionChannel = TestValidatedHeadersRequest.toInternalClientIdsRequest(Some("AuthenticatedPartyOnly"))
  val TestCspWithoutBadgeAuthorisedRequest = TestInternalClientIdsRequest.toCspAuthorisedRequest(Csp(Some(declarantEori), None))
  val TestCspAuthorisedRequest = TestInternalClientIdsRequest.toCspAuthorisedRequest(Csp(Some(declarantEori), Some(badgeIdentifier)))
  val TestCspAuthorisedRequestWithDeclarationSubmissionChannel = TestInternalClientIdsRequestWithDeclarationSubmissionChannel.toCspAuthorisedRequest(Csp(Some(declarantEori), Some(badgeIdentifier)))
  val TestNonCspAuthorisedRequest = TestInternalClientIdsRequest.toNonCspAuthorisedRequest(declarantEori)
  val TestNonCspAuthorisedRequestWithDeclarationSubmissionChannel = TestInternalClientIdsRequestWithDeclarationSubmissionChannel.toNonCspAuthorisedRequest(declarantEori)
  val TestValidatedHeadersRequestNoBadgeIdNoEori = TestApiVersionRequestV1.toValidatedHeadersRequest(TestExtractedHeaders)

  lazy val TestValidatedHeadersRequestWithValidBadgeIdEoriPair =
    ApiVersionRequest(conversationId, VersionOne, testFakeRequestWithMaybeBadgeIdEoriPair()).toValidatedHeadersRequest(TestExtractedHeaders)

  lazy val TestValidatedHeadersRequestWithValidBadgeIdAndNoEori =
    ApiVersionRequest(conversationId, VersionOne, testFakeRequestWithMaybeBadgeIdEoriPair(maybeEoriString = None)).toValidatedHeadersRequest(TestExtractedHeaders)

  lazy val TestValidatedHeadersRequestWithValidEoriAndNoBadgeId =
    ApiVersionRequest(conversationId, VersionOne, testFakeRequestWithMaybeBadgeIdEoriPair(maybeBadgeIdString = None)).toValidatedHeadersRequest(TestExtractedHeaders)

  lazy val TestValidatedHeadersRequestWithInvalidBadgeIdTooLongAndEori =
    ApiVersionRequest(conversationId, VersionOne, testFakeRequestWithMaybeBadgeIdEoriPair(maybeBadgeIdString = Some("INVALID_BADGE_IDENTIFIER_TO_LONG"))).toValidatedHeadersRequest(TestExtractedHeaders)

  lazy val TestValidatedHeadersRequestWithInvalidBadgeIdTooShortAndEori =
    ApiVersionRequest(conversationId, VersionOne, testFakeRequestWithMaybeBadgeIdEoriPair(maybeBadgeIdString = Some("SHORT"))).toValidatedHeadersRequest(TestExtractedHeaders)

  lazy val TestValidatedHeadersRequestWithInvalidBadgeIdInvalidCharsAndEori =
    ApiVersionRequest(conversationId, VersionOne, testFakeRequestWithMaybeBadgeIdEoriPair(maybeBadgeIdString = Some("(*&*(^&*&%"))).toValidatedHeadersRequest(TestExtractedHeaders)

  lazy val TestValidatedHeadersRequestWithInvalidBadgeIdLowercaseAndEori =
    ApiVersionRequest(conversationId, VersionOne, testFakeRequestWithMaybeBadgeIdEoriPair(maybeBadgeIdString = Some("lowercase"))).toValidatedHeadersRequest(TestExtractedHeaders)

  lazy val TestValidatedHeadersRequestWithInvalidEoriTooLongAndBadgeId =
    ApiVersionRequest(conversationId, VersionOne, testFakeRequestWithMaybeBadgeIdEoriPair(maybeEoriString = Some("INVALID_EORI_TO_LONG"))).toValidatedHeadersRequest(TestExtractedHeaders)

  lazy val TestValidatedHeadersRequestWithInvalidEoriInvalidCharsAndBadgeId =
    ApiVersionRequest(conversationId, VersionOne, testFakeRequestWithMaybeBadgeIdEoriPair(maybeEoriString = Some("(*&*(^&*&%"))).toValidatedHeadersRequest(TestExtractedHeaders)

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

  val X_SUBMITTER_IDENTIFIER_NAME = "X-Submitter-Identifier"
  val X_SUBMITTER_IDENTIFIER_HEADER: (String, String) = X_SUBMITTER_IDENTIFIER_NAME -> declarantEori.value

  val X_CLIENT_ID_NAME = "X-Client-ID"
  val X_CLIENT_ID_HEADER: (String, String) = X_CLIENT_ID_NAME -> ApiSubscriptionFieldsTestData.xClientId
  val X_CLIENT_ID_HEADER_INVALID: (String, String) = X_CLIENT_ID_NAME -> "This is not a UUID"

  val ACCEPT_HMRC_XML_V1 = "application/vnd.hmrc.1.0+xml"
  val ACCEPT_HMRC_XML_V2 = "application/vnd.hmrc.2.0+xml"
  val ACCEPT_HMRC_XML_HEADER_V1: (String, String) = ACCEPT -> ACCEPT_HMRC_XML_V1
  val ACCEPT_HMRC_XML_HEADER_V2: (String, String) = ACCEPT -> ACCEPT_HMRC_XML_V2
  val ACCEPT_HEADER_INVALID: (String, String) = ACCEPT -> "invalid"

  val ValidHeaders: Map[String, String] = Map(
    ACCEPT_HMRC_XML_HEADER_V1,
    X_CLIENT_ID_HEADER,
    X_BADGE_IDENTIFIER_HEADER
  )
}
