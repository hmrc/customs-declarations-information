/*
 * Copyright 2019 HM Revenue & Customs
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
import uk.gov.hmrc.customs.declarations.information.model.actionbuilders.AuthorisedRequest
import uk.gov.hmrc.customs.declarations.information.xml.MdgPayloadCreator
import uk.gov.hmrc.play.test.UnitSpec
import util.ApiSubscriptionFieldsTestData.apiSubscriptionFieldsResponse
import util.TestData._

import scala.xml.NodeSeq

class MdgPayloadCreatorSpec extends UnitSpec with MockitoSugar {

  private val year = 2017
  private val monthOfYear = 6
  private val dayOfMonth = 8
  private val hourOfDay = 13
  private val minuteOfHour = 55
  private val secondOfMinute = 0
  private val millisOfSecond = 0
  private val dateTime = new DateTime(year, monthOfYear, dayOfMonth, hourOfDay, minuteOfHour, secondOfMinute, millisOfSecond, DateTimeZone.UTC)
  private val payloadCreator = new MdgPayloadCreator()

  "MdgPayloadCreator" should {
    implicit val implicitAr: AuthorisedRequest[AnyContentAsEmpty.type] = TestCspAuthorisedRequest
    def createPayload(): NodeSeq = payloadCreator.create(correlationId, dateTime, mrn, dmirId, apiSubscriptionFieldsResponse)

    "set the clientID" in {
      val result = createPayload()

      val rd = result \\ "clientID"

      rd.head.text shouldBe "327d9145-4965-4d28-a2c5-39dedee50334"
    }

    "set the conversationID" in {
      val result = createPayload()

      val rd = result \\ "conversationID"

      rd.head.text shouldBe "38400000-8cf0-11bd-b23e-10b96e4ef00d"
    }

    "set the correlationID" in {
      val result = createPayload()

      val rd = result \\ "correlationID"

      rd.head.text shouldBe "e61f8eee-812c-4b8f-b193-06aedc60dca2"
    }

    "set the badgeIdentifier" in {
      val result = createPayload()

      val rd = result \\ "badgeIdentifier"

      rd.head.text shouldBe "BADGEID123"
    }

    "set the dateTimeStamp" in {
      val result = createPayload()

      val rd = result \\ "dateTimeStamp"

      rd.head.text shouldBe "2017-06-08T13:55:00.000Z"
    }

    "set the dmirId" in {
      val result = createPayload()

      val rd = result \\ "id"

      rd.head.text shouldBe "1b0a48a8-1259-42c9-9d6a-e797b919eb16"
    }

    "set the timeStamp" in {
      val result = createPayload()

      val rd = result \\ "timeStamp"

      rd.head.text shouldBe "2017-06-08T13:55:00.000Z"
    }

    "set the mrn" in {
      val result = createPayload()

      val rd = result \\ "reference"

      rd.head.text shouldBe "theMrn"
    }
    
  }
  
}
