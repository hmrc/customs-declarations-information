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

package unit.schemas

import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers
import uk.gov.hmrc.customs.api.common.xml.ValidateXmlAgainstSchema
import util.UnitSpec

import scala.xml.{Elem, SAXException}

//TODO update depending on any response changes
class DeclarationStatusResponseSpec extends UnitSpec with MockitoSugar with BeforeAndAfterEach {

  protected implicit val ec = Helpers.stubControllerComponents().executionContext

  import ValidateXmlAgainstSchema._
  val schemaFile = getSchema("/api/conf/1.0/schemas/wco/declaration/DeclarationInformationRetrievalStatusResponse.xsd")
  def xmlValidationService: ValidateXmlAgainstSchema = new ValidateXmlAgainstSchema(schemaFile.get)

  def getFirstValidationException(xml: Elem): SAXException = {
    val result = xmlValidationService.validateWithErrors(xml)
    result.isLeft shouldBe true

    result.left.get.head
  }

  "A status query response" should {
    "be successfully validated if correct" in {
      val result = xmlValidationService.validate(ValidDeclarationStatusQueryResponseXML)

      result should be(true)
    }

    "fail validation if is incorrect" in {
      val caught = getFirstValidationException(InvalidDeclarationStatusQueryResponseXML)

      caught.getMessage shouldBe "cvc-elt.1.a: Cannot find the declaration of element 'taggie'."

      Option(caught.getException) shouldBe None
    }

    "fail validation if is not filtered" in {
      val caught = getFirstValidationException(FullDeclarationStatusQueryResponseXML)

      caught.getMessage shouldBe "cvc-elt.1.a: Cannot find the declaration of element 'n1:queryDeclarationInformationResponse'."

      Option(caught.getException) shouldBe None
    }
  }

  private val InvalidDeclarationStatusQueryResponseXML = <taggie>content</taggie>

  private val ValidDeclarationStatusQueryResponseXML: Elem = {
    <p:DeclarationStatusResponse
    xmlns:p="http://gov.uk/customs/declarationInformationRetrieval/status/v2"
    xmlns:p1="urn:wco:datamodel:WCO:Response_DS:DMS:2"
    xmlns:p2="urn:wco:datamodel:WCO:DEC-DMS:2"
    xmlns:p3="urn:wco:datamodel:WCO:Declaration_DS:DMS:2"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://gov.uk/customs/declarationInformationRetrieval/status/v2 ../schemas/wco/declaration/DeclarationInformationRetrievalStatusResponse.xsd ">

      <p:DeclarationStatusDetails>
        <p:Declaration>
          <p:AcceptanceDateTime>
            <p1:DateTimeString formatCode="304">20190702110757Z</p1:DateTimeString>
          </p:AcceptanceDateTime>
          <p:ID>18GB9JLC3CU1LFGVR2</p:ID>
          <p:VersionID>1</p:VersionID>
          <p:ReceivedDateTime>
            <p:DateTimeString formatCode="304">20190702110757Z</p:DateTimeString>
          </p:ReceivedDateTime>
          <p:GoodsReleasedDateTime>
            <p:DateTimeString formatCode="304">20190702110757Z</p:DateTimeString>
          </p:GoodsReleasedDateTime>
          <p:ROE>6</p:ROE>
          <p:ICS>15</p:ICS>
          <p:IRC>000</p:IRC>
        </p:Declaration>
        <p2:Declaration>
          <p2:FunctionCode>9</p2:FunctionCode>
          <p2:TypeCode>IMZ</p2:TypeCode>
          <p2:GoodsItemQuantity>100</p2:GoodsItemQuantity>
          <p2:TotalPackageQuantity>10</p2:TotalPackageQuantity>
          <p2:Submitter>
            <p2:ID>GB123456789012000</p2:ID>
          </p2:Submitter>
          <p2:GoodsShipment>
            <p2:PreviousDocument>
              <p2:ID>18GBAKZ81EQJ2FGVR</p2:ID>
              <p2:TypeCode>DCR</p2:TypeCode>
            </p2:PreviousDocument>
            <p2:PreviousDocument>
              <p2:ID>18GBAKZ81EQJ2FGVA</p2:ID>
              <p2:TypeCode>MCR</p2:TypeCode>
            </p2:PreviousDocument>
            <p2:UCR>
              <p2:TraderAssignedReferenceID>20GBAKZ81EQJ2WXYZ</p2:TraderAssignedReferenceID>
            </p2:UCR>
          </p2:GoodsShipment>
        </p2:Declaration>
      </p:DeclarationStatusDetails>
    </p:DeclarationStatusResponse>
  }

