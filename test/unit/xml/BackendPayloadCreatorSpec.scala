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
import uk.gov.hmrc.customs.declarations.information.model.actionbuilders.AuthorisedRequest
import uk.gov.hmrc.customs.declarations.information.xml.{BackendStatusPayloadCreator, BackendVersionPayloadCreator}
import util.UnitSpec
import util.ApiSubscriptionFieldsTestData.apiSubscriptionFieldsResponse
import util.TestData._
import uk.gov.hmrc.customs.api.common.xml.ValidateXmlAgainstSchema
import util.VersionTestXMLData.{validCspVerionRequestPayload, validCspVerionRequestPayloadWithDeclarationSubmissionChannel, validCspVerionRequestWithoutBadgePayload, validNonCspVerionRequestPayload, validNonCspVerionRequestPayloadWithDeclarationSubmissionChannel}
import util.XmlOps.stringToXml

import scala.xml.NodeSeq

class BackendPayloadCreatorSpec extends UnitSpec with MockitoSugar {
  implicit val ec = Helpers.stubControllerComponents().executionContext

  private val year = 2017
  private val monthOfYear = 6
  private val dayOfMonth = 8
  private val hourOfDay = 13
  private val minuteOfHour = 55
  private val secondOfMinute = 0
  private val millisOfSecond = 0
  private val dateTime = new DateTime(year, monthOfYear, dayOfMonth, hourOfDay, minuteOfHour, secondOfMinute, millisOfSecond, DateTimeZone.UTC)
  private val statusPayloadCreator = new BackendStatusPayloadCreator()
  private val versionPayloadCreator = new BackendVersionPayloadCreator()

  import ValidateXmlAgainstSchema._
  def xmlValidationService: ValidateXmlAgainstSchema = new ValidateXmlAgainstSchema(getSchema("/xml/backend_schemas/request/queryDeclarationStatusRequest.xsd").get)
  def xmlVersionValidationService: ValidateXmlAgainstSchema = new ValidateXmlAgainstSchema(getSchema("/xml/backend_schemas/request/retrieveDeclarationVersionRequest.xsd").get)

  "BackendStatusPayloadCreator.createVersionPayload" should {
    implicit val implicitAr: AuthorisedRequest[AnyContentAsEmpty.type] = TestCspAuthorisedRequest

    def createVersionPayload(ar: AuthorisedRequest[AnyContentAsEmpty.type]): NodeSeq = versionPayloadCreator.create(conversationId, correlationId, dateTime, Right(mrn), Some(apiSubscriptionFieldsResponse))(ar)

    "sample Version request passes schema validation" in {
      xmlVersionValidationService.validate(createVersionPayload(TestCspAuthorisedRequest)) should be(true)
    }

    "non csp version request is created correctly" in {
      val actual = createVersionPayload(TestNonCspAuthorisedRequest)
      xmlVersionValidationService.validate(actual) should be(true)
      stringToXml(actual) shouldBe stringToXml(validNonCspVerionRequestPayload)
    }

    "non csp version request is with declarationSubmissionChannel created correctly" in {
      val actual = createVersionPayload(TestNonCspAuthorisedRequestWithDeclarationSubmissionChannel)
      xmlVersionValidationService.validate(actual) should be(true)
      stringToXml(actual) shouldBe stringToXml(validNonCspVerionRequestPayloadWithDeclarationSubmissionChannel)
    }

    "csp version request with badge identifier is created correctly" in {
      val actual = createVersionPayload(TestCspAuthorisedRequest)
      xmlVersionValidationService.validate(actual) should be(true)
      stringToXml(actual) shouldBe stringToXml(validCspVerionRequestPayload)
    }

    "csp version request without badge identifier is created correctly" in {
      val actual = createVersionPayload(TestCspWithoutBadgeAuthorisedRequest)
      xmlVersionValidationService.validate(actual) should be(true)
      stringToXml(actual) shouldBe stringToXml(validCspVerionRequestWithoutBadgePayload)
    }

    "csp version request with badge identifier and DeclarationSubmissionChannel is created correctly" in {
      val actual = createVersionPayload(TestCspAuthorisedRequestWithDeclarationSubmissionChannel)
      xmlVersionValidationService.validate(actual) should be(true)
      stringToXml(actual) shouldBe stringToXml(validCspVerionRequestPayloadWithDeclarationSubmissionChannel)
    }

  }

  "BackendStatusPayloadCreator" should {
    implicit val implicitAr: AuthorisedRequest[AnyContentAsEmpty.type] = TestCspAuthorisedRequest
    def createMrnPayload(): NodeSeq = statusPayloadCreator.create(conversationId, correlationId, dateTime, Left(mrn), Some(apiSubscriptionFieldsResponse))
    def createDucrPayload(): NodeSeq = statusPayloadCreator.create(conversationId, correlationId, dateTime, Left(ducr), Some(apiSubscriptionFieldsResponse))
    def createUcrPayload(): NodeSeq = statusPayloadCreator.create(conversationId, correlationId, dateTime, Left(ucr), Some(apiSubscriptionFieldsResponse))
    def createInventoryReferencePayload(): NodeSeq = statusPayloadCreator.create(conversationId, correlationId, dateTime, Left(inventoryReference), Some(apiSubscriptionFieldsResponse))

    "sample MRN request passes schema validation" in {
      xmlValidationService.validate(createMrnPayload()) should be(true)
    }

    "sample DUCR request passes schema validation" in {
      xmlValidationService.validate(createDucrPayload()) should be(true)
    }

    "sample UCR request passes schema validation" in {
      xmlValidationService.validate(createUcrPayload()) should be(true)
    }

    "sample IR request passes schema validation" in {
      xmlValidationService.validate(createInventoryReferencePayload()) should be(true)
    }

    "set the clientID" in {
      val result = createMrnPayload()

      val rd = result \\ "clientID"

      rd.head.text shouldBe "99999999-9999-9999-9999-999999999999"
    }

    "set the conversationID" in {
      val result = createMrnPayload()

      val rd = result \\ "conversationID"

      rd.head.text shouldBe "38400000-8cf0-11bd-b23e-10b96e4ef00d"
    }

    "set the correlationID" in {
      val result = createMrnPayload()

      val rd = result \\ "correlationID"

      rd.head.text shouldBe "e61f8eee-812c-4b8f-b193-06aedc60dca2"
    }

    "set the badgeIdentifier" in {
      val result = createMrnPayload()

      val rd = result \\ "badgeIdentifier"

      rd.head.text shouldBe "BADGEID123"
    }

    "set the dateTimeStamp" in {
      val result = createMrnPayload()

      val rd = result \\ "dateTimeStamp"

      rd.head.text shouldBe "2017-06-08T13:55:00.000Z"
    }

    "set the timeStamp" in {
      val result = createMrnPayload()

      val rd = result \\ "dateTimeStamp"

      rd.head.text shouldBe "2017-06-08T13:55:00.000Z"
    }

    "set the mrn" in {
      val result = createMrnPayload()

      val rd = result \\ "MRN"

      rd.head.text shouldBe "theMrn"
    }

    "set the ducr" in {
      val result = createDucrPayload()

      val rd = result \\ "DUCR"

      rd.head.text shouldBe "theDucr"
    }

    "set the ucr" in {
      val result = createUcrPayload()

      val rd = result \\ "UCR"

      rd.head.text shouldBe "theUcr"
    }

    "set the inventoryReference" in {
      val result = createInventoryReferencePayload()

      val rd = result \\ "InventoryReference"

      rd.head.text shouldBe "theInventoryReference"
    }
  }
}
