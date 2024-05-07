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

package uk.gov.hmrc.customs.declarations.information.services.filter

import uk.gov.hmrc.customs.declarations.information.xml.HelperXMLUtils.{createPrefixTransformer, extractNamespacesFromAllElements}

import scala.xml.transform.RuleTransformer
import scala.xml.{NodeSeq, TopScope}

abstract class AbstractResponseFilterService() {
  protected val rootElementLabel: String
  protected val detailsElementLabel: String
  protected val NameSpaceP: String
  protected val NameSpaceP1: String = "urn:wco:datamodel:WCO:Response_DS:DMS:2" //wco response elements definition name space
  protected val NameSpaceP2: String = "urn:wco:datamodel:WCO:DEC-DMS:2" //wco Declaration elements definition name space
  protected val NameSpaceP3: String = "urn:wco:datamodel:WCO:Declaration_DS:DMS:2" //wco Declaration complex type definition
  protected val NameSpaceP4: String = "urn:un:unece:uncefact:data:standard:UnqualifiedDataType:6"
  protected val NameSpaceXsi: String = "http://www.w3.org/2001/XMLSchema-instance"

  //TODO no idea the point of this method, looks like it can be removed but requires investigation
  protected def endNodes(xml: NodeSeq, rule: RuleTransformer): NodeSeq = NodeSeq.Empty

  def transform(xml: NodeSeq, declarationDetailsPath: NodeSeq): NodeSeq = {
    val inputPrefixToUriMap: Map[String, String] = extractNamespacesFromAllElements(xml.head)
      .map(namespaceBinding => namespaceBinding.prefix -> namespaceBinding.uri).toMap
    val prefixReWriter = createTransformer(inputPrefixToUriMap)

    <p:root
    xmlns:p={NameSpaceP}
    xmlns:p1={NameSpaceP1}
    xmlns:p2={NameSpaceP2}
    xmlns:p3={NameSpaceP3}
    xmlns:p4={NameSpaceP4}
    xmlns:xsi={NameSpaceXsi}
    xsi:schemaLocation={NameSpaceP}>
      {declarationDetailsPath.map{ node =>
      val declarations = node \ "Declaration"
      val mdgDeclaration = declarations.filter(node => inputPrefixToUriMap(node.prefix) == NameSpaceP)
      val wcoDeclaration = declarations.filter(node => inputPrefixToUriMap(node.prefix) == "urn:wco:datamodel:WCO:DEC-DMS:2")

        <p:details>
          {prefixReWriter.transform(mdgDeclaration)}
          {prefixReWriter.transform(wcoDeclaration)}
        </p:details>.copy(label = detailsElementLabel)
      }}{endNodes(xml, prefixReWriter)}
    </p:root>.copy(label = rootElementLabel)
  }

  protected def constructInputPrefixToOutputPrefixMap(inputPrefixToUriMap: Map[String, String], outputUriToPrefixMap: Map[String, String]): Map[String, String] = {
    inputPrefixToUriMap.keySet.foldLeft(Seq.empty[(String, String)]){ (inputPrefixToOutputPrefix, inputPrefix) =>
      val inputUri = inputPrefixToUriMap(inputPrefix)
      val outputPrefix = outputUriToPrefixMap.getOrElse(inputUri, inputPrefix)
      val inputToOutputPrefix: (String, String) = (inputPrefix, outputPrefix)

      inputPrefixToOutputPrefix :+ inputToOutputPrefix
    }.toMap
  }

  protected def createTransformer(inputPrefixToUriMap: Map[String, String]): RuleTransformer = {
    val outputUriToPrefixMap: Map[String, String] = Map(
      NameSpaceXsi -> "xsi",
      NameSpaceP   -> "p",
      NameSpaceP1  -> "p1",
      NameSpaceP2  -> "p2",
      NameSpaceP3  -> "p3",
      NameSpaceP4  -> "p4")
    val inputPrefixToOutputPrefixMap: Map[String, String] = constructInputPrefixToOutputPrefixMap(inputPrefixToUriMap, outputUriToPrefixMap)
    createPrefixTransformer(inputPrefixToOutputPrefixMap, TopScope)
  }
}
