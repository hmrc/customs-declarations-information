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
import scala.xml.transform.{RewriteRule, RuleTransformer}

@Singleton
class StatusResponseFilterService @Inject() (informationLogger: InformationLogger, informationConfigService: InformationConfigService) {

  def transform(xml: NodeSeq): NodeSeq = {
    val decStatusDetails: NodeSeq = xml \ "responseDetail" \ "retrieveDeclarationStatusResponse" \ "retrieveDeclarationStatusDetailsList" \\ "retrieveDeclarationStatusDetails"

    <p:DeclarationStatusResponse
      xmlns:clm5ISO42173A="urn:un:unece:uncefact:codelist:standard:ISO:ISO3AlphaCurrencyCode:2012-08-31"
      xmlns:clm63055="urn:un:unece:uncefact:codelist:standard:UNECE:AgencyIdentificationCode:D12B"
      xmlns:p="http://gov.uk/customs/declarationInformationRetrieval/status/v2"
      xmlns:p1="urn:wco:datamodel:WCO:Response_DS:DMS:2"
      xmlns:p2="urn:wco:datamodel:WCO:DEC-DMS:2"
      xmlns:p3="urn:wco:datamodel:WCO:Declaration_DS:DMS:2"
      xmlns:udt="urn:un:unece:uncefact:data:standard:UnqualifiedDataType:6"
      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
      xsi:schemaLocation="http://gov.uk/customs/declarationInformationRetrieval/status/v2 ../schemas/wco/declaration/declarationInformationRetrievalStatusResponse.xsd ">

      {decStatusDetails.map{ node =>
        val declarations = node \ "Declaration"
        val mdgDeclaration = declarations.filter( node => xml.head.getNamespace(node.prefix) == "http://gov.uk/customs/declarationInformationRetrieval/status/v2")
        val wcoDeclaration = declarations.filter( node => xml.head.getNamespace(node.prefix) == "urn:wco:datamodel:WCO:DEC-DMS:2")

        transformDeclarationStatusDetail(mdgDeclaration, wcoDeclaration)
      }}
    </p:DeclarationStatusResponse>
  }

  private def transformDeclarationStatusDetail(mdgDeclaration: NodeSeq, wcoDeclaration: NodeSeq): NodeSeq = {

    val maybeReceivedDateTime = extract(mdgDeclaration \ "ReceivedDateTime")
    val maybeGoodsReleasedDateTime = extract(mdgDeclaration \ "GoodsReleasedDateTime")
    val maybeAcceptanceDateTime = extract(mdgDeclaration \ "AcceptanceDateTime")
    val maybeROE = extract(mdgDeclaration \ "ROE")
    val maybeICS = extract(mdgDeclaration \ "ICS")
    val maybeIRC = extract(mdgDeclaration \ "IRC")
    val maybeID = extract(mdgDeclaration \ "ID")
    val maybeVersionId = extract(mdgDeclaration \ "VersionID")

    <p:DeclarationStatusDetails>
      <p:Declaration>
        {
          outputDateTimeStringTypeElement(<p:ReceivedDateTime></p:ReceivedDateTime>, maybeReceivedDateTime) ++
          outputDateTimeStringTypeElement(<p:GoodsReleasedDateTime></p:GoodsReleasedDateTime>, maybeGoodsReleasedDateTime) ++
          outputDateTimeStringTypeElement(<p:AcceptanceDateTime></p:AcceptanceDateTime>, maybeAcceptanceDateTime) ++
          outputStringTypeElement(<p:ROE></p:ROE>, maybeROE) ++
          outputStringTypeElement(<p:ICS></p:ICS>, maybeICS) ++
          outputStringTypeElement(<p:IRC></p:IRC>, maybeIRC) ++
          outputStringTypeElement(<p:ID></p:ID>, maybeID) ++
          outputStringTypeElement(<p:VersionID></p:VersionID>, maybeVersionId)
        }
      </p:Declaration>
      {wcoNamespaceReWriter.transform(wcoDeclaration)}
    </p:DeclarationStatusDetails>
  }

  private def outputStringTypeElement(element: Elem, maybeValueAndMetaDataPair: (Option[String], Option[MetaData])): NodeSeq = {
    maybeValueAndMetaDataPair._1.fold(NodeSeq.Empty){value =>
      element.copy(child = Text(value))
    }
  }

  private def outputDateTimeStringTypeElement(parentElement: Elem, maybeValueAndMetaDataPair: (Option[String], Option[MetaData])): NodeSeq = {
    println(s">> $maybeValueAndMetaDataPair")
    maybeValueAndMetaDataPair._1.fold(NodeSeq.Empty){value =>
      val content = <p:DateTimeString formatCode={outputAttribute(maybeValueAndMetaDataPair._2, "formatCode")}>{value}</p:DateTimeString>
      parentElement.copy(child = Seq[Node](content))
    }
  }

  private def outputAttribute(maybeAttributes: Option[MetaData], attributeLabel: String): Option[Text] = {
    maybeAttributes.fold[Option[Text]](None){attr =>
      attr.get(attributeLabel).fold[Option[Text]](None){attrValue =>
        Some(Text(attrValue.text))
      }
    }
  }

  private def createNamespaceTransformer(targetPrefix: String): RuleTransformer = {
    new RuleTransformer( new RewriteRule {
      override def transform(n: Node): Seq[Node] = n match {
        case e: Elem => e.copy(prefix = targetPrefix)
        case `n` => n
      }
    })
  }

  private val cdiNamespaceReWriter = createNamespaceTransformer("p")
  private val wcoNamespaceReWriter = createNamespaceTransformer("p2")


  private def extract(path: NodeSeq): (Option[String], Option[MetaData]) = {
    if (path.nonEmpty && path.head.nonEmpty) {
      (Some(path.head.text.trim()), Some(path.head.attributes))
    }
    else {
      (None, None)
    }
  }
}