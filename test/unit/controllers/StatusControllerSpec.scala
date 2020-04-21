/*
 * Copyright 2020 HM Revenue & Customs
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

import java.util.UUID

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
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse.errorBadRequest
import uk.gov.hmrc.customs.api.common.logging.CdsLogger
import uk.gov.hmrc.customs.declarations.information.connectors.{ApiSubscriptionFieldsConnector, DeclarationStatusConnector}
import uk.gov.hmrc.customs.declarations.information.controllers.StatusController
import uk.gov.hmrc.customs.declarations.information.controllers.actionBuilders.{AuthAction, ConversationIdAction, HeaderValidator, ValidateAndExtractHeadersAction}
import uk.gov.hmrc.customs.declarations.information.logging.InformationLogger
import uk.gov.hmrc.customs.declarations.information.model._
import uk.gov.hmrc.customs.declarations.information.model.actionbuilders.{AuthorisedRequest, ValidatedHeadersRequest}
import uk.gov.hmrc.customs.declarations.information.services._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.test.UnitSpec
import util.ApiSubscriptionFieldsTestData.apiSubscriptionFieldsResponse
import util.FakeRequests._
import util.RequestHeaders._
import util.TestData._
import util.{AuthConnectorStubbing, StatusTestXMLData}

import scala.concurrent.Future
import scala.xml.NodeSeq

class StatusControllerSpec extends UnitSpec
  with Matchers with MockitoSugar with BeforeAndAfterEach {

  trait SetUp extends AuthConnectorStubbing {
    override val mockAuthConnector: AuthConnector = mock[AuthConnector]

    protected val mockStatusResponseFilterService: StatusResponseFilterService = mock[StatusResponseFilterService]
    protected val mockApiSubscriptionFieldsConnector: ApiSubscriptionFieldsConnector = mock[ApiSubscriptionFieldsConnector]
    protected val mockInformationLogger: InformationLogger = mock[InformationLogger]
    protected val headerValidator = new HeaderValidator(mockInformationLogger)
    protected val mockCdsLogger: CdsLogger = mock[CdsLogger]
    protected val mockErrorResponse: ErrorResponse = mock[ErrorResponse]
    protected val mockResult: Result = mock[Result]
    protected val mockInformationConfigService: InformationConfigService = mock[InformationConfigService]
    protected implicit val ec = Helpers.stubControllerComponents().executionContext

    protected val stubHttpResponse = HttpResponse(responseStatus = Status.OK, responseJson = None, responseString = Some(StatusTestXMLData.validBackendStatusResponse.toString))

    protected val mockStatusConnector: DeclarationStatusConnector = mock[DeclarationStatusConnector]
    protected val customsAuthService = new CustomsAuthService(mockAuthConnector, mockInformationLogger)
    protected val mockDateTimeService: DateTimeService = mock[DateTimeService]
    protected val dateTime = new DateTime()

    protected val stubAuthStatusAction: AuthAction = new AuthAction(customsAuthService, headerValidator, mockInformationLogger)
    protected val stubValidateAndExtractHeadersAction: ValidateAndExtractHeadersAction = new ValidateAndExtractHeadersAction(new HeaderValidator(mockInformationLogger), mockInformationLogger)
    protected val stubDeclarationStatusService = new DeclarationStatusService(mockStatusResponseFilterService,
      mockApiSubscriptionFieldsConnector, mockInformationLogger, mockStatusConnector, mockDateTimeService, stubUniqueIdsService)
    protected val stubConversationIdAction = new ConversationIdAction(stubUniqueIdsService, mockInformationLogger)

    protected val controller: StatusController = new StatusController(
      stubValidateAndExtractHeadersAction,
      stubAuthStatusAction,
      stubConversationIdAction,
      stubDeclarationStatusService,
      Helpers.stubControllerComponents(),
      mockInformationLogger) {}

    protected def awaitSubmitMrn(request: Request[AnyContent]): Result = {
      controller.getByMrn(mrnValue).apply(request)
    }

    protected def submitMrn(request: Request[AnyContent]): Future[Result] = {
      controller.getByMrn(mrnValue).apply(request)
    }

    when(mockStatusConnector.send(any[DateTime], meq[UUID](correlationId.uuid).asInstanceOf[CorrelationId], any[ApiVersion], any[Option[ApiSubscriptionFieldsResponse]], meq[SearchType](mrn).asInstanceOf[SearchType])(any[AuthorisedRequest[_]])).thenReturn(Future.successful(stubHttpResponse))
    when(mockDateTimeService.nowUtc()).thenReturn(dateTime)
    when(mockApiSubscriptionFieldsConnector.getSubscriptionFields(any[ApiSubscriptionKey])(any[ValidatedHeadersRequest[_]], any[HeaderCarrier])).thenReturn(Future.successful(apiSubscriptionFieldsResponse))
    when(mockStatusResponseFilterService.transform(any[NodeSeq])).thenReturn(<xml>some xml</xml>)
  }

  private val errorResultBadgeIdentifier = errorBadRequest("X-Badge-Identifier header is missing or invalid").XmlResult.withHeaders(X_CONVERSATION_ID_HEADER)
  private val errorResultMissingIdentifiers = errorBadRequest("Both X-Submitter-Identifier and X-Badge-Identifier headers are missing").XmlResult.withHeaders(X_CONVERSATION_ID_HEADER)
  private val errorResultInvalidSearch = errorBadRequest("Invalid Search").XmlResult.withHeaders(X_CONVERSATION_ID_HEADER)

  "Declaration Status Controller for MRN queries" should {
    "process CSP request when call is authorised for CSP" in new SetUp() {
      authoriseCsp()

      awaitSubmitMrn(ValidCspDeclarationStatusRequest)

      verifyCspAuthorisationCalled(numberOfTimes = 1)
    }

    "respond with status 200 and conversationId in header for a processed valid CSP request" in new SetUp() {
      authoriseCsp()

      val result: Future[Result] = submitMrn(ValidCspDeclarationStatusRequest)

      status(result) shouldBe OK
      header(X_CONVERSATION_ID_NAME, result) shouldBe Some(conversationIdValue)
    }

    "respond with status 404 and conversationId in header for a request without an Mrn" in new SetUp() {
      authoriseCsp()

      val result: Future[Result] = controller.getByMrn("").apply(ValidCspDeclarationStatusRequest)

      status(result) shouldBe NOT_FOUND
      header(X_CONVERSATION_ID_NAME, result) shouldBe Some(conversationIdValue)
    }

    "respond with status 404 and conversationId in header for a request with an Mrn containing a space" in new SetUp() {
      authoriseCsp()

      val result: Future[Result] = controller.getByMrn("12345678 ").apply(ValidCspDeclarationStatusRequest)

      status(result) shouldBe NOT_FOUND
      header(X_CONVERSATION_ID_NAME, result) shouldBe Some(conversationIdValue)
    }

    "respond with status 400 for a CSP request with both a missing X-Badge-Identifier and a missing X-Submitter-Identifier" in new SetUp() {
      authoriseCsp()

      val result: Result = awaitSubmitMrn(ValidCspDeclarationStatusRequest.withHeaders(ValidCspDeclarationStatusRequest.headers.remove(X_BADGE_IDENTIFIER_NAME)))
      result shouldBe errorResultMissingIdentifiers
      verifyNoMoreInteractions(mockStatusConnector)
    }

    "respond with status 500 for a CSP request with a missing X-Client-ID" in new SetUp() {
      authoriseCsp()

      val result: Result = awaitSubmitMrn(ValidCspDeclarationStatusRequest.withHeaders(ValidCspDeclarationStatusRequest.headers.remove(X_CLIENT_ID_NAME)))
      status(result) shouldBe INTERNAL_SERVER_ERROR
      verifyNoMoreInteractions(mockStatusConnector)
    }

    "respond with status 400 for a CSP request with an invalid X-Badge-Identifier" in new SetUp() {
      authoriseCsp()

      val result: Result = awaitSubmitMrn(ValidCspDeclarationStatusRequest.withHeaders((ValidHeaders + X_BADGE_IDENTIFIER_HEADER_INVALID_CHARS).toSeq: _*))

      result shouldBe errorResultBadgeIdentifier
      verifyNoMoreInteractions(mockStatusConnector)
    }

    "respond with status 400 for a request with an invalid MRN value" in new SetUp() {
      authoriseCsp()

      val result: Result = controller.getByMrn(invalidMrnTooLong).apply(ValidCspDeclarationStatusRequest.withHeaders(ValidHeaders.toSeq: _*))

      result shouldBe errorResultInvalidSearch
      verifyNoMoreInteractions(mockStatusConnector)
    }

    "respond with status 400 for a request with an invalid DUCR value" in new SetUp() {
      authoriseCsp()

      val result: Result = controller.getByDucr(invalidDucrTooLong).apply(ValidCspDeclarationStatusRequest.withHeaders(ValidHeaders.toSeq: _*))

      result shouldBe errorResultInvalidSearch
      verifyNoMoreInteractions(mockStatusConnector)
    }

    "respond with status 400 for a request with an invalid UCR value" in new SetUp() {
      authoriseCsp()

      val result: Result = controller.getByUcr(invalidUcrTooLong).apply(ValidCspDeclarationStatusRequest.withHeaders(ValidHeaders.toSeq: _*))

      result shouldBe errorResultInvalidSearch
      verifyNoMoreInteractions(mockStatusConnector)
    }

    "respond with status 400 for a request with an invalid InventoryReference value" in new SetUp() {
      authoriseCsp()

      val result: Result = controller.getByInventoryReference(invalidInventoryReferenceTooLong).apply(ValidCspDeclarationStatusRequest.withHeaders(ValidHeaders.toSeq: _*))

      result shouldBe errorResultInvalidSearch
      verifyNoMoreInteractions(mockStatusConnector)
    }

    "process non-CSP request when call is authorised for non-CSP" in new SetUp() {
      unauthoriseCsp()
      authoriseNonCsp(Some(declarantEori))

      awaitSubmitMrn(ValidNonCspDeclarationStatusRequest)

      verifyNonCspAuthorisationCalled(numberOfTimes = 1)
    }

    "respond with status 200 and conversationId in header for a processed valid non-CSP request" in new SetUp() {
      unauthoriseCsp()
      authoriseNonCsp(Some(declarantEori))

      val result: Future[Result] = submitMrn(ValidNonCspDeclarationStatusRequest)

      status(result) shouldBe OK
      header(X_CONVERSATION_ID_NAME, result) shouldBe Some(conversationIdValue)
    }

    "respond with status 200 for a non-CSP request with both a missing X-Badge-Identifier and a missing X-Submitter-Identifier" in new SetUp() {
      unauthoriseCsp()
      authoriseNonCsp(Some(declarantEori))

      val result: Result = awaitSubmitMrn(ValidNonCspDeclarationStatusRequest.withHeaders(ValidNonCspDeclarationStatusRequest.headers.remove(X_BADGE_IDENTIFIER_NAME)))
      status(result) shouldBe OK
      verifyNonCspAuthorisationCalled(numberOfTimes = 1)
    }

    "respond with status 500 for a non-CSP request with a missing X-Client-ID" in new SetUp() {
      unauthoriseCsp()
      authoriseNonCsp(Some(declarantEori))

      val result: Result = awaitSubmitMrn(ValidNonCspDeclarationStatusRequest.withHeaders(ValidNonCspDeclarationStatusRequest.headers.remove(X_CLIENT_ID_NAME)))
      status(result) shouldBe INTERNAL_SERVER_ERROR
      verifyNoMoreInteractions(mockStatusConnector)
    }

    "return the Internal Server error when connector returns a 500 " in new SetUp() {
      when(mockStatusConnector.send(any[DateTime],
        meq[UUID](correlationId.uuid).asInstanceOf[CorrelationId],
        any[ApiVersion],
        any[Option[ApiSubscriptionFieldsResponse]],
        meq[SearchType](mrn).asInstanceOf[SearchType])(any[AuthorisedRequest[_]]))
        .thenReturn(Future.failed(emulatedServiceFailure))

      authoriseCsp()

      val result: Result = awaitSubmitMrn(ValidCspDeclarationStatusRequest)

      result.header.status shouldBe INTERNAL_SERVER_ERROR
    }
  }

  "Declaration Status Controller for DUCR queries" should {
    "process CSP request when call is authorised for CSP" in new SetUp() {
      when(mockStatusConnector.send(any[DateTime], meq[UUID](correlationId.uuid).asInstanceOf[CorrelationId], any[ApiVersion], any[Option[ApiSubscriptionFieldsResponse]], meq[SearchType](ducr).asInstanceOf[SearchType])(any[AuthorisedRequest[_]])).thenReturn(Future.successful(stubHttpResponse))
      authoriseCsp()

      await(controller.getByDucr(ducrValue).apply(ValidCspDeclarationStatusRequest))

      verifyCspAuthorisationCalled(numberOfTimes = 1)
    }

    "process non-CSP request when call is authorised for non-CSP" in new SetUp() {
      when(mockStatusConnector.send(any[DateTime], meq[UUID](correlationId.uuid).asInstanceOf[CorrelationId], any[ApiVersion], any[Option[ApiSubscriptionFieldsResponse]], meq[SearchType](ducr).asInstanceOf[SearchType])(any[AuthorisedRequest[_]])).thenReturn(Future.successful(stubHttpResponse))
      unauthoriseCsp()
      authoriseNonCsp(Some(declarantEori))

      await(controller.getByDucr(ducrValue).apply(ValidNonCspDeclarationStatusRequest))

      verifyNonCspAuthorisationCalled(numberOfTimes = 1)
    }
  }

  "Declaration Status Controller for UCR queries" should {
    "process CSP request when call is authorised for CSP" in new SetUp() {
      when(mockStatusConnector.send(any[DateTime], meq[UUID](correlationId.uuid).asInstanceOf[CorrelationId], any[ApiVersion], any[Option[ApiSubscriptionFieldsResponse]], meq[SearchType](ucr).asInstanceOf[SearchType])(any[AuthorisedRequest[_]])).thenReturn(Future.successful(stubHttpResponse))
      authoriseCsp()

      await(controller.getByUcr(ucrValue).apply(ValidCspDeclarationStatusRequest))

      verifyCspAuthorisationCalled(numberOfTimes = 1)
    }

    "process non-CSP request when call is authorised for non-CSP" in new SetUp() {
      when(mockStatusConnector.send(any[DateTime], meq[UUID](correlationId.uuid).asInstanceOf[CorrelationId], any[ApiVersion], any[Option[ApiSubscriptionFieldsResponse]], meq[SearchType](ucr).asInstanceOf[SearchType])(any[AuthorisedRequest[_]])).thenReturn(Future.successful(stubHttpResponse))
      unauthoriseCsp()
      authoriseNonCsp(Some(declarantEori))

     await(controller.getByUcr(ucrValue).apply(ValidNonCspDeclarationStatusRequest))

      verifyNonCspAuthorisationCalled(numberOfTimes = 1)
    }
  }

  "Declaration Status Controller for inventory-reference queries" should {
    "process CSP request when call is authorised for CSP" in new SetUp() {
      when(mockStatusConnector.send(any[DateTime], meq[UUID](correlationId.uuid).asInstanceOf[CorrelationId], any[ApiVersion], any[Option[ApiSubscriptionFieldsResponse]], meq[SearchType](inventoryReference).asInstanceOf[SearchType])(any[AuthorisedRequest[_]])).thenReturn(Future.successful(stubHttpResponse))
      authoriseCsp()

      await(controller.getByInventoryReference(inventoryReferenceValue).apply(ValidCspDeclarationStatusRequest))

      verifyCspAuthorisationCalled(numberOfTimes = 1)
    }

    "process non-CSP request when call is authorised for non-CSP" in new SetUp() {
      when(mockStatusConnector.send(any[DateTime], meq[UUID](correlationId.uuid).asInstanceOf[CorrelationId], any[ApiVersion], any[Option[ApiSubscriptionFieldsResponse]], meq[SearchType](inventoryReference).asInstanceOf[SearchType])(any[AuthorisedRequest[_]])).thenReturn(Future.successful(stubHttpResponse))
      unauthoriseCsp()
      authoriseNonCsp(Some(declarantEori))

      await(controller.getByInventoryReference(inventoryReferenceValue).apply(ValidNonCspDeclarationStatusRequest))

      verifyNonCspAuthorisationCalled(numberOfTimes = 1)
    }
  }

}
