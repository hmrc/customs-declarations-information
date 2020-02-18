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

package unit.xml

import org.joda.time.{DateTime, DateTimeZone}
import org.mockito.Mockito.when
import org.scalatest.Assertion
import org.scalatestplus.mockito.MockitoSugar
import play.api.Configuration
import play.api.mvc.AnyContentAsEmpty
import play.api.test.Helpers
import uk.gov.hmrc.customs.declarations.information.model.actionbuilders.AuthorisedRequest
import uk.gov.hmrc.customs.declarations.information.xml.BackendPayloadCreator
import uk.gov.hmrc.play.test.UnitSpec
import util.ApiSubscriptionFieldsTestData.apiSubscriptionFieldsResponse
import util.TestData._
import util.XmlValidationService

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
  private val payloadCreator = new BackendPayloadCreator()

  "BackendPayloadCreator" should {
    implicit val implicitAr: AuthorisedRequest[AnyContentAsEmpty.type] = TestCspAuthorisedRequest
    def createMrnPayload(): NodeSeq = payloadCreator.create(correlationId, dateTime, mrn, Some(apiSubscriptionFieldsResponse))
    def createDucrPayload(): NodeSeq = payloadCreator.create(correlationId, dateTime, ducr, Some(apiSubscriptionFieldsResponse))
    def createUcrPayload(): NodeSeq = payloadCreator.create(correlationId, dateTime, ucr, Some(apiSubscriptionFieldsResponse))
    def createInventoryReferencePayload(): NodeSeq = payloadCreator.create(correlationId, dateTime, inventoryReference, Some(apiSubscriptionFieldsResponse))

    "sample MRN request passes schema validation" in {
      validateAgainstSchema(createMrnPayload())
    }

    "sample DUCR request passes schema validation" in {
      validateAgainstSchema(createDucrPayload())
    }

    "sample UCR request passes schema validation" in {
      validateAgainstSchema(createUcrPayload())
    }

    "sample IR request passes schema validation" in {
      validateAgainstSchema(createInventoryReferencePayload())
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

  private def validateAgainstSchema(xml: NodeSeq): Assertion = {
    val mockConfiguration = mock[Configuration]
    val propertyName: String = "xsd.locations.statusqueryresponse"
    val xsdLocations: Seq[String] = Seq("/xml/backend_schemas/request/queryDeclarationStatusRequest.xsd")

    when(mockConfiguration.getOptional[Seq[String]](propertyName)).thenReturn(Some(xsdLocations))
    when(mockConfiguration.getOptional[Int]("xml.max-errors")).thenReturn(None)

    val xmlValidationService = new XmlValidationService(mockConfiguration, schemaPropertyName = propertyName) {}

    await(xmlValidationService.validate(xml)) should be(())
  }
}
