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

package unit.controllers

import org.joda.time.DateTime
import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatest.{BeforeAndAfterEach, Matchers}
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status
import play.api.mvc._
import play.api.test.Helpers
import play.api.test.Helpers.{header, _}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse._
import uk.gov.hmrc.customs.api.common.logging.CdsLogger
import uk.gov.hmrc.customs.declarations.information.connectors.{ApiSubscriptionFieldsConnector, DeclarationStatusConnector, DeclarationVersionConnector}
import uk.gov.hmrc.customs.declarations.information.controllers.VersionController
import uk.gov.hmrc.customs.declarations.information.controllers.actionBuilders._
import uk.gov.hmrc.customs.declarations.information.logging.InformationLogger
import uk.gov.hmrc.customs.declarations.information.model._
import uk.gov.hmrc.customs.declarations.information.model.actionbuilders.{AuthorisedRequest, ValidatedHeadersRequest}
import uk.gov.hmrc.customs.declarations.information.services._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import util.ApiSubscriptionFieldsTestData.apiSubscriptionFieldsResponse
import util.FakeRequests._
import util.RequestHeaders._
import util.TestData._
import util.XmlOps.stringToXml
import util.{AuthConnectorStubbing, UnitSpec, VersionTestXMLData}

import java.util.UUID
import scala.concurrent.Future

