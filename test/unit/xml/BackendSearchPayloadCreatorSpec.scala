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

package unit.xml

import play.api.mvc.AnyContentAsEmpty
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.customs.api.common.xml.ValidateXmlAgainstSchema
import uk.gov.hmrc.customs.api.common.xml.ValidateXmlAgainstSchema._
import uk.gov.hmrc.customs.declarations.information.model._
import uk.gov.hmrc.customs.declarations.information.model.actionbuilders.ActionBuilderModelHelper.{ApiVersionRequestOps, InternalClientIdsRequestOps, SearchParametersRequestOps, ValidatedHeadersRequestOps}
import uk.gov.hmrc.customs.declarations.information.model.actionbuilders.{ApiVersionRequest, AuthorisedRequest, ExtractedHeadersImpl, SearchParameters}
import uk.gov.hmrc.customs.declarations.information.xml.BackendSearchPayloadCreator
import util.ApiSubscriptionFieldsTestData.apiSubscriptionFieldsResponse
import util.SearchTestXMLData._
import util.TestData2.conversationIdValue
import util.{ApiSubscriptionFieldsTestData, UnitSpec}
import util.XmlOps.stringToXml

import java.text.SimpleDateFormat
import java.time.{LocalDateTime, Month, ZoneId, ZonedDateTime}
import java.util.UUID
import java.util.UUID.fromString
import scala.concurrent.ExecutionContext
import scala.xml.NodeSeq

class BackendSearchPayloadCreatorSpec extends UnitSpec {
  implicit val ec: ExecutionContext = Helpers.stubControllerComponents().executionContext
//TODO delete when confirmed working
  private val instant = 1496930100000L // 2017-06-08T13:55:00.000Z
  private val localDateTime = LocalDateTime.of(2017, Month.JUNE, 6, 8, 13, 55)
  private val dateTime = ZonedDateTime.of(localDateTime, ZoneId.of("UTC"))
  private val payloadCreator = new BackendSearchPayloadCreator()

  val conversationIdUuid: UUID = fromString(conversationIdValue)
  val conversationId: ConversationId = ConversationId(conversationIdUuid)
  val correlationIdValue = "e61f8eee-812c-4b8f-b193-06aedc60dca2"
  val correlationIdUuid: UUID = fromString(correlationIdValue)
  val correlationId = CorrelationId(correlationIdUuid)
  val mrnValue = "theMrn"
  val mrn = Mrn(mrnValue)
  val TestFakeRequestV1 = FakeRequest().withHeaders(("Accept", "application/vnd.hmrc.1.0+xml"))
  val TestExtractedHeaders = ExtractedHeadersImpl(ApiSubscriptionFieldsTestData.clientId)
  val sdf = new SimpleDateFormat("yyyy-MM-dd")
  val searchParameters = SearchParameters(Some(Eori("GB123456789000")), PartyRole("submitter"), DeclarationCategory("IM"), Some(GoodsLocationCode("BELBELOB4")), Some(DeclarationStatus("all")), Some(sdf.parse("2021-04-01")), Some(sdf.parse("2021-04-04")), Some(2))
  val declarantEoriValue = "ZZ123456789000"
  val declarantEori = Eori(declarantEoriValue)
  val declarationSubmissionChannel = Some(DeclarationSubmissionChannel("AuthenticatedPartyOnly"))
  val searchParametersMandatoryOnly = SearchParameters(None, PartyRole("submitter"), DeclarationCategory("IM"), None, None, None, None, None)
  val searchParametersWithDate = SearchParameters(None, PartyRole("submitter"), DeclarationCategory("IM"), None, None, Some(sdf.parse("2021-04-01")), Some(sdf.parse("2021-04-04")), None)
  val validBadgeIdentifierValue = "BADGEID123"
  val badgeIdentifier: BadgeIdentifier = BadgeIdentifier(validBadgeIdentifierValue)


  def xmlSearchValidationService: ValidateXmlAgainstSchema = new ValidateXmlAgainstSchema(getSchema("/api/conf/1.0/schemas/wco/declaration/retrieveDeclarationSummaryDataRequest.xsd").get)

