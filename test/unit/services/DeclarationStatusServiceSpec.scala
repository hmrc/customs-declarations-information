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

package unit.services

import java.util.UUID
import org.joda.time.DateTime
import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.Mockito.{reset, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.Helpers
import play.mvc.Http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, NOT_FOUND}
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse.{NotFoundCode, errorInternalServerError}
import uk.gov.hmrc.customs.declarations.information.connectors.{ApiSubscriptionFieldsConnector, DeclarationStatusConnector, Non2xxResponseException}
import uk.gov.hmrc.customs.declarations.information.logging.InformationLogger
import uk.gov.hmrc.customs.declarations.information.model._
import uk.gov.hmrc.customs.declarations.information.model.actionbuilders.ActionBuilderModelHelper._
import uk.gov.hmrc.customs.declarations.information.model.actionbuilders.{AuthorisedRequest, HasConversationId}
import uk.gov.hmrc.customs.declarations.information.services._
import uk.gov.hmrc.customs.declarations.information.xml.BackendStatusPayloadCreator
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import util.UnitSpec
import util.ApiSubscriptionFieldsTestData.{apiSubscriptionFieldsResponse, apiSubscriptionFieldsResponseWithEmptyEori, apiSubscriptionFieldsResponseWithNoEori}
import util.TestData.{correlationId, _}

import scala.concurrent.Future
import scala.util.Left

class DeclarationStatusServiceSpec extends UnitSpec with MockitoSugar with BeforeAndAfterEach{
  private val dateTime = new DateTime()
  private val headerCarrier: HeaderCarrier = HeaderCarrier()
  private implicit val vpr: AuthorisedRequest[AnyContentAsEmpty.type] = TestCspAuthorisedRequest

  protected lazy val mockStatusResponseFilterService: StatusResponseFilterService = mock[StatusResponseFilterService]
  protected lazy val mockApiSubscriptionFieldsConnector: ApiSubscriptionFieldsConnector = mock[ApiSubscriptionFieldsConnector]
  protected lazy val mockLogger: InformationLogger = mock[InformationLogger]
  protected lazy val mockDeclarationStatusConnector: DeclarationStatusConnector = mock[DeclarationStatusConnector]
  protected lazy val mockPayloadDecorator: BackendStatusPayloadCreator = mock[BackendStatusPayloadCreator]
  protected lazy val mockDateTimeProvider: DateTimeService = mock[DateTimeService]
  protected lazy val mockHttpResponse: HttpResponse = mock[HttpResponse]
  protected lazy val mockInformationConfigService: InformationConfigService = mock[InformationConfigService]
  protected val searchType = Mrn("theMrn")
  protected lazy val missingEoriResult = errorInternalServerError("Missing authenticated eori in service lookup").XmlResult.withConversationId
  protected implicit val ec = Helpers.stubControllerComponents().executionContext

  trait SetUp {
    when(mockDateTimeProvider.nowUtc()).thenReturn(dateTime)
    when(mockDeclarationStatusConnector.send(any[DateTime], meq[UUID](correlationId.uuid).asInstanceOf[CorrelationId],
      any[ApiVersion], any[Option[ApiSubscriptionFieldsResponse]],
      meq[Either[SearchType, Mrn]](Left(searchType)))(any[AuthorisedRequest[_]]))
      .thenReturn(Future.successful(mockHttpResponse))
    when(mockHttpResponse.body).thenReturn("<xml>some xml</xml>")
    when(mockHttpResponse.headers).thenReturn(any[Map[String, Seq[String]]])
    when(mockStatusResponseFilterService.transform(<xml>backendXml</xml>)).thenReturn(<xml>transformed</xml>)
    when(mockApiSubscriptionFieldsConnector.getSubscriptionFields(any[ApiSubscriptionKey])(any[HasConversationId], any[HeaderCarrier])).thenReturn(Future.successful(apiSubscriptionFieldsResponse))

    protected lazy val service: DeclarationStatusService = new DeclarationStatusService(mockStatusResponseFilterService, mockApiSubscriptionFieldsConnector,
      mockLogger, mockDeclarationStatusConnector, mockDateTimeProvider, stubUniqueIdsService)