  private val FullDeclarationStatusQueryResponseXML: Elem = <n1:queryDeclarationInformationResponse
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xmlns:xsd_1="http://trade.core.ecf/messages/2017/03/31/"
  xmlns:n1="http://gov.uk/customs/retrieveDeclarationInformation/v1"
  xmlns:tns="http://cmm.core.ecf/BaseTypes/cmmPartyTypes/trade/2017/02/22/"
  xmlns:tns_1="http://cmm.core.ecf/BaseTypes/cmmServiceTypes/trade/2017/02/22/"
  xmlns:n2="http://cmm.core.ecf/BaseTypes/cmmDeclarationTypes/trade/2017/02/22/"
  xmlns:tns_4="http://cmm.core.ecf/BaseTypes/cmmEnhancementTypes/trade/2017/02/22/"
  xsi:schemaLocation="http://gov.uk/customs/retrieveDeclarationInformation/v1 response_schema.xsd">

    <n1:responseCommon>
      <n1:processingDate>2016-11-30T09:31:00Z</n1:processingDate>
    </n1:responseCommon>
    <n1:responseDetail>
      <n1:declarationStatusResponse>
        <tns_1:request>
          <tns_1:id>12b0a74b-13bb-42a0-b9db-8ba4ada22fd9</tns_1:id>
          <tns_1:timeStamp>2016-11-30T10:28:54.128Z</tns_1:timeStamp>
        </tns_1:request>
        <xsd_1:declaration>
          <tns_4:isCurrent>true</tns_4:isCurrent>
          <tns_4:versionNumber>1</tns_4:versionNumber>
          <tns_4:creationDate>2016-11-22T08:11:30.016Z</tns_4:creationDate>
          <tns_4:isDisplayable>true</tns_4:isDisplayable>
          <n2:totalGrossMass>90.00</n2:totalGrossMass>
          <n2:modeOfEntry>2</n2:modeOfEntry>
          <n2:goodsItemCount>1</n2:goodsItemCount>
          <n2:communicationAddress>hmrcgwid:144b80b0-b46e-4c56-be1a-83b36649ac46:a
            d3a8c50-fc1c-4b81-a56c-bb153aced791:DHL127</n2:communicationAddress>
          <n2:receiveDate>2016-11-22T15:11:30.123Z</n2:receiveDate>
          <n2:reference>523-123456A-B6780</n2:reference>
          <n2:submitterReference>SNST2487851</n2:submitterReference>
          <n2:tradeMovementType>IM</n2:tradeMovementType>
          <n2:type>D</n2:type>
          <n2:goodsCommunityStatus>1</n2:goodsCommunityStatus>
          <n2:loadingListCount>1</n2:loadingListCount>
          <n2:packageCount>3</n2:packageCount>
          <n2:acceptanceDate>2016-11-22T15:11:40.123Z</n2:acceptanceDate>
          <n2:invoiceAmount>13915.37</n2:invoiceAmount>
          <xsd_1:consignmentShipment>
            <xsd_1:customsOffices/>
            <xsd_1:invoice/>
            <xsd_1:tradeTerms/>
            <xsd_1:goodsItems>
              <n2:quotaOrderNumber/>
              <n2:methodOfPayment/>
              <n2:customsReferenceNumber/>
              <n2:valuationMethod/>
              <n2:previousProcedure/>
              <n2:requestedProcedure/>
              <n2:sequenceNumber>1</n2:sequenceNumber>
              <xsd_1:previousDocuments>
                <n2:type>MCR</n2:type>
                <n2:sequenceNumber>1</n2:sequenceNumber>
                <n2:id>2476AB127</n2:id>
              </xsd_1:previousDocuments>
              <xsd_1:ucr/>
              <xsd_1:commodity>
                <n2:sequenceNumber>1</n2:sequenceNumber>
                <n2:classifications>
                  <n2:identifier>12345561</n2:identifier>
                  <n2:sequenceNumber>1</n2:sequenceNumber>
                  <n2:type>TSP</n2:type>
                </n2:classifications>
              </xsd_1:commodity>
            </xsd_1:goodsItems>
          </xsd_1:consignmentShipment>
        </xsd_1:declaration>
      </n1:declarationStatusResponse>
    </n1:responseDetail>
  </n1:queryDeclarationInformationResponse>
}
