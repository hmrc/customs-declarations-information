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

package uk.gov.hmrc.customs.declarations.information.services.filters

import javax.inject.{Inject, Singleton}
import scala.xml.transform.RuleTransformer
import scala.xml.{NodeSeq, Text}

@Singleton
class SearchResponseFilterService @Inject()() extends AbstractResponseFilterService {
  override protected val rootElementLabel: String = "DeclarationSearchResponse"
  override protected val detailsElementLabel: String = "DeclarationSearchDetails"
  override protected val NameSpaceP: String = "http://gov.uk/customs/declarationInformationRetrieval/declarationSummary/v1"

  def findPathThenTransform(xml: NodeSeq): NodeSeq = {
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