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

package uk.gov.hmrc.customs.declarations.information.services

import javax.inject.{Inject, Singleton}
import scala.xml.{NodeSeq, TopScope}

@Singleton
class StatusResponseFilterService @Inject() () {
  import uk.gov.hmrc.customs.declarations.information.xml.HelperXMLUtils._

  val NameSpaceP="http://gov.uk/customs/declarationInformationRetrieval/status/v2"
  val NameSpaceP1="urn:wco:datamodel:WCO:Response_DS:DMS:2"   //wco response elements definition name space
  val NameSpaceP2="urn:wco:datamodel:WCO:DEC-DMS:2"           //wco Declaration elements definition name space
  val NameSpaceP3="urn:wco:datamodel:WCO:Declaration_DS:DMS:2"//wco Declaration complex type definition
  val NameSpaceP4="urn:un:unece:uncefact:data:standard:UnqualifiedDataType:6"
  val NameSpaceXsi="http://www.w3.org/2001/XMLSchema-instance"

  private val outputUriToPrefixMap = Map(
    NameSpaceXsi -> "xsi",
    NameSpaceP -> "p",
    NameSpaceP1 -> "p1",
    NameSpaceP2 -> "p2",
    NameSpaceP3 -> "p3",
    NameSpaceP4 -> "p4"
  )

  def transform(xml: NodeSeq): NodeSeq = {
    val inputPrefixToUriMap = extractNamespacesFromAllElements(xml.head)
      .map( nsb => (nsb.prefix -> nsb.uri))
      .toMap

    val inputPrefixToOutputPrefixMap = constructInputPrefixToOutputPrefixMap(inputPrefixToUriMap, outputUriToPrefixMap)
    val prefixReWriter = createPrefixTransformer(inputPrefixToOutputPrefixMap, TopScope)
    val decStatusDetails: NodeSeq = xml \ "responseDetail" \ "retrieveDeclarationStatusResponse" \ "retrieveDeclarationStatusDetailsList" \\ "retrieveDeclarationStatusDetails"

    <p:DeclarationStatusResponse
    xmlns:p={NameSpaceP}
    xmlns:p1={NameSpaceP1}
    xmlns:p2={NameSpaceP2}
    xmlns:p3={NameSpaceP3}
    xmlns:p4={NameSpaceP4}
    xmlns:xsi={NameSpaceXsi}
    xsi:schemaLocation={NameSpaceP}>
      {decStatusDetails.map{ node =>
        val declarations = node \ "Declaration"
        val mdgDeclaration = declarations.filter( node => inputPrefixToUriMap(node.prefix) == "http://gov.uk/customs/declarationInformationRetrieval/status/v2")
        val wcoDeclaration = declarations.filter( node => inputPrefixToUriMap(node.prefix) == "urn:wco:datamodel:WCO:DEC-DMS:2")

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
