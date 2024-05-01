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
import play.api.test.Helpers
import uk.gov.hmrc.customs.api.common.xml.ValidateXmlAgainstSchema
import uk.gov.hmrc.customs.api.common.xml.ValidateXmlAgainstSchema._
import uk.gov.hmrc.customs.declarations.information.model.AuthorisedRequest
import uk.gov.hmrc.customs.declarations.information.xml.BackendStatusPayloadCreator
import util.ApiSubscriptionFieldsTestData.apiSubscriptionFieldsResponse
import util.TestData._
import util.UnitSpec

import java.time.{LocalDateTime, Month, ZoneId, ZonedDateTime}
import scala.concurrent.ExecutionContext
import scala.xml.NodeSeq

class BackendStatusPayloadCreatorSpec extends UnitSpec {
  implicit val ec: ExecutionContext = Helpers.stubControllerComponents().executionContext

  private val instant = 1496930100000L // 2017-06-08T13:55:00.000Z
  private val dateTime: ZonedDateTime = LocalDateTime.of(2017, Month.JUNE, 8, 13, 55, 0, 0).atZone(ZoneId.of("UTC"))
  private val payloadCreator = new BackendStatusPayloadCreator()

  def xmlStatusValidationService: ValidateXmlAgainstSchema = new ValidateXmlAgainstSchema(getSchema("/api/conf/1.0/schemas/wco/declaration/queryDeclarationStatusRequest.xsd").get)

  "BackendStatusPayloadCreator" should {
    implicit val implicitAr: AuthorisedRequest[AnyContentAsEmpty.type] = TestCspAuthorisedRequest

    def createMrnPayload(): NodeSeq = payloadCreator.create(conversationId, correlationId, dateTime, mrn, Some(apiSubscriptionFieldsResponse))

    def createDucrPayload(): NodeSeq = payloadCreator.create(conversationId, correlationId, dateTime, ducr, Some(apiSubscriptionFieldsResponse))

    def createUcrPayload(): NodeSeq = payloadCreator.create(conversationId, correlationId, dateTime, ucr, Some(apiSubscriptionFieldsResponse))

    def createInventoryReferencePayload(): NodeSeq = payloadCreator.create(conversationId, correlationId, dateTime, inventoryReference, Some(apiSubscriptionFieldsResponse))

    "sample MRN request passes schema validation" in {
      xmlStatusValidationService.validate(createMrnPayload()) should be(true)
    }

    "sample DUCR request passes schema validation" in {
      xmlStatusValidationService.validate(createDucrPayload()) should be(true)
    }

    "sample UCR request passes schema validation" in {
      xmlStatusValidationService.validate(createUcrPayload()) should be(true)
    }

    "sample IR request passes schema validation" in {
      xmlStatusValidationService.validate(createInventoryReferencePayload()) should be(true)
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
