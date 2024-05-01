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
import scala.xml.NodeSeq

@Singleton
class StatusResponseFilterService @Inject()() extends AbstractResponseFilterService {
  override protected val rootElementLabel: String = "DeclarationStatusResponse"
  override protected val detailsElementLabel: String = "DeclarationStatusDetails"
  override protected val NameSpaceP: String = "http://gov.uk/customs/declarationInformationRetrieval/status/v2"

  def findPathThenTransform(xml: NodeSeq): NodeSeq = {
    val declarationDetailsPath: NodeSeq = xml \ "responseDetail" \ "retrieveDeclarationStatusResponse" \ "retrieveDeclarationStatusDetailsList" \\ "retrieveDeclarationStatusDetails"
    transform(xml, declarationDetailsPath)
  }
}
