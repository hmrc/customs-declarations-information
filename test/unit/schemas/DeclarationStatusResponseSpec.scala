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

package unit.schemas

import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.Configuration
import play.api.test.Helpers
import uk.gov.hmrc.play.test.UnitSpec
import util.{StatusTestXMLData, XmlValidationService}

import scala.xml.{Elem, Node, SAXException}

//TODO update depending on any response changes
class DeclarationStatusResponseSpec extends UnitSpec with MockitoSugar with BeforeAndAfterEach {

  protected val mockConfiguration = mock[Configuration]
  protected val mockXml = mock[Node]

  protected val propertyName: String = "xsd.locations.statusqueryresponse"

  protected val xsdLocations: Seq[String] = Seq("/api/conf/1.0/schemas/wco/declaration/declarationInformationRetrievalStatusResponse.xsd")
  protected implicit val ec = Helpers.stubControllerComponents().executionContext
  
  def xmlValidationService: XmlValidationService = new XmlValidationService(mockConfiguration, schemaPropertyName = propertyName) {}

  override protected def beforeEach() {
    reset(mockConfiguration)
    when(mockConfiguration.getOptional[Seq[String]](propertyName)).thenReturn(Some(xsdLocations))
    when(mockConfiguration.getOptional[Int]("xml.max-errors")).thenReturn(None)
  }

  "A status query response" should {
    "be successfully validated if correct" in {
      val result = await(xmlValidationService.validate(StatusTestXMLData.ValidDeclarationStatusQueryResponseXML))

      result should be(())
    }

    "fail validation if is incorrect" in {
      val caught = intercept[SAXException] {
        await(xmlValidationService.validate(InvalidDeclarationStatusQueryResponseXML))
      }

      caught.getMessage shouldBe "cvc-elt.1.a: Cannot find the declaration of element 'taggie'."

      Option(caught.getException) shouldBe None
    }

    "fail validation if is not filtered" in {
      val caught = intercept[SAXException] {
        await(xmlValidationService.validate(FullDeclarationStatusQueryResponseXML))
      }

      caught.getMessage shouldBe "cvc-elt.1.a: Cannot find the declaration of element 'n1:queryDeclarationStatusResponse'."

      Option(caught.getException) shouldBe None
    }
  }

  private val InvalidDeclarationStatusQueryResponseXML = <taggie>content</taggie>

  private val FullDeclarationStatusQueryResponseXML: Elem =
    <n1:queryDeclarationStatusResponse
      xmlns:od="urn:wco:datamodel:WCO:DEC-DMS:2"
      xmlns:otnds="urn:wco:datamodel:WCO:Response_DS:DMS:2"
      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
      xmlns:ds="urn:wco:datamodel:WCO:Declaration_DS:DMS:2"
      xmlns:n1="http://gov.uk/customs/declarationInformationRetrieval/status/v2"
      xsi:schemaLocation="http://gov.uk/customs/declarationInformationRetrieval/status/v2 queryDeclarationStatusResponse.xsd">
    <n1:responseCommon>
      <n1:processingDate>2001-12-17T09:30:47Z</n1:processingDate>
    </n1:responseCommon>
    <n1:responseDetail>
      <n1:retrieveDeclarationStatusResponse>
        <n1:retrieveDeclarationStatusDetailsList>
          <n1:retrieveDeclarationStatusDetails>
            <n1:Declaration>
              <n1:AcceptanceDateTime>
                <n1:DateTimeString formatCode="304">20190702110757Z</n1:DateTimeString>
              </n1:AcceptanceDateTime>
              <n1:ID>18GB9JLC3CU1LFGVR2</n1:ID>
              <n1:VersionID>1</n1:VersionID>
              <n1:ReceivedDateTime>
                <n1:DateTimeString formatCode="304">20190702110757Z
                </n1:DateTimeString>
              </n1:ReceivedDateTime>
              <n1:GoodsReleasedDateTime>
                <n1:DateTimeString formatCode="304">20190702110757Z
                </n1:DateTimeString>
              </n1:GoodsReleasedDateTime>
              <n1:ROE>6</n1:ROE>
              <n1:ICS>15</n1:ICS>
              <n1:IRC>000</n1:IRC>
            </n1:Declaration>
            <od:Declaration>
              <od:FunctionCode>9</od:FunctionCode>
              <od:TypeCode>IMZ</od:TypeCode>
              <od:GoodsItemQuantity unitCode="NPR">100</od:GoodsItemQuantity>
              <od:TotalPackageQuantity>10</od:TotalPackageQuantity>
              <od:Submitter>
                <od:ID>GB123456789012000</od:ID>
              </od:Submitter>
              <od:GoodsShipment>
                <od:PreviousDocument>
                  <od:ID>18GBAKZ81EQJ2FGVR</od:ID>
                  <od:TypeCode>DCR</od:TypeCode>
                </od:PreviousDocument>
                <od:PreviousDocument>
                  <od:ID>18GBAKZ81EQJ2FGVA</od:ID>
                  <od:TypeCode>MCR</od:TypeCode>
                </od:PreviousDocument>
                <od:PreviousDocument>
                  <od:ID>18GBAKZ81EQJ2FGVB</od:ID>
                  <od:TypeCode>MCR</od:TypeCode>
                </od:PreviousDocument>
                <od:PreviousDocument>
                  <od:ID>18GBAKZ81EQJ2FGVC</od:ID>
                  <od:TypeCode>DCR</od:TypeCode>
                </od:PreviousDocument>
                <od:PreviousDocument>
                  <od:ID>18GBAKZ81EQJ2FGVD</od:ID>
                  <od:TypeCode>MCR</od:TypeCode>
                </od:PreviousDocument>
                <od:PreviousDocument>
                  <od:ID>18GBAKZ81EQJ2FGVE</od:ID>
                  <od:TypeCode>MCR</od:TypeCode>
                </od:PreviousDocument>
                <od:UCR>
                  <od:TraderAssignedReferenceID>20GBAKZ81EQJ2WXYZ</od:TraderAssignedReferenceID>
                </od:UCR>
              </od:GoodsShipment>
            </od:Declaration>
          </n1:retrieveDeclarationStatusDetails>
        </n1:retrieveDeclarationStatusDetailsList>
      </n1:retrieveDeclarationStatusResponse>
    </n1:responseDetail>
  </n1:queryDeclarationStatusResponse>
}
