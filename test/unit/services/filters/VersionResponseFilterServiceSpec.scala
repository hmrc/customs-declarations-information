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

package unit.services.filters

import org.scalatest.Assertion
import play.api.test.Helpers
import uk.gov.hmrc.customs.api.common.xml.ValidateXmlAgainstSchema
import uk.gov.hmrc.customs.api.common.xml.ValidateXmlAgainstSchema._
import uk.gov.hmrc.customs.declarations.information.services.filters.VersionResponseFilterService
import util.UnitSpec
import util.VersionTestXMLData.{defaultDateTime, generateDeclarationResponseContainingAllOptionalElements, generateDeclarationVersionResponse, validBackendVersionResponse}

import javax.xml.validation.Schema
import scala.concurrent.ExecutionContext
import scala.util.Try
import scala.xml._
import scala.xml.transform.{RewriteRule, RuleTransformer}

class VersionResponseFilterServiceSpec extends UnitSpec {
  implicit val ec: ExecutionContext = Helpers.stubControllerComponents().executionContext
  val schemaFile: Try[Schema] = getSchema("/api/conf/1.0/schemas/wco/declaration/DeclarationInformationRetrievalVersionResponse.xsd")
  val xmlValidationService: ValidateXmlAgainstSchema = new ValidateXmlAgainstSchema(schemaFile.get)

  trait SetUp {
    implicit val versionResponseFilterService: VersionResponseFilterService = new VersionResponseFilterService()
    val declarationVersionResponse: NodeSeq = generateDeclarationVersionResponse(creationDate = defaultDateTime)
    val versionResponseWithAllValues: NodeSeq = versionResponseFilterService.findPathThenTransform(declarationVersionResponse)
  }

  "Version Response Filter Service" should {
    "ensure output passes schema validation" in new SetUp {
      xmlValidationService.validate(versionResponseWithAllValues) should be(true)
    }

    "handle actual backend response" in new SetUp {
      val multiVersionResponsesWithAllValues: NodeSeq = versionResponseFilterService.findPathThenTransform(validBackendVersionResponse)
      xmlValidationService.validate(multiVersionResponsesWithAllValues) should be(true)
    }

    "handle multiple DeclarationVersionDetails elements in backend response" in new SetUp {
      val numberOfDecVersions = 5
      val multiVersionResponsesWithAllValues: NodeSeq = versionResponseFilterService.findPathThenTransform(generateDeclarationVersionResponse(numberOfDecVersions, creationDate = defaultDateTime))
      xmlValidationService.validate(multiVersionResponsesWithAllValues) should be(true)

      val node: NodeSeq = multiVersionResponsesWithAllValues \\ "DeclarationVersionDetails"
      node.size shouldBe numberOfDecVersions
    }

    "ensure element CreatedDateTime is present and correct" in new SetUp {
      val node: NodeSeq = getNodeFromXml(versionResponseWithAllValues, "CreatedDateTime", "DateTimeString")
      node.text shouldBe "20200715123000Z"
      node.head.attribute("formatCode").get.text shouldBe "304"
    }

    "ensure element LRN is present and correct" in new SetUp {
      val node: NodeSeq = getNodeFromXml(versionResponseWithAllValues, "LRN")
      node.text shouldBe "20GBAKZ81EQJ2WXYZ"
    }

    "ensure element ID is present and correct" in new SetUp {
      val node: NodeSeq = getNodeFromXml(versionResponseWithAllValues, "ID")
      node.text shouldBe "18GB9JLC3CU1LFGVR2"
    }

    "ensure element VersionID is present and correct" in new SetUp {
      val node: NodeSeq = getNodeFromXml(versionResponseWithAllValues, "VersionID")
      node.text shouldBe "1"
    }

    "handle missing VersionID element in backend response" in new SetUp {
      testForMissingElement("VersionID")
    }

    "handle missing ID element in backend response" in new SetUp {
      testForMissingElement("ID")
    }

    "handle future extension where all optional fields are returned" in new SetUp {
      val responsesWithAllValues: NodeSeq = versionResponseFilterService.findPathThenTransform(generateDeclarationResponseContainingAllOptionalElements(defaultDateTime))
      xmlValidationService.validate(responsesWithAllValues) should be(true)
    }

    "no declarations found" in new SetUp {
      val zeroDeclarations: NodeSeq = versionResponseFilterService.findPathThenTransform(generateDeclarationVersionResponse(0, defaultDateTime))
      xmlValidationService.validate(zeroDeclarations) should be(false)
    }
  }

  private def testForMissingElement(missingElementName: String)(implicit service: VersionResponseFilterService): Assertion = {
    val missingElementSourceXml = createElementFilter(missingElementName, "n1").transform(generateDeclarationVersionResponse(creationDate = defaultDateTime))
    val versionResponseWithMissingValues: NodeSeq = service.findPathThenTransform(missingElementSourceXml)
    xmlValidationService.validate(versionResponseWithMissingValues) should be(true)

    val node: NodeSeq = getNodeFromXml(versionResponseWithMissingValues, missingElementName)
    node.size shouldBe 0
  }

  private def createElementFilter(elementName: String, elementPrefix: String): RuleTransformer = {
    new RuleTransformer(new RewriteRule {
      override def transform(n: Node): Seq[Node] = n match {
        case Elem(`elementPrefix`, `elementName`, _, _, _*) => NodeSeq.Empty
        case n => n
      }
    })
  }

  private def getNodeFromXml(xml: NodeSeq, nodeName: String): NodeSeq = xml \\ "DeclarationVersionDetails" \ "Declaration" \ nodeName
  private def getNodeFromXml(xml: NodeSeq, nodeName1: String, nodeName2: String): NodeSeq = xml \\ "DeclarationVersionDetails" \ "Declaration" \ nodeName1 \ nodeName2
}