class VersionControllerSpec extends UnitSpec
  with Matchers with MockitoSugar with BeforeAndAfterEach {

  trait SetUp extends AuthConnectorStubbing {

    protected val mockInformationConfigService: InformationConfigService = mock[InformationConfigService]
    when(mockInformationConfigService.informationShutterConfig).thenReturn(InformationShutterConfig(Some(false), Some(false)))
    when(mockInformationConfigService.informationConfig).thenReturn(InformationConfig("url", 1, Seq()))

    protected val mockInformationLogger: InformationLogger = mock[InformationLogger]
    override val mockAuthConnector: AuthConnector = mock[AuthConnector]

    protected val mockApiSubscriptionFieldsConnector: ApiSubscriptionFieldsConnector = mock[ApiSubscriptionFieldsConnector]
    protected val headerValidator = new HeaderValidator(mockInformationLogger)
    protected val mockCdsLogger: CdsLogger = mock[CdsLogger]
    protected val mockErrorResponse: ErrorResponse = mock[ErrorResponse]
    protected val mockResult: Result = mock[Result]
    protected implicit val ec = Helpers.stubControllerComponents().executionContext

    protected val stubHttpResponse = HttpResponse(Status.OK, VersionTestXMLData.validBackendVersionResponse.toString)

    protected val mockVersionConnector: DeclarationVersionConnector = mock[DeclarationVersionConnector]
    protected val customsAuthService = new CustomsAuthService(mockAuthConnector, mockInformationLogger)
    protected val mockDateTimeService: DateTimeService = mock[DateTimeService]
    protected val dateTime = new DateTime()

    protected val stubAuthStatusAction: AuthAction = new AuthAction(customsAuthService, headerValidator, mockInformationLogger)
    protected val stubShutterCheckAction: ShutterCheckAction = new ShutterCheckAction(mockInformationLogger, mockInformationConfigService)
    protected val stubInternalClientIdsCheckAction: InternalClientIdsCheckAction = new InternalClientIdsCheckAction(mockInformationLogger, mockInformationConfigService)
    protected val stubValidateAndExtractHeadersAction: ValidateAndExtractHeadersAction = new ValidateAndExtractHeadersAction(new HeaderValidator(mockInformationLogger))
    protected val stubVersionResponseFilterService: VersionResponseFilterService = new VersionResponseFilterService()
    protected val stubDeclarationVersionService = new DeclarationVersionService(stubVersionResponseFilterService, mockApiSubscriptionFieldsConnector, mockInformationLogger, mockVersionConnector, mockDateTimeService, stubUniqueIdsService)
    protected val stubConversationIdAction = new ConversationIdAction(stubUniqueIdsService, mockInformationLogger)

    protected val controller: VersionController = new VersionController(
      stubShutterCheckAction,
      stubValidateAndExtractHeadersAction,
      stubAuthStatusAction,
      stubConversationIdAction,
      stubInternalClientIdsCheckAction,
      stubDeclarationVersionService,
      Helpers.stubControllerComponents(),
      mockInformationLogger) {}

    protected def awaitSubmitMrn(request: Request[AnyContent], declarationSubmissionChannel: Option[String] = None): Result = {
      controller.list(mrnValue, declarationSubmissionChannel).apply(request)
    }

    protected def submitMrn(request: Request[AnyContent], declarationSubmissionChannel: Option[String] = None): Future[Result] = {
      controller.list(mrnValue, declarationSubmissionChannel).apply(request)
    }

    when(mockVersionConnector.send(any[DateTime], meq[UUID](correlationId.uuid).asInstanceOf[CorrelationId], any[ApiVersion], any[Option[ApiSubscriptionFieldsResponse]], meq[Either[SearchType, Mrn]](Right(mrn)))(any[AuthorisedRequest[_]])).thenReturn(Future.successful(stubHttpResponse))
    when(mockDateTimeService.nowUtc()).thenReturn(dateTime)
    when(mockApiSubscriptionFieldsConnector.getSubscriptionFields(any[ApiSubscriptionKey])(any[ValidatedHeadersRequest[_]], any[HeaderCarrier])).thenReturn(Future.successful(apiSubscriptionFieldsResponse))
  }

  private val errorResultBadgeIdentifier = errorBadRequest("X-Badge-Identifier header is missing or invalid").XmlResult.withHeaders(X_CONVERSATION_ID_HEADER)
  private val errorResultMissingIdentifiers = errorBadRequest("Both X-Submitter-Identifier and X-Badge-Identifier headers are missing").XmlResult.withHeaders(X_CONVERSATION_ID_HEADER)

  private val errorResultMrnTooLong = errorBadRequest("MRN too long").XmlResult.withHeaders(X_CONVERSATION_ID_HEADER)
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
      val result: Future[Result] = submitMrn(ValidCspDeclarationVersionRequestWithDeclarationSubmissionChannel, Some("AuthenticatedPartyOnly"))
      status(result) shouldBe OK
      verifyCspAuthorisationCalled(numberOfTimes = 1)
    }

    "process CSP request when call is authorised for CSP and declarationSubmissionChannel is invalid" in new SetUp() {
      authoriseCsp()

      val result: Future[Result] = submitMrn(ValidCspDeclarationVersionRequestWithInvalidDeclarationSubmissionChannel, Some("AuthenticatedPartyOnly1"))
      status(result) shouldBe BAD_REQUEST
      stringToXml(contentAsString(result)) shouldBe stringToXml("<errorResponse><code>BAD_REQUEST</code><message>Invalid declarationSubmissionChannel parameter</message></errorResponse>")
    }

    "respond with status 200 and conversationId in header for a processed valid CSP request" in new SetUp() {
      authoriseCsp()

      val result: Future[Result] = submitMrn(ValidCspDeclarationRequest)

      status(result) shouldBe OK
      header(X_CONVERSATION_ID_NAME, result) shouldBe Some(conversationIdValue)
    }

    "respond with status 400 and conversationId in header for a request without an Mrn" in new SetUp() {
      authoriseCsp()

      val result: Future[Result] = controller.list("").apply(ValidCspDeclarationRequest)

      status(result) shouldBe BAD_REQUEST
      stringToXml(contentAsString(result)) shouldBe stringToXml("<errorResponse><code>BAD_REQUEST</code><message>Missing MRN parameter</message></errorResponse>")

      header(X_CONVERSATION_ID_NAME, result) shouldBe Some(conversationIdValue)
    }

    "respond with status 400 and conversationId in header for a request with an Mrn containing a space" in new SetUp() {
      authoriseCsp()

      val result: Future[Result] = controller.list("12345678 ").apply(ValidCspDeclarationRequest)

      status(result) shouldBe BAD_REQUEST
      stringToXml(contentAsString(result)) shouldBe stringToXml("<errorResponse><code>CDS60002</code><message>MRN parameter invalid</message></errorResponse>")
      header(X_CONVERSATION_ID_NAME, result) shouldBe Some(conversationIdValue)
    }

    "respond with status 400 for a CSP request with both a missing X-Badge-Identifier and a missing X-Submitter-Identifier" in new SetUp() {
      authoriseCsp()

      val result: Result = awaitSubmitMrn(ValidCspDeclarationRequest.withHeaders(ValidCspDeclarationRequest.headers.remove(X_BADGE_IDENTIFIER_NAME)))
      result shouldBe errorResultMissingIdentifiers
      verifyNoMoreInteractions(mockVersionConnector)
    }

    "respond with status 500 for a CSP request with a missing X-Client-ID" in new SetUp() {
      authoriseCsp()

      val result: Result = awaitSubmitMrn(ValidCspDeclarationRequest.withHeaders(ValidCspDeclarationRequest.headers.remove(X_CLIENT_ID_NAME)))
      status(result) shouldBe INTERNAL_SERVER_ERROR
      verifyNoMoreInteractions(mockVersionConnector)
    }

    "respond with status 400 for a CSP request with an invalid X-Badge-Identifier" in new SetUp() {
      authoriseCsp()

      val result: Result = awaitSubmitMrn(ValidCspDeclarationRequest.withHeaders((ValidHeaders + X_BADGE_IDENTIFIER_HEADER_INVALID_CHARS).toSeq: _*))

      result shouldBe errorResultBadgeIdentifier
      verifyNoMoreInteractions(mockVersionConnector)
    }

    "respond with status 400 for a request with an MRN value that is too long" in new SetUp() {
      authoriseCsp()

      val result: Result = controller.list(invalidMrnTooLong).apply(ValidCspDeclarationRequest.withHeaders(ValidHeaders.toSeq: _*))
      result shouldBe errorResultMrnTooLong
      verifyNoMoreInteractions(mockVersionConnector)
    }

    "respond with status 400 for a request with an MRN value that is too short" in new SetUp() {
      authoriseCsp()

      val result: Result = controller.list("").apply(ValidCspDeclarationRequest.withHeaders(ValidHeaders.toSeq: _*))

      result shouldBe errorResultMissingMrn
      verifyNoMoreInteractions(mockVersionConnector)
    }

    "respond with status 400 for a request with an MRN value that contains a space" in new SetUp() {
      authoriseCsp()

      val result: Result = controller.list("mrn withASpace").apply(ValidCspDeclarationRequest.withHeaders(ValidHeaders.toSeq: _*))

      result shouldBe errorResultCDS60002
      verifyNoMoreInteractions(mockVersionConnector)
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

      stringToXml(contentAsString(result)) shouldBe stringToXml("<errorResponse><code>BAD_REQUEST</code><message>Invalid declarationSubmissionChannel parameter</message></errorResponse>")

    }

    "respond with status 200 and conversationId in header for a processed valid non-CSP request" in new SetUp() {
      unauthoriseCsp()
      authoriseNonCsp(Some(declarantEori))

      val result: Future[Result] = submitMrn(ValidNonCspDeclarationRequest)

      status(result) shouldBe OK
      header(X_CONVERSATION_ID_NAME, result) shouldBe Some(conversationIdValue)
    }

    "respond with status 200 for a non-CSP request with both a missing X-Badge-Identifier and a missing X-Submitter-Identifier" in new SetUp() {
      unauthoriseCsp()
      authoriseNonCsp(Some(declarantEori))

      val result: Result = awaitSubmitMrn(ValidNonCspDeclarationRequest.withHeaders(ValidNonCspDeclarationRequest.headers.remove(X_BADGE_IDENTIFIER_NAME)))
      status(result) shouldBe OK
      verifyNonCspAuthorisationCalled(numberOfTimes = 1)
    }

    "respond with status 500 for a non-CSP request with a missing X-Client-ID" in new SetUp() {
      unauthoriseCsp()
      authoriseNonCsp(Some(declarantEori))

      val result: Result = awaitSubmitMrn(ValidNonCspDeclarationRequest.withHeaders(ValidNonCspDeclarationRequest.headers.remove(X_CLIENT_ID_NAME)))
      status(result) shouldBe INTERNAL_SERVER_ERROR
      verifyNoMoreInteractions(mockVersionConnector)
    }

    "return the Internal Server error when connector returns a 500 " in new SetUp() {
      when(mockVersionConnector.send(any[DateTime],
        meq[UUID](correlationId.uuid).asInstanceOf[CorrelationId],
        any[ApiVersion],
        any[Option[ApiSubscriptionFieldsResponse]],
        meq[Either[SearchType, Mrn]](Right(mrn)))(any[AuthorisedRequest[_]]))
        .thenReturn(Future.failed(emulatedServiceFailure))

      authoriseCsp()

      val result: Result = awaitSubmitMrn(ValidCspDeclarationRequest)

      result.header.status shouldBe INTERNAL_SERVER_ERROR
    }
  }
}
