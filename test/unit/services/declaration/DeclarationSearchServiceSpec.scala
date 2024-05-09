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

package unit.services.declaration

import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.Mockito.{mock, reset, verify, when}
import org.scalatest.BeforeAndAfterEach
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.Helpers
import play.mvc.Http.Status._
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse.errorInternalServerError
import uk.gov.hmrc.customs.declarations.information.config.{InformationConfig, ConfigService}
import uk.gov.hmrc.customs.declarations.information.connectors.{ApiSubscriptionFieldsConnector, DeclarationSearchConnector}
import uk.gov.hmrc.customs.declarations.information.logging.InformationLogger
import uk.gov.hmrc.customs.declarations.information.model._
import ActionBuilderModelHelper._
import uk.gov.hmrc.customs.declarations.information.services.declaration.DeclarationSearchService
import uk.gov.hmrc.customs.declarations.information.services.filter.SearchResponseFilterService
import uk.gov.hmrc.customs.declarations.information.xml.BackendSearchPayloadCreator
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import util.ApiSubscriptionFieldsTestData.{apiSubscriptionFieldsResponse, apiSubscriptionFieldsResponseWithEmptyEori, apiSubscriptionFieldsResponseWithNoEori}
import util.TestData._
import util.UnitSpec

import java.time.{Clock, ZoneId, ZonedDateTime}
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class DeclarationSearchServiceSpec extends UnitSpec with BeforeAndAfterEach {
  private val dateTime = ZonedDateTime.ofInstant(Clock.systemUTC().instant(), ZoneId.of("UTC"))
  private val headerCarrier: HeaderCarrier = HeaderCarrier()
  private implicit val vpr: AuthorisedRequest[AnyContentAsEmpty.type] = TestCspAuthorisedRequest

  protected lazy val mockSearchResponseFilterService: SearchResponseFilterService = mock(classOf[SearchResponseFilterService])
  protected lazy val mockApiSubscriptionFieldsConnector: ApiSubscriptionFieldsConnector = mock(classOf[ApiSubscriptionFieldsConnector])
  implicit protected lazy val mockLogger: InformationLogger = mock(classOf[InformationLogger])
  protected lazy val mockDeclarationSearchConnector: DeclarationSearchConnector = mock(classOf[DeclarationSearchConnector])
  protected lazy val mockPayloadDecorator: BackendSearchPayloadCreator = mock(classOf[BackendSearchPayloadCreator])
  protected lazy val mockHttpResponse: HttpResponse = HttpResponse(OK, "<xml>some xml</xml>")
  protected lazy val mockInformationConfigService: ConfigService = mock(classOf[ConfigService])
  protected lazy val mockInformationConfig: InformationConfig = mock(classOf[InformationConfig])
  protected val mrn = Mrn("theMrn")
  protected lazy val missingEoriResult = errorInternalServerError("Missing authenticated eori in service lookup").XmlResult.withConversationId
  protected implicit val ec: ExecutionContext = Helpers.stubControllerComponents().executionContext

  trait SetUp {
    when(mockDeclarationSearchConnector.send(any[ZonedDateTime], meq[UUID](correlationId.uuid).asInstanceOf[CorrelationId],
      any[ApiVersion], any[Option[ApiSubscriptionFieldsResponse]],
      meq[Mrn](mrn))(any[AuthorisedRequest[_]]))
      .thenReturn(Future.successful(Right(mockHttpResponse)))
    when(mockSearchResponseFilterService.findPathThenTransform(<xml>some xml</xml>)).thenReturn(<xml>transformed</xml>)
    when(mockApiSubscriptionFieldsConnector.getSubscriptionFields(any[ApiSubscriptionKey])(any[HasConversationId], any[HeaderCarrier])).thenReturn(Future.successful(Some(apiSubscriptionFieldsResponse)))
    when(mockInformationConfigService.informationConfig).thenReturn(mockInformationConfig)

    protected lazy val service: DeclarationSearchService = new DeclarationSearchService(mockSearchResponseFilterService, mockApiSubscriptionFieldsConnector,
      mockLogger, mockDeclarationSearchConnector, stubUniqueIdsService, mockInformationConfigService)

    protected def send(vpr: AuthorisedRequest[AnyContentAsEmpty.type] = TestCspAuthorisedRequest, hc: HeaderCarrier = headerCarrier): Either[Result, HttpResponse] = {
      await(service.send(mrn, dateTime)(vpr, hc))
    }
  }

  override def beforeEach(): Unit = {
    reset(mockDeclarationSearchConnector, mockSearchResponseFilterService)
  }

  "Business Service" should {
    "send xml to connector as CSP" in new SetUp() {
      val result: Either[Result, HttpResponse] = send()
      result.toOption.get.body shouldBe "<xml>transformed</xml>"
      verify(mockDeclarationSearchConnector).send(dateTime, correlationId, VersionOne, Some(apiSubscriptionFieldsResponse), mrn)(TestCspAuthorisedRequest)
    }

    "send xml to connector as non-CSP" in new SetUp() {
      implicit val nonCspRequest: AuthorisedRequest[AnyContentAsEmpty.type] = TestInternalClientIdsRequest.toNonCspAuthorisedRequest(declarantEori)
      val result: Either[Result, HttpResponse] = send(nonCspRequest)
      result.toOption.get.body shouldBe "<xml>transformed</xml>"
      verify(mockDeclarationSearchConnector).send(dateTime, correlationId, VersionOne, None, mrn)(nonCspRequest)
    }

    "return 500 with detailed message when call to api-subscription field returns no eori" in new SetUp() {
      when(mockApiSubscriptionFieldsConnector.getSubscriptionFields(any[ApiSubscriptionKey])(any[HasConversationId], any[HeaderCarrier])).thenReturn(Future.successful(Some(apiSubscriptionFieldsResponseWithNoEori)))
      val result: Either[Result, HttpResponse] = send()
      result shouldBe Left(missingEoriResult)
    }

    "return 500 with detailed message when call to api-subscription field returns empty eori" in new SetUp() {
      when(mockApiSubscriptionFieldsConnector.getSubscriptionFields(any[ApiSubscriptionKey])(any[HasConversationId], any[HeaderCarrier])).thenReturn(Future.successful(Some(apiSubscriptionFieldsResponseWithEmptyEori)))
      val result: Either[Result, HttpResponse] = send()
      result shouldBe Left(missingEoriResult)
    }

    "return 404 error response when backend call fails with 500 and errorCode CDS60001" in new SetUp() {
      val declarationsResponseBody =
        """<cds:errorDetail xmlns:cds="http://www.hmrc.gsi.gov.uk/cds">
          |        <cds:timestamp>2016-08-30T14:11:47Z</cds:timestamp>
          |        <cds:correlationId>05c97e0f-1336-4850-9008-b992a373f2fg</cds:correlationId>
          |        <cds:errorCode>CDS60001</cds:errorCode>
          |        <cds:errorMessage>Declaration Not Found</cds:errorMessage>
          |        <cds:source/>
          |      </cds:errorDetail>""".stripMargin


      when(mockDeclarationSearchConnector.send(any[ZonedDateTime],
        meq[UUID](correlationId.uuid).asInstanceOf[CorrelationId],
        any[ApiVersion],
        any[Option[ApiSubscriptionFieldsResponse]],
        meq[Mrn](mrn))(any[AuthorisedRequest[_]])).thenReturn(Future.successful(Left(Non2xxResponseError(INTERNAL_SERVER_ERROR, declarationsResponseBody))))
      val result: Either[Result, HttpResponse] = send()

      result shouldBe Left(ErrorResponse(NOT_FOUND, "CDS60001", "Declaration not found").XmlResult.withConversationId)
    }

    "return 400 error response when backend call fails with 500 and errorCode CDS60002" in new SetUp() {
      val declarationsResponseBody =
        """<cds:errorDetail xmlns:cds="http://www.hmrc.gsi.gov.uk/cds">
          |        <cds:timestamp>2016-08-30T14:11:47Z</cds:timestamp>
          |        <cds:correlationId>05c97e0f-1336-4850-9008-b992a373f2fg</cds:correlationId>
          |        <cds:errorCode>CDS60002</cds:errorCode>
          |        <cds:errorMessage>Search parameter invalid</cds:errorMessage>
          |        <cds:source/>
          |      </cds:errorDetail>""".stripMargin


      when(mockDeclarationSearchConnector.send(any[ZonedDateTime],
        meq[UUID](correlationId.uuid).asInstanceOf[CorrelationId],
        any[ApiVersion],
        any[Option[ApiSubscriptionFieldsResponse]],
        meq[Mrn](mrn))(any[AuthorisedRequest[_]])).thenReturn(Future.successful(Left(Non2xxResponseError(INTERNAL_SERVER_ERROR, declarationsResponseBody))))
      val result: Either[Result, HttpResponse] = send()

      result shouldBe Left(ErrorResponse(BAD_REQUEST, "CDS60002", "MRN parameter invalid").XmlResult.withConversationId)
    }

    "return 500 error response when backend call fails with 500 and errorCode CDS60003" in new SetUp() {
      val declarationsResponseBody =
        """<cds:errorDetail xmlns:cds="http://www.hmrc.gsi.gov.uk/cds">
          |        <cds:timestamp>2016-08-30T14:11:47Z</cds:timestamp>
          |        <cds:correlationId>05c97e0f-1336-4850-9008-b992a373f2fg</cds:correlationId>
          |        <cds:errorCode>CDS60003</cds:errorCode>
          |        <cds:errorMessage>Internal server error</cds:errorMessage>
          |        <cds:source/>
          |      </cds:errorDetail>""".stripMargin

      when(mockDeclarationSearchConnector.send(any[ZonedDateTime],
        meq[UUID](correlationId.uuid).asInstanceOf[CorrelationId],
        any[ApiVersion],
        any[Option[ApiSubscriptionFieldsResponse]],
        meq[Mrn](mrn))(any[AuthorisedRequest[_]])).thenReturn(Future.successful(Left(Non2xxResponseError(INTERNAL_SERVER_ERROR, declarationsResponseBody))))
      val result: Either[Result, HttpResponse] = send()

      result shouldBe Left(ErrorResponse(INTERNAL_SERVER_ERROR, "CDS60003", "Internal server error").XmlResult.withConversationId)
    }

    "return 500 error response when backend call fails with 500 and errorCode CDS60005" in new SetUp() {
      val declarationsResponseBody =
        """<cds:errorDetail xmlns:cds="http://www.hmrc.gsi.gov.uk/cds">
          |        <cds:timestamp>2016-08-30T14:11:47Z</cds:timestamp>
          |        <cds:correlationId>05c97e0f-1336-4850-9008-b992a373f2fg</cds:correlationId>
          |        <cds:errorCode>CDS60005</cds:errorCode>
          |        <cds:errorMessage>Page Number is Out of Bound</cds:errorMessage>
          |        <cds:source/>
          |      </cds:errorDetail>""".stripMargin

      when(mockDeclarationSearchConnector.send(any[ZonedDateTime],
        meq[UUID](correlationId.uuid).asInstanceOf[CorrelationId],
        any[ApiVersion],
        any[Option[ApiSubscriptionFieldsResponse]],
        meq[Mrn](mrn))(any[AuthorisedRequest[_]])).thenReturn(Future.successful(Left(Non2xxResponseError(INTERNAL_SERVER_ERROR, declarationsResponseBody))))
      val result: Either[Result, HttpResponse] = send()

      result shouldBe Left(ErrorResponse(BAD_REQUEST, "CDS60005", "pageNumber parameter out of bounds").XmlResult.withConversationId)
    }

    "return 500 error response when backend call fails with 500 and errorCode CDS60006" in new SetUp() {
      val declarationsResponseBody =
        """<cds:errorDetail xmlns:cds="http://www.hmrc.gsi.gov.uk/cds">
          |        <cds:timestamp>2016-08-30T14:11:47Z</cds:timestamp>
          |        <cds:correlationId>05c97e0f-1336-4850-9008-b992a373f2fg</cds:correlationId>
          |        <cds:errorCode>CDS60006</cds:errorCode>
          |        <cds:errorMessage>Invalid Party Role</cds:errorMessage>
          |        <cds:source/>
          |      </cds:errorDetail>""".stripMargin


      when(mockDeclarationSearchConnector.send(any[ZonedDateTime],
        meq[UUID](correlationId.uuid).asInstanceOf[CorrelationId],
        any[ApiVersion],
        any[Option[ApiSubscriptionFieldsResponse]],
        meq[Mrn](mrn))(any[AuthorisedRequest[_]])).thenReturn(Future.successful(Left(Non2xxResponseError(INTERNAL_SERVER_ERROR, declarationsResponseBody))))
      val result: Either[Result, HttpResponse] = send()

      result shouldBe Left(ErrorResponse(BAD_REQUEST, "CDS60006", "Invalid partyRole parameter").XmlResult.withConversationId)
    }

    "return 500 error response when backend call fails with 500 and errorCode CDS60007" in new SetUp() {
      val declarationsResponseBody =
        """<cds:errorDetail xmlns:cds="http://www.hmrc.gsi.gov.uk/cds">
          |        <cds:timestamp>2016-08-30T14:11:47Z</cds:timestamp>
          |        <cds:correlationId>05c97e0f-1336-4850-9008-b992a373f2fg</cds:correlationId>
          |        <cds:errorCode>CDS60007</cds:errorCode>
          |        <cds:errorMessage>Invalid Declaration Status</cds:errorMessage>
          |        <cds:source/>
          |      </cds:errorDetail>""".stripMargin


      when(mockDeclarationSearchConnector.send(any[ZonedDateTime],
        meq[UUID](correlationId.uuid).asInstanceOf[CorrelationId],
        any[ApiVersion],
        any[Option[ApiSubscriptionFieldsResponse]],
        meq[Mrn](mrn))(any[AuthorisedRequest[_]])).thenReturn(Future.successful(Left(Non2xxResponseError(INTERNAL_SERVER_ERROR, declarationsResponseBody))))
      val result: Either[Result, HttpResponse] = send()

      result shouldBe Left(ErrorResponse(BAD_REQUEST, "CDS60007", "Invalid declarationStatus parameter").XmlResult.withConversationId)
    }

    "return 500 error response when backend call fails with 500 and errorCode CDS60008" in new SetUp() {
      val declarationsResponseBody =
        """<cds:errorDetail xmlns:cds="http://www.hmrc.gsi.gov.uk/cds">
          |        <cds:timestamp>2016-08-30T14:11:47Z</cds:timestamp>
          |        <cds:correlationId>05c97e0f-1336-4850-9008-b992a373f2fg</cds:correlationId>
          |        <cds:errorCode>CDS60008</cds:errorCode>
          |        <cds:errorMessage>Invalid Declaration Category</cds:errorMessage>
          |        <cds:source/>
          |      </cds:errorDetail>""".stripMargin


      when(mockDeclarationSearchConnector.send(any[ZonedDateTime],
        meq[UUID](correlationId.uuid).asInstanceOf[CorrelationId],
        any[ApiVersion],
        any[Option[ApiSubscriptionFieldsResponse]],
        meq[Mrn](mrn))(any[AuthorisedRequest[_]])).thenReturn(Future.successful(Left(Non2xxResponseError(INTERNAL_SERVER_ERROR, declarationsResponseBody))))
      val result: Either[Result, HttpResponse] = send()

      result shouldBe Left(ErrorResponse(BAD_REQUEST, "CDS60008", "Invalid declarationCategory parameter").XmlResult.withConversationId)
    }

    "return 500 error response when backend call fails with 500 and errorCode CDS60009" in new SetUp() {
      val declarationsResponseBody =
        """<cds:errorDetail xmlns:cds="http://www.hmrc.gsi.gov.uk/cds">
          |        <cds:timestamp>2016-08-30T14:11:47Z</cds:timestamp>
          |        <cds:correlationId>05c97e0f-1336-4850-9008-b992a373f2fg</cds:correlationId>
          |        <cds:errorCode>CDS60009</cds:errorCode>
          |        <cds:errorMessage>Invalid Date Range</cds:errorMessage>
          |        <cds:source/>
          |      </cds:errorDetail>""".stripMargin


      when(mockDeclarationSearchConnector.send(any[ZonedDateTime],
        meq[UUID](correlationId.uuid).asInstanceOf[CorrelationId],
        any[ApiVersion],
        any[Option[ApiSubscriptionFieldsResponse]],
        meq[Mrn](mrn))(any[AuthorisedRequest[_]])).thenReturn(Future.successful(Left(Non2xxResponseError(INTERNAL_SERVER_ERROR, declarationsResponseBody))))
      val result: Either[Result, HttpResponse] = send()

      result shouldBe Left(ErrorResponse(BAD_REQUEST, "CDS60009", "Invalid date parameters").XmlResult.withConversationId)
    }

    "return 500 error response when backend call fails with 500 and errorCode CDS60010" in new SetUp() {
      val declarationsResponseBody =
        """<cds:errorDetail xmlns:cds="http://www.hmrc.gsi.gov.uk/cds">
          |        <cds:timestamp>2016-08-30T14:11:47Z</cds:timestamp>
          |        <cds:correlationId>05c97e0f-1336-4850-9008-b992a373f2fg</cds:correlationId>
          |        <cds:errorCode>CDS60010</cds:errorCode>
          |        <cds:errorMessage>Invalid Goods Location Code</cds:errorMessage>
          |        <cds:source/>
          |      </cds:errorDetail>""".stripMargin


      when(mockDeclarationSearchConnector.send(any[ZonedDateTime],
        meq[UUID](correlationId.uuid).asInstanceOf[CorrelationId],
        any[ApiVersion],
        any[Option[ApiSubscriptionFieldsResponse]],
        meq[Mrn](mrn))(any[AuthorisedRequest[_]])).thenReturn(Future.successful(Left(Non2xxResponseError(INTERNAL_SERVER_ERROR, declarationsResponseBody))))
      val result: Either[Result, HttpResponse] = send()

      result shouldBe Left(ErrorResponse(BAD_REQUEST, "CDS60010", "Invalid goodsLocationCode parameter").XmlResult.withConversationId)
    }

    "return 400 error response when backend call fails with 500 and errorCode CDS60011" in new SetUp() {
      val declarationsResponseBody =
        """<cds:errorDetail xmlns:cds="http://www.hmrc.gsi.gov.uk/cds">
          |        <cds:timestamp>2016-08-30T14:11:47Z</cds:timestamp>
          |        <cds:correlationId>05c97e0f-1336-4850-9008-b992a373f2fg</cds:correlationId>
          |        <cds:errorCode>CDS60011</cds:errorCode>
          |        <cds:errorMessage>Invalid Declaration Submission Channel</cds:errorMessage>
          |        <cds:source/>
          |      </cds:errorDetail>""".stripMargin


      when(mockDeclarationSearchConnector.send(any[ZonedDateTime],
        meq[UUID](correlationId.uuid).asInstanceOf[CorrelationId],
        any[ApiVersion],
        any[Option[ApiSubscriptionFieldsResponse]],
        meq[Mrn](mrn))(any[AuthorisedRequest[_]])).thenReturn(Future.successful(Left(Non2xxResponseError(INTERNAL_SERVER_ERROR, declarationsResponseBody))))
      val result: Either[Result, HttpResponse] = send()

      result shouldBe Left(ErrorResponse(BAD_REQUEST, "CDS60011", "Invalid declarationSubmissionChannel parameter").XmlResult.withConversationId)
    }

    "return 400 error response when backend call fails with 500 and errorCode CDS60012" in new SetUp() {
      val declarationsResponseBody =
        """<cds:errorDetail xmlns:cds="http://www.hmrc.gsi.gov.uk/cds">
          |        <cds:timestamp>2016-08-30T14:11:47Z</cds:timestamp>
          |        <cds:correlationId>05c97e0f-1336-4850-9008-b992a373f2fg</cds:correlationId>
          |        <cds:errorCode>CDS60012</cds:errorCode>
          |        <cds:errorMessage>Invalid Page Number</cds:errorMessage>
          |        <cds:source/>
          |      </cds:errorDetail>""".stripMargin


      when(mockDeclarationSearchConnector.send(any[ZonedDateTime],
        meq[UUID](correlationId.uuid).asInstanceOf[CorrelationId],
        any[ApiVersion],
        any[Option[ApiSubscriptionFieldsResponse]],
        meq[Mrn](mrn))(any[AuthorisedRequest[_]])).thenReturn(Future.successful(Left(Non2xxResponseError(INTERNAL_SERVER_ERROR, declarationsResponseBody))))
      val result: Either[Result, HttpResponse] = send()

      result shouldBe Left(ErrorResponse(BAD_REQUEST, "CDS60012", "Invalid pageNumber parameter").XmlResult.withConversationId)
    }

    "return 500 error response when backend call fails with 500 and errorCode not CDS60003" in new SetUp() {
      val declarationsResponseBody =
        """<cds:errorDetail xmlns:cds="http://www.hmrc.gsi.gov.uk/cds">
          |        <cds:timestamp>2016-08-30T14:11:47Z</cds:timestamp>
          |        <cds:correlationId>05c97e0f-1336-4850-9008-b992a373f2fg</cds:correlationId>
          |        <cds:errorCode>an-error-code</cds:errorCode>
          |        <cds:errorMessage>Internal server error</cds:errorMessage>
          |        <cds:source/>
          |      </cds:errorDetail>""".stripMargin


      when(mockDeclarationSearchConnector.send(any[ZonedDateTime],
        meq[UUID](correlationId.uuid).asInstanceOf[CorrelationId],
        any[ApiVersion],
        any[Option[ApiSubscriptionFieldsResponse]],
        meq[Mrn](mrn))(any[AuthorisedRequest[_]])).thenReturn(Future.successful(Left(Non2xxResponseError(INTERNAL_SERVER_ERROR, declarationsResponseBody))))
      val result: Either[Result, HttpResponse] = send()

      result shouldBe Left(ErrorResponse(INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "Internal server error").XmlResult.withConversationId)
    }

    "return 403 error response when backend call fails with 403" in new SetUp() {
      when(mockDeclarationSearchConnector.send(any[ZonedDateTime],
        meq[UUID](correlationId.uuid).asInstanceOf[CorrelationId],
        any[ApiVersion],
        any[Option[ApiSubscriptionFieldsResponse]],
        meq[Mrn](mrn))(any[AuthorisedRequest[_]])).thenReturn(Future.successful(Left(Non2xxResponseError(FORBIDDEN, mockHttpResponse.body))))
      val result: Either[Result, HttpResponse] = send()

      result shouldBe Left(ErrorResponse.ErrorPayloadForbidden.XmlResult.withConversationId)
    }

    "return 500 error response when backend call fails" in new SetUp() {
      when(mockDeclarationSearchConnector.send(any[ZonedDateTime],
        meq[UUID](correlationId.uuid).asInstanceOf[CorrelationId],
        any[ApiVersion],
        any[Option[ApiSubscriptionFieldsResponse]],
        meq[Mrn](mrn))(any[AuthorisedRequest[_]])).thenReturn(Future.successful(Left(UnexpectedError(emulatedServiceFailure))))
      val result: Either[Result, HttpResponse] = send()

      result shouldBe Left(ErrorResponse.ErrorInternalServerError.XmlResult.withConversationId)
    }
  }
}
