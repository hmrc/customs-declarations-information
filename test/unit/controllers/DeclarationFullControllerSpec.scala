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

import org.mockito.ArgumentMatchers.{eq as meq, *}
import org.mockito.Mockito.*
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import play.api.http.Status
import play.api.mvc.*
import play.api.test.Helpers
import play.api.test.Helpers.*
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse.*
import uk.gov.hmrc.customs.api.common.logging.CdsLogger
import uk.gov.hmrc.customs.declarations.information.action.*
import uk.gov.hmrc.customs.declarations.information.config.{ConfigService, InformationConfig, InformationShutterConfig}
import uk.gov.hmrc.customs.declarations.information.connectors.{ApiSubscriptionFieldsConnector, DeclarationFullConnector}
import uk.gov.hmrc.customs.declarations.information.controllers.DeclarationFullController
import uk.gov.hmrc.customs.declarations.information.logging.InformationLogger
import uk.gov.hmrc.customs.declarations.information.model.*
import uk.gov.hmrc.customs.declarations.information.services.*
import uk.gov.hmrc.customs.declarations.information.services.declaration.DeclarationFullService
import uk.gov.hmrc.customs.declarations.information.services.filter.FullResponseFilterService
import uk.gov.hmrc.customs.declarations.information.util.HeaderValidator
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import util.ApiSubscriptionFieldsTestData.apiSubscriptionFieldsResponse
import util.FakeRequests.*
import util.RequestHeaders.*
import util.TestData.*
import util.XmlOps.stringToXml
import util.{AuthConnectorStubbing, UnitSpec, VerifyLogging, VersionTestXMLData}

