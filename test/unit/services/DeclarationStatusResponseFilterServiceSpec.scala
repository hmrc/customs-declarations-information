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

package unit.services

import org.joda.time.{DateTime, DateTimeZone}
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.customs.declarations.information.logging.InformationLogger
import uk.gov.hmrc.customs.declarations.information.services.{InformationConfigService, StatusResponseFilterService}
import uk.gov.hmrc.play.test.UnitSpec
import util.StatusTestXMLData.validBackendStatusResponse

import scala.xml.NodeSeq

//TODO to be completed once xml transformation is implemented
class DeclarationStatusResponseFilterServiceSpec extends UnitSpec with MockitoSugar {

  trait SetUp {

    val mockInformationLogger: InformationLogger = mock[InformationLogger]
    val mockInformationConfigService: InformationConfigService = mock[InformationConfigService]

    val service = new StatusResponseFilterService(mockInformationLogger, mockInformationConfigService)

    def createStatusResponseWithAllValues(): NodeSeq = service.transform(validBackendStatusResponse)
  }

}
