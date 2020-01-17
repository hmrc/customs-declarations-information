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

package uk.gov.hmrc.customs.declarations.information.services

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.customs.declarations.information.logging.InformationLogger

import scala.xml._

@Singleton
class StatusResponseFilterService @Inject() (informationLogger: InformationLogger, informationConfigService: InformationConfigService) {
  import uk.gov.hmrc.customs.declarations.information.xml.HelperXMLUtils._

  private val dirPrefixReWriter = createPrefixTransformer("p")
  private val wcoResponsePrefixReWriter = createPrefixTransformer("p1")
  private val wcoPrefixReWriter = createPrefixTransformer("p2")

  def transform(xml: NodeSeq): NodeSeq = {
    val decStatusDetails: NodeSeq = xml \ "responseDetail" \ "retrieveDeclarationStatusResponse" \ "retrieveDeclarationStatusDetailsList" \\ "retrieveDeclarationStatusDetails"

    <p:DeclarationStatusResponse
      xmlns:p="http://gov.uk/customs/declarationInformationRetrieval/status/v2"
      xmlns:p1="urn:wco:datamodel:WCO:Response_DS:DMS:2"
      xmlns:p2="urn:wco:datamodel:WCO:DEC-DMS:2"
      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
      xsi:schemaLocation="http://gov.uk/customs/declarationInformationRetrieval/status/v2">

      {decStatusDetails.map{ node =>
        val declarations = node \ "Declaration"
        val mdgDeclaration = declarations.filter( node => xml.head.getNamespace(node.prefix) == "http://gov.uk/customs/declarationInformationRetrieval/status/v2")
        val wcoDeclaration = declarations.filter( node => xml.head.getNamespace(node.prefix) == "urn:wco:datamodel:WCO:DEC-DMS:2")

        transformDeclarationStatusDetail(mdgDeclaration, wcoDeclaration)
      }}
    </p:DeclarationStatusResponse>
  }

  private def transformDeclarationStatusDetail(mdgDeclaration: NodeSeq, wcoDeclaration: NodeSeq): NodeSeq = {
    <p:DeclarationStatusDetails>
      <p:Declaration>
        {dirPrefixReWriter.transform(mdgDeclaration \ "ReceivedDateTime")}
        {dirPrefixReWriter.transform(mdgDeclaration \ "GoodsReleasedDateTime")}
        {dirPrefixReWriter.transform(mdgDeclaration \ "ROE")}
        {dirPrefixReWriter.transform(mdgDeclaration \ "ICS")}
        {dirPrefixReWriter.transform(mdgDeclaration \ "IRC")}
        {wcoResponsePrefixReWriter.transform(mdgDeclaration \ "AcceptanceDateTime" \ "DateTimeString").map{ dts => <p:AcceptanceDateTime>{dts}</p:AcceptanceDateTime>}}
        {dirPrefixReWriter.transform(mdgDeclaration \ "ID")}
        {dirPrefixReWriter.transform(mdgDeclaration \ "VersionID")}
      </p:Declaration>
      {wcoPrefixReWriter.transform(wcoDeclaration)}
    </p:DeclarationStatusDetails>
  }
}