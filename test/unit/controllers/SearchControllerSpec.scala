/*
 * Copyright 2024 HM Revenue & Customs
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

package unit.controllers

import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import play.api.http.Status
import play.api.mvc._
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse._
import uk.gov.hmrc.customs.api.common.logging.CdsLogger
import uk.gov.hmrc.customs.declarations.information.action.{ConversationIdAction, InternalClientIdsCheckAction, SearchAuthAction, SearchParametersCheckAction, ShutterCheckAction, ValidateAndExtractHeadersAction}
import uk.gov.hmrc.customs.declarations.information.config.{InformationConfig, ConfigService, InformationShutterConfig}
import uk.gov.hmrc.customs.declarations.information.connectors.AbstractDeclarationConnector._
import uk.gov.hmrc.customs.declarations.information.connectors.{ApiSubscriptionFieldsConnector, DeclarationSearchConnector}
import uk.gov.hmrc.customs.declarations.information.controllers.SearchController
import uk.gov.hmrc.customs.declarations.information.logging.InformationLogger
import uk.gov.hmrc.customs.declarations.information.model._
import uk.gov.hmrc.customs.declarations.information.services._
import uk.gov.hmrc.customs.declarations.information.services.declaration.DeclarationSearchService
import uk.gov.hmrc.customs.declarations.information.services.filter.SearchResponseFilterService
import uk.gov.hmrc.customs.declarations.information.util.HeaderValidator
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import util.ApiSubscriptionFieldsTestData.apiSubscriptionFieldsResponse
import util.FakeRequests._
import util.RequestHeaders._
import util.TestData._
import util.XmlOps.stringToXml
import util.{AuthConnectorStubbing, SearchTestXMLData, UnitSpec}

import java.time.ZonedDateTime
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class SearchControllerSpec extends UnitSpec
  with Matchers with BeforeAndAfterEach {

  trait SetUp extends AuthConnectorStubbing {

    protected val mockInformationConfigService: ConfigService = mock(classOf[ConfigService])
    when(mockInformationConfigService.informationShutterConfig).thenReturn(InformationShutterConfig(Some(false), Some(false)))
    when(mockInformationConfigService.informationConfig).thenReturn(InformationConfig("url", 1, Seq()))

    protected val mockInformationLogger: InformationLogger = mock(classOf[InformationLogger])
    override val mockAuthConnector: AuthConnector = mock(classOf[AuthConnector])

    protected val mockApiSubscriptionFieldsConnector: ApiSubscriptionFieldsConnector = mock(classOf[ApiSubscriptionFieldsConnector])
    protected val headerValidator = new HeaderValidator(mockInformationLogger)
    protected val mockCdsLogger: CdsLogger = mock(classOf[CdsLogger])
    protected val mockErrorResponse: ErrorResponse = mock(classOf[ErrorResponse])
    protected val mockResult: Result = mock(classOf[Result])
    protected implicit val ec: ExecutionContext = Helpers.stubControllerComponents().executionContext

    protected val stubHttpResponse = HttpResponse(Status.OK, SearchTestXMLData.validBackendSearchResponse.toString)

    protected val mockSearchConnector: DeclarationSearchConnector = mock(classOf[DeclarationSearchConnector])
    protected val customsAuthService = new CustomsAuthService(mockAuthConnector, mockInformationLogger)
    protected val dateTime = ZonedDateTime.now()

    protected val stubAuthStatusAction: SearchAuthAction = new SearchAuthAction(customsAuthService, headerValidator, mockInformationLogger)
    protected val stubShutterCheckAction: ShutterCheckAction = new ShutterCheckAction(mockInformationLogger, mockInformationConfigService)
    protected val stubInternalClientIdsCheckAction: InternalClientIdsCheckAction = new InternalClientIdsCheckAction(mockInformationLogger, mockInformationConfigService)
    protected val stubSearchParametersCheckAction: SearchParametersCheckAction = new SearchParametersCheckAction(mockInformationLogger, mockInformationConfigService)
    protected val stubValidateAndExtractHeadersAction: ValidateAndExtractHeadersAction = new ValidateAndExtractHeadersAction(new HeaderValidator(mockInformationLogger))
    protected val stubSearchResponseFilterService: SearchResponseFilterService = new SearchResponseFilterService()
    protected val stubDeclarationSearchService = new DeclarationSearchService(stubSearchResponseFilterService, mockApiSubscriptionFieldsConnector,
      mockInformationLogger, mockSearchConnector, stubUniqueIdsService, mockInformationConfigService)
    protected val stubConversationIdAction = new ConversationIdAction(stubUniqueIdsService, mockInformationLogger)

    protected val controller: SearchController = new SearchController(
      stubShutterCheckAction,
      stubValidateAndExtractHeadersAction,
      stubAuthStatusAction,
      stubConversationIdAction,
      stubSearchParametersCheckAction,
      stubInternalClientIdsCheckAction,
      stubDeclarationSearchService,
      Helpers.stubControllerComponents(),
      mockInformationLogger) {}

    protected def awaitSubmitSearch(request: Request[AnyContent]): Result = {
      controller.list(None, None, None, None, None, None, None, None, None).apply(request)
    }

    protected def submitSearch(request: Request[AnyContent]): Future[Result] = {
      controller.list(None, None, None, None, None, None, None, None, None).apply(request)
    }

    when(mockSearchConnector.send(any[ZonedDateTime], meq[UUID](correlationId.uuid).asInstanceOf[CorrelationId], any[ApiVersion], any[Option[ApiSubscriptionFieldsResponse]], any[ParameterSearch])(any[AuthorisedRequest[_]])).thenReturn(Future.successful(Right(stubHttpResponse)))
    when(mockApiSubscriptionFieldsConnector.getSubscriptionFields(any[ApiSubscriptionKey])(any[ValidatedHeadersRequest[_]], any[HeaderCarrier])).thenReturn(Future.successful(Some(apiSubscriptionFieldsResponse)))
  }

  private val errorResultBadgeIdentifier = errorBadRequest("X-Badge-Identifier header is missing or invalid").XmlResult.withHeaders(X_CONVERSATION_ID_HEADER)
  private val errorResultMissingIdentifiers = errorBadRequest("Both X-Submitter-Identifier and X-Badge-Identifier headers are missing").XmlResult.withHeaders(X_CONVERSATION_ID_HEADER)

  "Declaration Search Controller" should {
    "process CSP request when call is authorised for CSP" in new SetUp() {
      authoriseCsp()
      when(mockInformationConfigService.informationConfig).thenReturn(InformationConfig("url", 30, Seq("SOME_X_CLIENT_ID")))

      private val result = awaitSubmitSearch(FakeRequest("GET", "/search?partyRole=submitter&declarationCategory=IM&declarationSubmissionChannel=AuthenticatedPartyOnly").withHeaders(ValidHeaders.toSeq: _*).fromCsp)

      status(result) shouldBe OK
      verifyCspAuthorisationCalled(numberOfTimes = 1)
    }

    "process CSP request when call is authorised for CSP and declarationSubmissionChannel is set and is internal clientId" in new SetUp() {
      authoriseCsp()
      when(mockInformationConfigService.informationConfig).thenReturn(InformationConfig("url", 30, Seq("SOME_X_CLIENT_ID")))
      val result: Future[Result] = submitSearch(FakeRequest("GET", "/search?partyRole=submitter&declarationCategory=IM&declarationSubmissionChannel=AuthenticatedPartyOnly").withHeaders(ValidHeaders.toSeq: _*).fromCsp)
      status(result) shouldBe OK
      verifyCspAuthorisationCalled(numberOfTimes = 1)
    }

    "process CSP request when call is authorised for CSP and declarationSubmissionChannel is invalid" in new SetUp() {
      authoriseCsp()

      val result: Future[Result] = submitSearch(ValidCspDeclarationSearchRequestWithInvalidDeclarationSubmissionChannel)
      status(result) shouldBe BAD_REQUEST
      stringToXml(contentAsString(result)) shouldBe stringToXml("<errorResponse><code>CDS60011</code><message>Invalid declarationSubmissionChannel parameter</message></errorResponse>")
    }

    "respond with status 200 and conversationId in header for a processed valid CSP request" in new SetUp() {
      authoriseCsp()

      val result: Future[Result] = submitSearch(ValidCspDeclarationSearchRequest)

      status(result) shouldBe OK
      header(X_CONVERSATION_ID_NAME, result) shouldBe Some(conversationIdValue)
    }

    "respond with status 400 for a CSP request with both a missing X-Badge-Identifier and a missing X-Submitter-Identifier" in new SetUp() {
      authoriseCsp()

      val result: Result = awaitSubmitSearch(ValidCspDeclarationSearchRequest.withHeaders(ValidCspDeclarationSearchRequest.headers.remove(X_BADGE_IDENTIFIER_NAME)))
      result shouldBe errorResultMissingIdentifiers
      verifyNoMoreInteractions(mockSearchConnector)
    }

    "respond with status 500 for a CSP request with a missing X-Client-ID" in new SetUp() {
      authoriseCsp()

      val result: Result = awaitSubmitSearch(ValidCspDeclarationRequest.withHeaders(ValidCspDeclarationRequest.headers.remove(X_CLIENT_ID_NAME)))
      status(result) shouldBe INTERNAL_SERVER_ERROR
      verifyNoMoreInteractions(mockSearchConnector)
    }

    "respond with status 400 for a CSP request with an invalid X-Badge-Identifier" in new SetUp() {
      authoriseCsp()

      val result: Result = awaitSubmitSearch(ValidCspDeclarationSearchRequest.withHeaders((ValidHeaders + X_BADGE_IDENTIFIER_HEADER_INVALID_CHARS).toSeq: _*))

      result shouldBe errorResultBadgeIdentifier
      verifyNoMoreInteractions(mockSearchConnector)
    }

    "process non-CSP request when call is authorised for non-CSP" in new SetUp() {
      unauthoriseCsp()
      authoriseNonCsp(Some(declarantEori))

      awaitSubmitSearch(ValidNonCspDeclarationSearchRequest)

      verifyNonCspAuthorisationCalled(numberOfTimes = 1)
    }

    "process non-CSP request when call is authorised for non-CSP with declarationSubmissionChannel set" in new SetUp() {
      unauthoriseCsp()
      authoriseNonCsp(Some(declarantEori))
      when(mockInformationConfigService.informationConfig).thenReturn(InformationConfig("url", 30, Seq("SOME_X_CLIENT_ID")))
      awaitSubmitSearch(ValidNonCspDeclarationSearchWithSubChannelRequest)

      verifyNonCspAuthorisationCalled(numberOfTimes = 1)
    }

    "respond with status 200 and conversationId in header for a processed valid non-CSP request" in new SetUp() {
      unauthoriseCsp()
      authoriseNonCsp(Some(declarantEori))

      val result: Future[Result] = submitSearch(ValidNonCspDeclarationSearchRequest)

      status(result) shouldBe OK
      header(X_CONVERSATION_ID_NAME, result) shouldBe Some(conversationIdValue)
    }

    "respond with status 200 for a non-CSP request with both a missing X-Badge-Identifier and a missing X-Submitter-Identifier" in new SetUp() {
      unauthoriseCsp()
      authoriseNonCsp(Some(declarantEori))

      val result: Result = awaitSubmitSearch(ValidNonCspDeclarationSearchRequest.withHeaders(ValidNonCspDeclarationSearchRequest.headers.remove(X_BADGE_IDENTIFIER_NAME)))
      status(result) shouldBe OK
      verifyNonCspAuthorisationCalled(numberOfTimes = 1)
    }

    "respond with status 500 for a non-CSP request with a missing X-Client-ID" in new SetUp() {
      unauthoriseCsp()
      authoriseNonCsp(Some(declarantEori))

      val result: Result = awaitSubmitSearch(ValidNonCspDeclarationRequest.withHeaders(ValidNonCspDeclarationRequest.headers.remove(X_CLIENT_ID_NAME)))
      status(result) shouldBe INTERNAL_SERVER_ERROR
      verifyNoMoreInteractions(mockSearchConnector)
    }

    "return the Internal Server error when connector returns a 500 " in new SetUp() {
      when(mockSearchConnector.send(any[ZonedDateTime],
        meq[UUID](correlationId.uuid).asInstanceOf[CorrelationId],
        any[ApiVersion],
        any[Option[ApiSubscriptionFieldsResponse]],
        any[StatusSearchType])(any[AuthorisedRequest[_]]))
        .thenReturn(Future.successful(Left(UnexpectedError(emulatedServiceFailure))))

      authoriseCsp()

      val result: Result = awaitSubmitSearch(ValidNonCspDeclarationSearchRequest)

      result.header.status shouldBe INTERNAL_SERVER_ERROR
    }
  }
}
