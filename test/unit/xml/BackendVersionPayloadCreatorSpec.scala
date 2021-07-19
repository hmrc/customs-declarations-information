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

package unit.xml

import org.joda.time.{DateTime, DateTimeZone}
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.AnyContentAsEmpty
import play.api.test.Helpers
import uk.gov.hmrc.customs.api.common.xml.ValidateXmlAgainstSchema
import uk.gov.hmrc.customs.api.common.xml.ValidateXmlAgainstSchema._
import uk.gov.hmrc.customs.declarations.information.model.{Csp, VersionOne}
import uk.gov.hmrc.customs.declarations.information.model.actionbuilders.ActionBuilderModelHelper.{ApiVersionRequestOps, InternalClientIdsRequestOps, ValidatedHeadersRequestOps}
import uk.gov.hmrc.customs.declarations.information.model.actionbuilders.{ApiVersionRequest, AuthorisedRequest}
import uk.gov.hmrc.customs.declarations.information.xml.BackendVersionPayloadCreator
import util.ApiSubscriptionFieldsTestData.apiSubscriptionFieldsResponse
import util.TestData._
import util.UnitSpec
import util.VersionTestXMLData._
import util.XmlOps.stringToXml

import scala.concurrent.ExecutionContext
import scala.xml.NodeSeq

class BackendVersionPayloadCreatorSpec extends UnitSpec with MockitoSugar {
  implicit val ec: ExecutionContext = Helpers.stubControllerComponents().executionContext

  private val instant = 1496930100000L // 2017-06-08T13:55:00.000Z
  private val dateTime = new DateTime(instant, DateTimeZone.UTC)
  private val payloadCreator = new BackendVersionPayloadCreator()

  def xmlVersionValidationService: ValidateXmlAgainstSchema = new ValidateXmlAgainstSchema(getSchema("/xml/backend_schemas/request/retrieveDeclarationVersionRequest.xsd").get)
  
  "BackendVersionPayloadCreator" should {

    def createVersionPayload(ar: AuthorisedRequest[AnyContentAsEmpty.type]): NodeSeq =  payloadCreator.create(conversationId, correlationId, dateTime, mrn, Some(apiSubscriptionFieldsResponse))(ar)

    "sample Version request passes schema validation" in {
      xmlVersionValidationService.validate(createVersionPayload(TestCspAuthorisedRequest)) should be(true)
    }

    "non csp version request is created correctly" in {
      val request = ApiVersionRequest(conversationId, VersionOne, TestFakeRequestV1)
        .toValidatedHeadersRequest(TestExtractedHeaders)
        .toInternalClientIdsRequest(None)
        .toNonCspAuthorisedRequest(declarantEori)

      val actual = createVersionPayload(request)
      xmlVersionValidationService.validate(actual) should be(true)
      stringToXml(actual) shouldBe stringToXml(validNonCspVersionRequestPayload)
    }

    "non csp version request is with declarationSubmissionChannel created correctly" in {
      val request = ApiVersionRequest(conversationId, VersionOne, TestFakeRequestV1)
        .toValidatedHeadersRequest(TestExtractedHeaders)
        .toInternalClientIdsRequest(declarationSubmissionChannel)
        .toNonCspAuthorisedRequest(declarantEori)

      val actual = createVersionPayload(request)
      xmlVersionValidationService.validate(actual) should be(true)
      stringToXml(actual) shouldBe stringToXml(validNonCspVerionRequestPayloadWithDeclarationSubmissionChannel)
    }

    "csp version request with badge identifier is created correctly" in {
      val request = ApiVersionRequest(conversationId, VersionOne, TestFakeRequestV1)
        .toValidatedHeadersRequest(TestExtractedHeaders)
        .toInternalClientIdsRequest(None)
        .toCspAuthorisedRequest(Csp(Some(declarantEori), Some(badgeIdentifier)))

      val actual = createVersionPayload(request)
      xmlVersionValidationService.validate(actual) should be(true)
      stringToXml(actual) shouldBe stringToXml(validCspVerionRequestPayload)
    }

    "csp version request without badge identifier is created correctly" in {
      val request = ApiVersionRequest(conversationId, VersionOne, TestFakeRequestV1)
        .toValidatedHeadersRequest(TestExtractedHeaders)
        .toInternalClientIdsRequest(None)
        .toCspAuthorisedRequest(Csp(Some(declarantEori), None))

      val actual = createVersionPayload(request)
      xmlVersionValidationService.validate(actual) should be(true)
      stringToXml(actual) shouldBe stringToXml(validCspVerionRequestWithoutBadgePayload)
    }

    "csp version request with badge identifier and DeclarationSubmissionChannel is created correctly" in {
      val request = ApiVersionRequest(conversationId, VersionOne, TestFakeRequestV1)
        .toValidatedHeadersRequest(TestExtractedHeaders)
        .toInternalClientIdsRequest(declarationSubmissionChannel)
        .toCspAuthorisedRequest(Csp(Some(declarantEori), Some(badgeIdentifier)))

      val actual = createVersionPayload(request)
      xmlVersionValidationService.validate(actual) should be(true)
      stringToXml(actual) shouldBe stringToXml(validCspVerionRequestPayloadWithDeclarationSubmissionChannel)
    }
  }
}