    protected def send(vpr: AuthorisedRequest[AnyContentAsEmpty.type] = TestCspAuthorisedRequest, hc: HeaderCarrier = headerCarrier): Either[Result, HttpResponse] = {
      await(service.send(Left(searchType)) (vpr, hc))
    }
  }

  override def beforeEach(): Unit = {
    reset(mockDateTimeProvider, mockDeclarationStatusConnector, mockHttpResponse, mockStatusResponseFilterService)
  }

  "Business Service" should {

    "send xml to connector as CSP" in new SetUp() {
      val result: Either[Result, HttpResponse] = send()
      result.right.get.body shouldBe "<xml>transformed</xml>"
      verify(mockDeclarationStatusConnector).send(dateTime, correlationId, VersionOne, Some(apiSubscriptionFieldsResponse), Left(searchType))(TestCspAuthorisedRequest)
    }

    "send xml to connector as non-CSP" in new SetUp() {
      implicit val nonCspRequest: AuthorisedRequest[AnyContentAsEmpty.type] = TestInternalClientIdsRequest.toNonCspAuthorisedRequest(declarantEori)
      val result: Either[Result, HttpResponse] = send(nonCspRequest)
      result.right.get.body shouldBe "<xml>transformed</xml>"
      verify(mockDeclarationStatusConnector).send(dateTime, correlationId, VersionOne, None, Left(searchType))(nonCspRequest)
    }

    "return 500 with detailed message when call to api-subscription field returns no eori" in new SetUp() {
      when(mockApiSubscriptionFieldsConnector.getSubscriptionFields(any[ApiSubscriptionKey])(any[HasConversationId], any[HeaderCarrier])).thenReturn(Future.successful(apiSubscriptionFieldsResponseWithNoEori))
      val result: Either[Result, HttpResponse] = send()
      result shouldBe Left(missingEoriResult)
    }

    "return 500 with detailed message when call to api-subscription field returns empty eori" in new SetUp() {
      when(mockApiSubscriptionFieldsConnector.getSubscriptionFields(any[ApiSubscriptionKey])(any[HasConversationId], any[HeaderCarrier])).thenReturn(Future.successful(apiSubscriptionFieldsResponseWithEmptyEori))
      val result: Either[Result, HttpResponse] = send()
      result shouldBe Left(missingEoriResult)
    }

    "return 404 error response when backend call fails with 500 and errorCode CDS60001" in new SetUp() {
      when(mockHttpResponse.body).thenReturn("""<cds:errorDetail xmlns:cds="http://www.hmrc.gsi.gov.uk/cds">
                                               |        <cds:timestamp>2016-08-30T14:11:47Z</cds:timestamp>
                                               |        <cds:correlationId>05c97e0f-1336-4850-9008-b992a373f2fg</cds:correlationId>
                                               |        <cds:errorCode>CDS60001</cds:errorCode>
                                               |        <cds:errorMessage>Declaration not found</cds:errorMessage>
                                               |        <cds:source/>
                                               |      </cds:errorDetail>""".stripMargin
        )
      when(mockDeclarationStatusConnector.send(any[DateTime],
        meq[UUID](correlationId.uuid).asInstanceOf[CorrelationId],
        any[ApiVersion],
        any[Option[ApiSubscriptionFieldsResponse]],
        meq[Either[SearchType, Mrn]](Left(searchType)))(any[AuthorisedRequest[_]])).thenReturn(Future.failed(new Non2xxResponseException(mockHttpResponse, 500)))
      val result: Either[Result, HttpResponse] = send()

      result shouldBe Left(ErrorResponse(NOT_FOUND, "CDS60001", "Declaration not found").XmlResult.withConversationId)
    }

    "return 400 error response when backend call fails with 500 and errorCode CDS60002" in new SetUp() {
      when(mockHttpResponse.body).thenReturn("""<cds:errorDetail xmlns:cds="http://www.hmrc.gsi.gov.uk/cds">
                                               |        <cds:timestamp>2016-08-30T14:11:47Z</cds:timestamp>
                                               |        <cds:correlationId>05c97e0f-1336-4850-9008-b992a373f2fg</cds:correlationId>
                                               |        <cds:errorCode>CDS60002</cds:errorCode>
                                               |        <cds:errorMessage>Search parameter invalid</cds:errorMessage>
                                               |        <cds:source/>
                                               |      </cds:errorDetail>""".stripMargin
      )
      when(mockDeclarationStatusConnector.send(any[DateTime],
        meq[UUID](correlationId.uuid).asInstanceOf[CorrelationId],
        any[ApiVersion],
        any[Option[ApiSubscriptionFieldsResponse]],
        meq[Either[SearchType, Mrn]](Left(searchType)))(any[AuthorisedRequest[_]])).thenReturn(Future.failed(new Non2xxResponseException(mockHttpResponse, 500)))
      val result: Either[Result, HttpResponse] = send()

      result shouldBe Left(ErrorResponse(BAD_REQUEST, "CDS60002", "Search parameter invalid").XmlResult.withConversationId)
    }

    "return 500 error response when backend call fails with 500 and errorCode CDS60003" in new SetUp() {
      when(mockHttpResponse.body).thenReturn("""<cds:errorDetail xmlns:cds="http://www.hmrc.gsi.gov.uk/cds">
                                               |        <cds:timestamp>2016-08-30T14:11:47Z</cds:timestamp>
                                               |        <cds:correlationId>05c97e0f-1336-4850-9008-b992a373f2fg</cds:correlationId>
                                               |        <cds:errorCode>CDS60003</cds:errorCode>
                                               |        <cds:errorMessage>Internal server error</cds:errorMessage>
                                               |        <cds:source/>
                                               |      </cds:errorDetail>""".stripMargin
      )
      when(mockDeclarationStatusConnector.send(any[DateTime],
        meq[UUID](correlationId.uuid).asInstanceOf[CorrelationId],
        any[ApiVersion],
        any[Option[ApiSubscriptionFieldsResponse]],
        meq[Either[SearchType, Mrn]](Left(searchType)))(any[AuthorisedRequest[_]])).thenReturn(Future.failed(new Non2xxResponseException(mockHttpResponse, 500)))
      val result: Either[Result, HttpResponse] = send()

      result shouldBe Left(ErrorResponse(INTERNAL_SERVER_ERROR, "CDS60003", "Internal server error").XmlResult.withConversationId)
    }

    "return 500 error response when backend call fails with 403" in new SetUp() {
      when(mockDeclarationStatusConnector.send(any[DateTime],
        meq[UUID](correlationId.uuid).asInstanceOf[CorrelationId],
        any[ApiVersion],
        any[Option[ApiSubscriptionFieldsResponse]],
        meq[Either[SearchType, Mrn]](Left(searchType)))(any[AuthorisedRequest[_]])).thenReturn(Future.failed(new Non2xxResponseException(mockHttpResponse, 403)))
      val result: Either[Result, HttpResponse] = send()

      result shouldBe Left(ErrorResponse.ErrorInternalServerError.XmlResult.withConversationId)
    }

    "return 404 error response when backend call fails with 404" in new SetUp() {
      when(mockDeclarationStatusConnector.send(any[DateTime],
        meq[UUID](correlationId.uuid).asInstanceOf[CorrelationId],
        any[ApiVersion],
        any[Option[ApiSubscriptionFieldsResponse]],
        meq[Either[SearchType, Mrn]](Left(searchType)))(any[AuthorisedRequest[_]])).thenReturn(Future.failed(new Non2xxResponseException(mock[HttpResponse], 404)))
      val result: Either[Result, HttpResponse] = send()

      result shouldBe Left(ErrorResponse(NOT_FOUND, NotFoundCode, "Declaration not found").XmlResult.withConversationId)
    }

    "return 500 error response when backend call fails" in new SetUp() {
      when(mockDeclarationStatusConnector.send(any[DateTime],
        meq[UUID](correlationId.uuid).asInstanceOf[CorrelationId],
        any[ApiVersion],
        any[Option[ApiSubscriptionFieldsResponse]],
        meq[Either[SearchType, Mrn]](Left(searchType)))(any[AuthorisedRequest[_]])).thenReturn(Future.failed(emulatedServiceFailure))
      val result: Either[Result, HttpResponse] = send()

      result shouldBe Left(ErrorResponse.ErrorInternalServerError.XmlResult.withConversationId)
    }
  }
}
