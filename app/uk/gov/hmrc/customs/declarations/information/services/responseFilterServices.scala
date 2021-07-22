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

import uk.gov.hmrc.customs.declarations.information.xml.HelperXMLUtils.{createPrefixTransformer, extractNamespacesFromAllElements}

import javax.inject.{Inject, Singleton}
import scala.xml.transform.RuleTransformer
import scala.xml.{Node, NodeSeq, Text, TopScope}

@Singleton
class FullResponseFilterService @Inject()() extends ResponseFilterService {

  override protected val NameSpaceP="http://gov.uk/customs/FullDeclarationDataRetrievalService"
  override protected val rootElementLabel: String = ""
  override protected val detailsElementLabel: String = ""

  def transform(xml: NodeSeq): NodeSeq = {
    val declarationDetailsPath: NodeSeq = xml \ "responseDetail" \ "FullDeclarationDataRetrievalResponse"
    transform(xml, declarationDetailsPath)
  }

  override def transform(xml: NodeSeq, declarationDetailsPath: NodeSeq): NodeSeq = {

    val inputPrefixToUriMap: Map[String, String] = extractNamespacesFromAllElements(xml.head)
      .map( nsb => nsb.prefix -> nsb.uri)
      .toMap

    val prefixReWriter = createTransformer(inputPrefixToUriMap)

    <p:DeclarationFullResponse
    xmlns:p={NameSpaceP}
    xmlns:p1={NameSpaceP1}
    xmlns:p2={NameSpaceP2}
    xmlns:p3={NameSpaceP3}
    xmlns:p4={NameSpaceP4}
    xmlns:xsi={NameSpaceXsi}
    xsi:schemaLocation={NameSpaceP}>
        {prefixReWriter.transform((declarationDetailsPath \ "FullDeclarationDataDetails").filter(node => inputPrefixToUriMap(node.prefix) == NameSpaceP))}
    </p:DeclarationFullResponse>
  }

//transform with no prefix transform!!
  //  override def transform(xml: NodeSeq, declarationDetailsPath: NodeSeq): NodeSeq = {
//    <n1:DeclarationFullResponse
//    xmlns:Q1="urn:wco:datamodel:WCO:Response_DS:DMS:2"
//    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
//    xmlns:ds="urn:wco:datamodel:WCO:Declaration_DS:DMS:2"
//    xmlns:n1="http://gov.uk/customs/FullDeclarationDataRetrievalService"
//    xsi:schemaLocation="http://gov.uk/customs/FullDeclarationDataRetrievalService">
//      {declarationDetailsPath \ "FullDeclarationDataDetails"}
//    </n1:DeclarationFullResponse>
//  }
}

@Singleton
class SearchResponseFilterService @Inject()() extends ResponseFilterService {

  override protected val NameSpaceP="http://gov.uk/customs/declarationInformationRetrieval/declarationSummary/v1"
  override protected val rootElementLabel: String = "DeclarationSearchResponse"
  override protected val detailsElementLabel: String = "DeclarationSearchDetails"

  def transform(xml: NodeSeq): NodeSeq = {
    val declarationDetailsPath: NodeSeq = xml \ "responseDetail" \ "declarationSummary" \ "DeclarationSummaryDataList" \\ "DeclarationSummaryData"
    transform(xml, declarationDetailsPath)
  }

  override def endNodes(xml: NodeSeq, rule: RuleTransformer): NodeSeq = {
    val newLineAndIndentation = "\n        "
    val endNodesPath = xml \ "responseDetail" \ "declarationSummary" \ "DeclarationSummaryDataList"
    val currentPageNumber = endNodesPath \\ "CurrentPageNumber"
    val totalResultsAvailable = endNodesPath \\ "TotalResultsAvailable"
    val totalPagesAvailable = endNodesPath \\ "TotalPagesAvailable"
    val noResultsReturned = endNodesPath \\ "NoResultsReturned"

    Seq(rule.transform(currentPageNumber).head, Text(newLineAndIndentation),
        rule.transform(totalResultsAvailable).head, Text(newLineAndIndentation),
        rule.transform(totalPagesAvailable).head, Text(newLineAndIndentation),
        rule.transform(noResultsReturned).head)
  }
}

@Singleton
class VersionResponseFilterService @Inject()() extends ResponseFilterService {

  override protected val NameSpaceP="http://gov.uk/customs/retrieveDeclarationVersion"
  override protected val rootElementLabel: String = "DeclarationVersionResponse"
  override protected val detailsElementLabel: String = "DeclarationVersionDetails"

  def transform(xml: NodeSeq): NodeSeq = {
    val declarationDetailsPath: NodeSeq = xml \ "responseDetail" \ "RetrieveDeclarationVersionResponse" \ "RetrieveDeclarationVersionDetailsList" \\ "RetrieveDeclarationVersionDetails"
    transform(xml, declarationDetailsPath)
  }
}

@Singleton
class StatusResponseFilterService @Inject()() extends ResponseFilterService {

  override protected val NameSpaceP="http://gov.uk/customs/declarationInformationRetrieval/status/v2"
  override protected val rootElementLabel: String = "DeclarationStatusResponse"
  override protected val detailsElementLabel: String = "DeclarationStatusDetails"

  def transform(xml: NodeSeq): NodeSeq = {
    val declarationDetailsPath: NodeSeq = xml \ "responseDetail" \ "retrieveDeclarationStatusResponse" \ "retrieveDeclarationStatusDetailsList" \\ "retrieveDeclarationStatusDetails"
    transform(xml, declarationDetailsPath)
  }
}

abstract class ResponseFilterService() {

  protected val NameSpaceP: String
  protected val NameSpaceP1="urn:wco:datamodel:WCO:Response_DS:DMS:2"   //wco response elements definition name space
  protected val NameSpaceP2="urn:wco:datamodel:WCO:DEC-DMS:2"           //wco Declaration elements definition name space
  protected val NameSpaceP3="urn:wco:datamodel:WCO:Declaration_DS:DMS:2"//wco Declaration complex type definition
  protected val NameSpaceP4="urn:un:unece:uncefact:data:standard:UnqualifiedDataType:6"
  protected val NameSpaceXsi="http://www.w3.org/2001/XMLSchema-instance"

  protected val rootElementLabel: String
  protected val detailsElementLabel: String

  protected def endNodes(xml: NodeSeq, rule: RuleTransformer): NodeSeq = NodeSeq.Empty

  def transform(xml: NodeSeq, declarationDetailsPath: NodeSeq): NodeSeq = {

    val inputPrefixToUriMap = extractNamespacesFromAllElements(xml.head)
      .map( nsb => nsb.prefix -> nsb.uri)
      .toMap

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
        val mdgDeclaration = declarations.filter( node => inputPrefixToUriMap(node.prefix) == NameSpaceP)
        val wcoDeclaration = declarations.filter( node => inputPrefixToUriMap(node.prefix) == "urn:wco:datamodel:WCO:DEC-DMS:2")

        <p:details>
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
    val outputUriToPrefixMap = Map(
      NameSpaceXsi -> "xsi",
      NameSpaceP -> "p",
      NameSpaceP1 -> "p1",
      NameSpaceP2 -> "p2",
      NameSpaceP3 -> "p3",
      NameSpaceP4 -> "p4"
    )
    val inputPrefixToOutputPrefixMap = constructInputPrefixToOutputPrefixMap(inputPrefixToUriMap, outputUriToPrefixMap)
    createPrefixTransformer(inputPrefixToOutputPrefixMap, TopScope)
  }

}
