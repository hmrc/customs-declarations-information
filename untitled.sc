import scala.xml.XML
import uk.gov.hmrc.http.HttpResponse
import play.api.libs.json.Json

val  xmlBody = <n1:queryDeclarationStatusResponse
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
              <otnds:DateTimeString formatCode="304">20190702110757Z</otnds:DateTimeString>
            </n1:AcceptanceDateTime>
            <n1:ID>18GB9JLC3CU1LFGVR2</n1:ID>
            <n1:VersionID>1</n1:VersionID>
            <n1:ReceivedDateTime>
              <n1:DateTimeString formatCode="304">20190702110757Z</n1:DateTimeString>
            </n1:ReceivedDateTime>
            <n1:GoodsReleasedDateTime>
              <n1:DateTimeString formatCode="304">20190702110757Z</n1:DateTimeString>
            </n1:GoodsReleasedDateTime>
            <n1:ROE>6</n1:ROE>
            <n1:ICS>15</n1:ICS>
            <n1:IRC>000</n1:IRC>
          </n1:Declaration>
          <od:Declaration>
            <od:FunctionCode>9</od:FunctionCode>
            <od:TypeCode>IMZ</od:TypeCode>
            <od:GoodsItemQuantity>100</od:GoodsItemQuantity>
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

val stubbedResponse = s"""{
                         |    "status": 204,
                         |    "data" : "${xmlBody}"
                         |}""".stripMargin


val response: HttpResponse = new HttpResponse {
  def status = 200

  def body = stubbedResponse

  def allHeaders = ???
}



println(s"Response :  ${response}")
println(s"Response.Body:  ${response.body}")


val json = Json.parse(response.body)

val name = (json \ "data").as[String]

println(s"Response Json:  ${json}")
println(s" name:  ${name}")




XML.loadString(response.body)