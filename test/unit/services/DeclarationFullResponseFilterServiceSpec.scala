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

package unit.services

import org.scalatest.Assertion
import play.api.test.Helpers
import uk.gov.hmrc.customs.api.common.xml.ValidateXmlAgainstSchema
import util.FullTestXMLData.backendDeclarationFullResponse
import util.UnitSpec

import javax.xml.validation.Schema
import scala.concurrent.ExecutionContext
import scala.util.Try
import scala.xml._
import scala.xml.transform.{RewriteRule, RuleTransformer}
import ValidateXmlAgainstSchema._
import uk.gov.hmrc.customs.declarations.information.services.filters.FullResponseFilterService

class DeclarationFullResponseFilterServiceSpec extends UnitSpec {
  implicit val ec: ExecutionContext = Helpers.stubControllerComponents().executionContext
  val schemaFile: Try[Schema] = getSchema("/api/conf/1.0/schemas/wco/declaration/DeclarationInformationRetrievalFullResponse.xsd")
  val xmlValidationService: ValidateXmlAgainstSchema = new ValidateXmlAgainstSchema(schemaFile.get)

  trait SetUp {
    implicit val service: FullResponseFilterService = new FullResponseFilterService()
    val fullResponseWithAllValues: NodeSeq = service.findPathThenTransform(backendDeclarationFullResponse)
  }

  "Full Response Filter Service" should {
    "ensure output passes schema validation" in new SetUp {
      xmlValidationService.validate(fullResponseWithAllValues) should be(true)
    }

    "ensure element CreatedDateTime is present and correct" in new SetUp {
      val node: NodeSeq = fullResponseWithAllValues \ "FullDeclarationDataDetails" \ "HighLevelSummaryDetails" \ "CreatedDateTime" \ "DateTimeString"
      node.text shouldBe "20240111115536Z"
      node.head.attribute("formatCode").get.text shouldBe "304"
    }

    "ensure element LRN is present and correct" in new SetUp {
      val node: NodeSeq = fullResponseWithAllValues \ "FullDeclarationDataDetails" \ "HighLevelSummaryDetails" \ "LRN"
      node.text shouldBe "SK383RefTC181788"
    }

    "ensure element VersionID is present and correct" in new SetUp {
      val node: NodeSeq = fullResponseWithAllValues \ "FullDeclarationDataDetails" \ "HighLevelSummaryDetails" \ "VersionID"
      node.text shouldBe "1"
    }

    "handle missing VersionID element in backend response" in new SetUp {
      testForMissingElement("VersionID")
    }

    "handle missing ID element in backend response" in new SetUp {
      testForMissingElement("ID")
    }

  }

  private def createElementFilter(elementName: String, elementPrefix: String): RuleTransformer = {
    new RuleTransformer(new RewriteRule {
      override def transform(n: Node): Seq[Node] = n match {
        case Elem(`elementPrefix`, `elementName`, _, _, _*) => NodeSeq.Empty
        case n => n
      }
    })
  }

  private def testForMissingElement(missingElementName: String)(implicit service: FullResponseFilterService): Assertion = {
    val missingElementSourceXml = createElementFilter(missingElementName, "n1").transform(backendDeclarationFullResponse)
    val versionResponseWithMissingValues: NodeSeq = service.findPathThenTransform(missingElementSourceXml)
    xmlValidationService.validate(versionResponseWithMissingValues) should be(true)

    val node = commonPath(versionResponseWithMissingValues) \ missingElementName
    node.size shouldBe 0
  }

  private def commonPath(xml: NodeSeq): NodeSeq = xml \\ "FullDeclarationDataDetails" \ "HighLevelSummaryDetails"
}