  "BackendSearchPayloadCreator" should {

    def createSearchPayload(ar: AuthorisedRequest[AnyContentAsEmpty.type]): NodeSeq = payloadCreator.create(conversationId, correlationId, dateTime, mrn, Some(apiSubscriptionFieldsResponse))(ar)

    "non csp search request is created correctly" in {
      val request = ApiVersionRequest(conversationId, VersionOne, TestFakeRequestV1)
        .toValidatedHeadersRequest(TestExtractedHeaders)
        .toInternalClientIdsRequest(None)
        .toSearchParametersRequest(Some(searchParameters))
        .toNonCspAuthorisedRequest(declarantEori)

      val actual = createSearchPayload(request)
      xmlSearchValidationService.validate(actual) should be(true)
      stringToXml(actual) shouldBe stringToXml(validNonCspSearchRequestPayload)
    }

    "non csp search request with all optional elements and declarationSubmissionChannel created correctly" in {

      val request = ApiVersionRequest(conversationId, VersionOne, TestFakeRequestV1)
        .toValidatedHeadersRequest(TestExtractedHeaders)
        .toInternalClientIdsRequest(declarationSubmissionChannel)
        .toSearchParametersRequest(Some(searchParameters))
        .toNonCspAuthorisedRequest(declarantEori)

      val actual = createSearchPayload(request)
      xmlSearchValidationService.validate(actual) should be(true)
      stringToXml(actual) shouldBe stringToXml(validNonCspSearchRequestPayloadWithSubChannel)
    }

    "non csp search request with no optional elements and without declarationSubmissionChannel created correctly" in {

      val request = ApiVersionRequest(conversationId, VersionOne, TestFakeRequestV1)
        .toValidatedHeadersRequest(TestExtractedHeaders)
        .toInternalClientIdsRequest(None)
        .toSearchParametersRequest(Some(searchParametersMandatoryOnly))
        .toNonCspAuthorisedRequest(declarantEori)

      val actual = createSearchPayload(request)
      xmlSearchValidationService.validate(actual) should be(true)
      stringToXml(actual) shouldBe stringToXml(validNonCspSearchRequestPayloadWithMandatoryParametersOnly)
    }

    "non csp search request with dateFrom and dateTo but no optional elements and without declarationSubmissionChannel created correctly" in {

      val request = ApiVersionRequest(conversationId, VersionOne, TestFakeRequestV1)
        .toValidatedHeadersRequest(TestExtractedHeaders)
        .toInternalClientIdsRequest(None)
        .toSearchParametersRequest(Some(searchParametersWithDate))
        .toNonCspAuthorisedRequest(declarantEori)

      val actual = createSearchPayload(request)
      xmlSearchValidationService.validate(actual) should be(true)
      stringToXml(actual) shouldBe stringToXml(validNonCspSearchRequestPayloadWithMandatoryParametersOnlyAndDate)
    }

    "csp version request without badge identifier is created correctly" in {

      val request = ApiVersionRequest(conversationId, VersionOne, TestFakeRequestV1)
        .toValidatedHeadersRequest(TestExtractedHeaders)
        .toInternalClientIdsRequest(declarationSubmissionChannel)
        .toSearchParametersRequest(Some(searchParameters))
        .toCspAuthorisedRequest(Csp(Some(declarantEori), None))

      val actual = createSearchPayload(request)
      xmlSearchValidationService.validate(actual) should be(true)
      stringToXml(actual) shouldBe stringToXml(validCspSearchRequestPayloadWithoutBadge)
    }

    "csp search request with badge identifier and DeclarationSubmissionChannel is created correctly" in {
      val request = ApiVersionRequest(conversationId, VersionOne, TestFakeRequestV1)
        .toValidatedHeadersRequest(TestExtractedHeaders)
        .toInternalClientIdsRequest(declarationSubmissionChannel)
        .toSearchParametersRequest(Some(searchParameters))
        .toCspAuthorisedRequest(Csp(Some(declarantEori), Some(badgeIdentifier)))

      val actual = createSearchPayload(request)
      xmlSearchValidationService.validate(actual) should be(true)
      stringToXml(actual) shouldBe stringToXml(validCspSearchRequestPayload)
    }
  }
}
