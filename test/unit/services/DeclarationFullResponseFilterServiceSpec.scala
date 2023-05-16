/*
 * Copyright 2023 HM Revenue & Customs
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

package unit.services

import org.scalatest.Assertion

import play.api.test.Helpers
import uk.gov.hmrc.customs.api.common.xml.ValidateXmlAgainstSchema
import uk.gov.hmrc.customs.declarations.information.services.FullResponseFilterService
import util.FullTestXMLData.backendDeclarationFullResponse
import util.UnitSpec

import scala.xml._
import scala.xml.transform.{RewriteRule, RuleTransformer}

class DeclarationFullResponseFilterServiceSpec extends UnitSpec  {
  implicit val ec = Helpers.stubControllerComponents().executionContext

  private def createElementFilter(elementName: String, elementPrefix: String): RuleTransformer = {
    new RuleTransformer( new RewriteRule {
      override def transform(n: Node): Seq[Node] = n match {
        case Elem(`elementPrefix`, `elementName`, _, _, _*) => NodeSeq.Empty
        case n => n
      }
    })
  }

  import ValidateXmlAgainstSchema._
  val schemaFile = getSchema("/api/conf/1.0/schemas/wco/declaration/DeclarationInformationRetrievalFullResponse.xsd")
  def xmlValidationService: ValidateXmlAgainstSchema = new ValidateXmlAgainstSchema(schemaFile.get)

  trait SetUp {
    implicit val service = new FullResponseFilterService()
    val fullResponseWithAllValues: NodeSeq = service.transform(backendDeclarationFullResponse)
  }

  "Full Response Filter Service" should {

    "ensure output passes schema validation" in new SetUp {
      xmlValidationService.validate(fullResponseWithAllValues) should be(true)
    }

    "ensure element CreatedDateTime is present" in new SetUp {
      val node = fullResponseWithAllValues \ "FullDeclarationDataDetails" \ "HighLevelSummaryDetails"  \ "CreatedDateTime" \ "DateTimeString"

      node.text shouldBe "20190702110757Z"
      node.head.attribute("formatCode").get.text shouldBe "304"
    }

    "ensure element LRN is present" in new SetUp {
      val node = fullResponseWithAllValues \ "FullDeclarationDataDetails" \ "HighLevelSummaryDetails"  \ "LRN"

      node.text shouldBe "20GBAKZ81EQJ2WXYZ"
    }

    "ensure element VersionID is present" in new SetUp {
      val node = fullResponseWithAllValues \ "FullDeclarationDataDetails" \ "HighLevelSummaryDetails" \ "VersionID"

      node.text shouldBe "4"
    }

    "handle missing VersionID element in backend response" in new SetUp {
      testForMissingElement("VersionID")
    }

    "handle missing ID element in backend response" in new SetUp {
      testForMissingElement("ID")
    }

  }

  private def testForMissingElement(missingElementName: String)(implicit service: FullResponseFilterService): Assertion = {
    val missingElementSourceXml = createElementFilter(missingElementName, "n1").transform(backendDeclarationFullResponse)
    val versionResponseWithMissingValues: NodeSeq = service.transform(missingElementSourceXml)

    xmlValidationService.validate(versionResponseWithMissingValues) should be(true)

    val node = commonPath(versionResponseWithMissingValues) \ missingElementName

    node.size shouldBe 0
  }

  private def commonPath(xml: NodeSeq): NodeSeq = xml \\ "FullDeclarationDataDetails" \ "HighLevelSummaryDetails"
}