import java.time.ZonedDateTime
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class DeclarationFullControllerSpec extends UnitSpec with Matchers with BeforeAndAfterEach {
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

    protected val stubHttpResponse = HttpResponse(Status.OK, VersionTestXMLData.validBackendVersionResponse.toString)

    protected val mockDeclarationFullConnector: DeclarationFullConnector = mock(classOf[DeclarationFullConnector])
    protected val customsAuthService = new CustomsAuthService(mockAuthConnector, mockInformationLogger)
    protected val dateTime = ZonedDateTime.now()

    protected val stubAuthStatusAction: DeclarationFullAuthAction = new DeclarationFullAuthAction(customsAuthService, headerValidator, mockInformationLogger)
    protected val stubShutterCheckAction: ShutterCheckAction = new ShutterCheckAction(mockInformationLogger, mockInformationConfigService)
    protected val stubInternalClientIdsCheckAction: InternalClientIdsCheckAction = new InternalClientIdsCheckAction(mockInformationLogger, mockInformationConfigService)
    protected val stubDeclarationFullCheckAction: DeclarationFullCheckAction = new DeclarationFullCheckAction(mockInformationLogger, mockInformationConfigService)

    protected val stubValidateAndExtractHeadersAction: ValidateAndExtractHeadersAction = new ValidateAndExtractHeadersAction(new HeaderValidator(mockInformationLogger))
    protected val stubFullResponseFilterService: FullResponseFilterService = new FullResponseFilterService()
    protected val stubDeclarationFullService = new DeclarationFullService(stubFullResponseFilterService, mockApiSubscriptionFieldsConnector,
      mockInformationLogger, mockDeclarationFullConnector, stubUniqueIdsService)
    protected val stubConversationIdAction = new ConversationIdAction(stubUniqueIdsService, mockInformationLogger)

    protected val controller: DeclarationFullController = new DeclarationFullController(
      stubShutterCheckAction,
      stubValidateAndExtractHeadersAction,
      stubAuthStatusAction,
      stubConversationIdAction,
      stubInternalClientIdsCheckAction,
      stubDeclarationFullCheckAction,
      stubDeclarationFullService,
      Helpers.stubControllerComponents(),
      mockInformationLogger) {}

    protected def awaitSubmitMrn(request: Request[AnyContent], declarationVersion: Option[String] = None, declarationSubmissionChannel: Option[String] = None): Result = {
      controller.list(mrnValue, declarationVersion, declarationSubmissionChannel).apply(request)
    }

    protected def submitMrn(request: Request[AnyContent], declarationVersion: Option[String] = None, declarationSubmissionChannel: Option[String] = None): Future[Result] = {
      controller.list(mrnValue, declarationVersion, declarationSubmissionChannel).apply(request)
    }

    when(mockDeclarationFullConnector.send(any[ZonedDateTime], meq[UUID](correlationId.uuid).asInstanceOf[CorrelationId], any[ApiVersion], any[Option[ApiSubscriptionFieldsResponse]], meq[Mrn](mrn))(any[AuthorisedRequest[Any]], any[HeaderCarrier])).thenReturn(Future.successful(Right(stubHttpResponse)))
    when(mockApiSubscriptionFieldsConnector.getSubscriptionFields(any[ApiSubscriptionKey])(any[ValidatedHeadersRequest[Any]], any[HeaderCarrier])).thenReturn(Future.successful(Some(apiSubscriptionFieldsResponse)))
  }

  private val errorResultBadgeIdentifier = errorBadRequest("X-Badge-Identifier header is missing or invalid").XmlResult.withHeaders(X_CONVERSATION_ID_HEADER)
  private val errorResultMissingIdentifiers = errorBadRequest("Both X-Submitter-Identifier and X-Badge-Identifier headers are missing").XmlResult.withHeaders(X_CONVERSATION_ID_HEADER)

  private val errorResultMrnTooLong = errorBadRequest("MRN parameter too long").XmlResult.withHeaders(X_CONVERSATION_ID_HEADER)
  private val errorResultMissingMrn = errorBadRequest("Missing MRN parameter").XmlResult.withHeaders(X_CONVERSATION_ID_HEADER)
  private val errorResultCDS60002 = errorBadRequest("MRN parameter invalid", "CDS60002").XmlResult.withHeaders(X_CONVERSATION_ID_HEADER)

  "Declaration Version Controller for MRN queries" should {
    "process CSP request when call is authorised for CSP" in new SetUp() {
      authoriseCsp()

      awaitSubmitMrn(ValidCspDeclarationRequest)

      verifyCspAuthorisationCalled(numberOfTimes = 1)
    }

    "process CSP request when call is authorised for CSP and declarationSubmissionChannel is set and is internal clientId" in new SetUp() {
      authoriseCsp()
      when(mockInformationConfigService.informationConfig).thenReturn(InformationConfig("url", 30, Seq("SOME_X_CLIENT_ID")))
      val result: Future[Result] = submitMrn(ValidCspDeclarationVersionRequestWithDeclarationSubmissionChannel, None, Some("AuthenticatedPartyOnly"))
      status(result) shouldBe OK
      verifyCspAuthorisationCalled(numberOfTimes = 1)
    }

    "process CSP request when call is authorised for CSP and declarationSubmissionChannel is invalid" in new SetUp() {
      authoriseCsp()

      val result: Future[Result] = submitMrn(ValidCspDeclarationVersionRequestWithInvalidDeclarationSubmissionChannel, None, Some("AuthenticatedPartyOnly1"))
      status(result) shouldBe BAD_REQUEST
      stringToXml(contentAsString(result)) shouldBe stringToXml("<errorResponse><code>CDS60011</code><message>Invalid declarationSubmissionChannel parameter</message></errorResponse>")
      VerifyLogging.verifyInformationLogger("info","declarationSubmissionChannel parameter passed is invalid: Some(AuthenticatedPartyOnly1)")(mockInformationLogger)
    }

    "respond with status 200 and conversationId in header for a processed valid CSP request" in new SetUp() {
      authoriseCsp()

      val result: Future[Result] = submitMrn(ValidCspDeclarationRequest)

      status(result) shouldBe OK
      header(X_CONVERSATION_ID_NAME, result) shouldBe Some(conversationIdValue)
    }

    "respond with status 400 and conversationId in header for a request without an Mrn" in new SetUp() {
      authoriseCsp()

      val result: Future[Result] = controller.list("", None, None).apply(ValidCspDeclarationRequest)

      status(result) shouldBe BAD_REQUEST
      stringToXml(contentAsString(result)) shouldBe stringToXml("<errorResponse><code>BAD_REQUEST</code><message>Missing MRN parameter</message></errorResponse>")

      header(X_CONVERSATION_ID_NAME, result) shouldBe Some(conversationIdValue)
    }

    "respond with status 400 and conversationId in header for a request with an Mrn containing a space" in new SetUp() {
      authoriseCsp()

      val result: Future[Result] = controller.list("12345678 ", None, None).apply(ValidCspDeclarationRequest)

      status(result) shouldBe BAD_REQUEST
      stringToXml(contentAsString(result)) shouldBe stringToXml("<errorResponse><code>CDS60002</code><message>MRN parameter invalid</message></errorResponse>")
      header(X_CONVERSATION_ID_NAME, result) shouldBe Some(conversationIdValue)
    }

    "respond with status 400 for a CSP request with both a missing X-Badge-Identifier and a missing X-Submitter-Identifier" in new SetUp() {
      authoriseCsp()

      val result: Result = awaitSubmitMrn(ValidCspDeclarationRequest.withHeaders(ValidCspDeclarationRequest.headers.remove(X_BADGE_IDENTIFIER_NAME)))
      result shouldBe errorResultMissingIdentifiers
      verifyNoMoreInteractions(mockDeclarationFullConnector)
      VerifyLogging.verifyInformationLogger("info","X-Badge-Identifier header empty and allowed")(mockInformationLogger)
      VerifyLogging.verifyInformationLogger("error","Both X-Submitter-Identifier and X-Badge-Identifier are missing")(mockInformationLogger)
    }

    "respond with status 500 for a CSP request with a missing X-Client-ID" in new SetUp() {
      authoriseCsp()

      val result: Result = awaitSubmitMrn(ValidCspDeclarationRequest.withHeaders(ValidCspDeclarationRequest.headers.remove(X_CLIENT_ID_NAME)))
      status(result) shouldBe INTERNAL_SERVER_ERROR
      verifyNoMoreInteractions(mockDeclarationFullConnector)
    }

    "respond with status 400 for a CSP request with an invalid X-Badge-Identifier" in new SetUp() {
      authoriseCsp()

      val result: Result = awaitSubmitMrn(ValidCspDeclarationRequest.withHeaders((ValidHeaders + X_BADGE_IDENTIFIER_HEADER_INVALID_CHARS).toSeq: _*))

      result shouldBe errorResultBadgeIdentifier
      verifyNoMoreInteractions(mockDeclarationFullConnector)
      VerifyLogging.verifyInformationLogger("error","X-Badge-Identifier invalid or not present for CSP")(mockInformationLogger)
    }

    "respond with status 400 for a request with an MRN value that is too long" in new SetUp() {
      authoriseCsp()

      val result: Result = controller.list(invalidMrnTooLong, None, None).apply(ValidCspDeclarationRequest.withHeaders(ValidHeaders.toSeq: _*))
      result shouldBe errorResultMrnTooLong
      verifyNoMoreInteractions(mockDeclarationFullConnector)
      VerifyLogging.verifyInformationLogger("info","MRN parameter is too long: theMrnThatIsTooLongToBeAcceptableToThisService")(mockInformationLogger)
    }

    "respond with status 400 for a request with an MRN value that is too short" in new SetUp() {
      authoriseCsp()

      val result: Result = controller.list("", None, None).apply(ValidCspDeclarationRequest.withHeaders(ValidHeaders.toSeq: _*))

      result shouldBe errorResultMissingMrn
      verifyNoMoreInteractions(mockDeclarationFullConnector)
    }

    "respond with status 400 for a request with an MRN value that contains a space" in new SetUp() {
      authoriseCsp()

      val result: Result = controller.list("mrn withASpace", None, None).apply(ValidCspDeclarationRequest.withHeaders(ValidHeaders.toSeq: _*))

      result shouldBe errorResultCDS60002
      verifyNoMoreInteractions(mockDeclarationFullConnector)
      VerifyLogging.verifyInformationLogger("info","MRN parameter is invalid: mrn withASpace")(mockInformationLogger)
    }

    "process non-CSP request when call is authorised for non-CSP" in new SetUp() {
      unauthoriseCsp()
      authoriseNonCsp(Some(declarantEori))

      awaitSubmitMrn(ValidNonCspDeclarationRequest)

      verifyNonCspAuthorisationCalled(numberOfTimes = 1)
    }

    "process non-CSP request when call is authorised for non-CSP with declarationSubmissionChannel set" in new SetUp() {
      unauthoriseCsp()
      authoriseNonCsp(Some(declarantEori))
      when(mockInformationConfigService.informationConfig).thenReturn(InformationConfig("url", 30, Seq("SOME_X_CLIENT_ID")))
      awaitSubmitMrn(ValidNonCspDeclarationVersionRequestWithDeclarationSubmissionChannel)

      verifyNonCspAuthorisationCalled(numberOfTimes = 1)
    }

    "process non-CSP request when call is authorised for non-CSP with invalid declarationSubmissionChannel" in new SetUp() {
      unauthoriseCsp()
      authoriseNonCsp(Some(declarantEori))

      val result: Future[Result] = submitMrn(ValidNonCspDeclarationVersionRequestWithInvalidDeclarationSubmissionChannel)

      status(result) shouldBe BAD_REQUEST

      stringToXml(contentAsString(result)) shouldBe stringToXml("<errorResponse><code>CDS60011</code><message>Invalid declarationSubmissionChannel parameter</message></errorResponse>")

    }

    "respond with status 200 and conversationId in header for a processed valid non-CSP request" in new SetUp() {
      unauthoriseCsp()
      authoriseNonCsp(Some(declarantEori))

      val result: Future[Result] = submitMrn(ValidNonCspDeclarationRequest)

      status(result) shouldBe OK
      header(X_CONVERSATION_ID_NAME, result) shouldBe Some(conversationIdValue)
      VerifyLogging.verifyInformationLogger("info", "Full declaration information version processed successfully.")(mockInformationLogger)
    }

    "respond with status 200 for a non-CSP request with both a missing X-Badge-Identifier and a missing X-Submitter-Identifier" in new SetUp() {
      unauthoriseCsp()
      authoriseNonCsp(Some(declarantEori))

      val result: Result = awaitSubmitMrn(ValidNonCspDeclarationRequest.withHeaders(ValidNonCspDeclarationRequest.headers.remove(X_BADGE_IDENTIFIER_NAME)))
      status(result) shouldBe OK
      verifyNonCspAuthorisationCalled(numberOfTimes = 1)
      VerifyLogging.verifyInformationLogger("debug","Full declaration information request received. Path = / \nheaders = List((Host,localhost), (Accept,application/vnd.hmrc.1.0+xml), (X-Client-ID,SOME_X_CLIENT_ID), (Authorization,Bearer Software-House-Bearer-Token))")(mockInformationLogger)
    }

    "respond with status 500 for a non-CSP request with a missing X-Client-ID" in new SetUp() {
      unauthoriseCsp()
      authoriseNonCsp(Some(declarantEori))

      val result: Result = awaitSubmitMrn(ValidNonCspDeclarationRequest.withHeaders(ValidNonCspDeclarationRequest.headers.remove(X_CLIENT_ID_NAME)))
      status(result) shouldBe INTERNAL_SERVER_ERROR
      verifyNoMoreInteractions(mockDeclarationFullConnector)
      VerifyLogging.verifyInformationLogger("error","Error - header 'X-Client-ID' not present")(mockInformationLogger)
    }

    "return the Internal Server error when connector returns a 500 " in new SetUp() {
      when(mockDeclarationFullConnector.send(any[ZonedDateTime],
        meq[UUID](correlationId.uuid).asInstanceOf[CorrelationId],
        any[ApiVersion],
        any[Option[ApiSubscriptionFieldsResponse]],
        meq[Mrn](mrn))(any[AuthorisedRequest[Any]], any[HeaderCarrier]))
        .thenReturn(Future.successful(Left(UnexpectedError(emulatedServiceFailure))))

      authoriseCsp()

      val result: Result = awaitSubmitMrn(ValidCspDeclarationRequest)

      result.header.status shouldBe INTERNAL_SERVER_ERROR
    }
  }
}
