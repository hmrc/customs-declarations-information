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

import uk.gov.hmrc.customs.declarations.information.xml.HelperXMLUtils.extractNamespacesFromAllElements

import javax.inject.{Inject, Singleton}
import scala.xml.NodeSeq
import scala.xml.transform.RuleTransformer

@Singleton
class FullResponseFilterService @Inject()() extends AbstractResponseFilterService {
  override protected val NameSpaceP: String = "http://gov.uk/customs/FullDeclarationDataRetrievalService"
  override protected val rootElementLabel: String = "" // deliberately empty
  override protected val detailsElementLabel: String = "" // deliberately empty

  def findPathThenTransform(xml: NodeSeq): NodeSeq = {
    val declarationDetailsPath: NodeSeq = xml \ "responseDetail" \ "FullDeclarationDataRetrievalResponse"
    transform(xml, declarationDetailsPath)
  }

  override def transform(xml: NodeSeq, declarationDetailsPath: NodeSeq): NodeSeq = {
    val inputPrefixToUriMap: Map[String, String] = extractNamespacesFromAllElements(xml.head)
      .map(namespaceBinding => namespaceBinding.prefix -> namespaceBinding.uri).toMap

    val prefixReWriter: RuleTransformer = createTransformer(inputPrefixToUriMap)

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
}
