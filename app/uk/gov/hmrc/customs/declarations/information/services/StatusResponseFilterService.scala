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

  private val outputXmlNamespaceBindings = Map(
    ("http://www.w3.org/2001/XMLSchema-instance" -> "xsi"),
    ("http://gov.uk/customs/declarationInformationRetrieval/status/v2" -> "p"),
    ("urn:wco:datamodel:WCO:Response_DS:DMS:2" -> "p1"),
    ("urn:wco:datamodel:WCO:DEC-DMS:2" -> "p2"),
    ("urn:wco:datamodel:WCO:Declaration_DS:DMS:2" -> "p3"),
    ("urn:un:unece:uncefact:data:standard:UnqualifiedDataType:6" -> "p4")
  )

  def transform(xml: NodeSeq): NodeSeq = {
    val inputPrefixToUriMap = extractNamespaceBindings(xml.head.asInstanceOf[Elem]).map( nsb => (nsb.prefix -> nsb.uri)).toMap
    val inputPrefixToOutputPrefixMap = constructInputPrefixToOutputPrefixMap(inputPrefixToUriMap, outputXmlNamespaceBindings)
    val prefixReWriter = createDiscerningPrefixTransformer(inputPrefixToOutputPrefixMap)

    val decStatusDetails: NodeSeq = xml \ "responseDetail" \ "retrieveDeclarationStatusResponse" \ "retrieveDeclarationStatusDetailsList" \\ "retrieveDeclarationStatusDetails"

    <p:DeclarationStatusResponse
      xmlns:p="http://gov.uk/customs/declarationInformationRetrieval/status/v2"
      xmlns:p1="urn:wco:datamodel:WCO:Response_DS:DMS:2"
      xmlns:p2="urn:wco:datamodel:WCO:DEC-DMS:2"
      xmlns:p3="urn:wco:datamodel:WCO:Declaration_DS:DMS:2"
      xmlns:p4="urn:un:unece:uncefact:data:standard:UnqualifiedDataType:6"
      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
      xsi:schemaLocation="http://gov.uk/customs/declarationInformationRetrieval/status/v2">

      {decStatusDetails.map{ node =>
        val declarations = node \ "Declaration"
        val mdgDeclaration = declarations.filter( node => xml.head.getNamespace(node.prefix) == "http://gov.uk/customs/declarationInformationRetrieval/status/v2")
        val wcoDeclaration = declarations.filter( node => xml.head.getNamespace(node.prefix) == "urn:wco:datamodel:WCO:DEC-DMS:2")

        <p:DeclarationStatusDetails>
          {prefixReWriter.transform(mdgDeclaration)}
          {prefixReWriter.transform(wcoDeclaration)}
        </p:DeclarationStatusDetails>
      }}
    </p:DeclarationStatusResponse>
  }

  private def constructInputPrefixToOutputPrefixMap(inputPrefixToUriMap: Map[String, String], outputUriToPrefixMap: Map[String, String]): Map[String, String] = {
    inputPrefixToUriMap.keySet.foldLeft(Seq.empty[(String, String)]){ (inputPrefixToOutputPrefix, inputPrefix) =>
      val inputUri = inputPrefixToUriMap(inputPrefix)
      val outputPrefix = outputUriToPrefixMap.getOrElse(inputUri, inputPrefix)
      val inputToOutputPrefix: (String, String) = (inputPrefix, outputPrefix)

      inputPrefixToOutputPrefix :+ inputToOutputPrefix
    }.toMap
  }
}