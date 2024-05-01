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
import util.SearchTestXMLData.{defaultDateTime, generateDeclarationResponseContainingAllOptionalElements, generateDeclarationSearchResponse, validBackendSearchResponse}
import util.UnitSpec

import scala.concurrent.ExecutionContext
import scala.xml._
import scala.xml.transform.{RewriteRule, RuleTransformer}
import ValidateXmlAgainstSchema._
import uk.gov.hmrc.customs.declarations.information.services.filters.SearchResponseFilterService

import javax.xml.validation.Schema
import scala.util.Try

class DeclarationSearchResponseFilterServiceSpec extends UnitSpec {
  implicit val ec: ExecutionContext = Helpers.stubControllerComponents().executionContext
  private val schemaFile: Try[Schema] = getSchema("/api/conf/1.0/schemas/wco/declaration/DeclarationInformationRetrievalSearchResponse.xsd")
  private val xmlValidationService: ValidateXmlAgainstSchema = new ValidateXmlAgainstSchema(schemaFile.get)

  trait SetUp {
    implicit val service: SearchResponseFilterService = new SearchResponseFilterService()
    val declarationSearchResponse: NodeSeq = generateDeclarationSearchResponse(receivedDate = defaultDateTime)
    val searchResponseWithAllValues: NodeSeq = service.findPathThenTransform(declarationSearchResponse)
  }

  "Search Response Filter Service" should {
    "ensure output passes schema validation" in new SetUp {
      xmlValidationService.validate(searchResponseWithAllValues) should be(true)
    }

    "handle actual backend response" in new SetUp {
      val multiSearchResponsesWithAllValues: NodeSeq = service.findPathThenTransform(validBackendSearchResponse)
      xmlValidationService.validate(multiSearchResponsesWithAllValues) should be(true)
    }

    "handle multiple DeclarationSearchDetails elements in backend response" in new SetUp {
      val numberOfDecVersions = 5
      val declarationSearchResponseWithMultipleVersions: NodeSeq = generateDeclarationSearchResponse(numberOfDecVersions, receivedDate = defaultDateTime)
      val multiSearchResponsesWithAllValues: NodeSeq = service.findPathThenTransform(declarationSearchResponseWithMultipleVersions)
      xmlValidationService.validate(multiSearchResponsesWithAllValues) should be(true)

      val node: NodeSeq = multiSearchResponsesWithAllValues \\ "DeclarationSearchDetails"
      node.size shouldBe numberOfDecVersions
    }

    "ensure element ReceivedDateTime is present" in new SetUp {
      val node: NodeSeq = getNodeFromXml(searchResponseWithAllValues, "ReceivedDateTime", "DateTimeString")
      node.text shouldBe "20200715123000Z"
      node.head.attribute("formatCode").get.text shouldBe "304"
    }

    "ensure element LRN is present" in new SetUp {
      val node: NodeSeq = getNodeFromXml(searchResponseWithAllValues, "LRN")
      node.text shouldBe "20GBAKZ81EQJ2WXYZ"
    }

    "ensure element ID is present" in new SetUp {
      val node: NodeSeq = getNodeFromXml(searchResponseWithAllValues, "ID")
      node.text shouldBe "18GB9JLC3CU1LFGVR2"
    }

    "ensure element ICS is present" in new SetUp {
      val node: NodeSeq = getNodeFromXml(searchResponseWithAllValues, "ICS")
      node.text shouldBe "15"
    }

    "ensure element CurrentPageNumber is present" in new SetUp {
      val node: NodeSeq = searchResponseWithAllValues \ "CurrentPageNumber"
      node.text shouldBe "1"
    }

    "ensure element TotalResultsAvailable is present" in new SetUp {
      val node: NodeSeq = searchResponseWithAllValues \ "TotalResultsAvailable"
      node.text shouldBe "1"
    }

    "ensure element TotalPagesAvailable is present" in new SetUp {
      val node: NodeSeq = searchResponseWithAllValues \ "TotalPagesAvailable"
      node.text shouldBe "2"
    }

    "handle missing DutyTaxFee element in backend response" in new SetUp {
      testForMissingElement("DutyTaxFee")
    }

    "handle missing ID element in backend response" in new SetUp {
      testForMissingElement("ID")
    }

    "handle future extension where all optional fields are returned" in new SetUp {
      val responsesWithAllValues: NodeSeq = service.findPathThenTransform(generateDeclarationResponseContainingAllOptionalElements(defaultDateTime))
      xmlValidationService.validate(responsesWithAllValues) should be(true)
    }

    "no declarations found" in new SetUp {
      val zeroDeclarations: NodeSeq = service.findPathThenTransform(generateDeclarationSearchResponse(0, defaultDateTime))

      xmlValidationService.validate(zeroDeclarations) should be(true)
      (searchResponseWithAllValues \ "CurrentPageNumber").text shouldBe "1"
    }
  }

  private def testForMissingElement(missingElementName: String)(implicit service: SearchResponseFilterService): Assertion = {
    val missingElementSourceXml = createElementFilter(missingElementName, "n1").transform(generateDeclarationSearchResponse(receivedDate = defaultDateTime))
    val searchResponseWithMissingValues: NodeSeq = service.findPathThenTransform(missingElementSourceXml)
    xmlValidationService.validate(searchResponseWithMissingValues) should be(true)

    val node = getNodeFromXml(searchResponseWithMissingValues, missingElementName)
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

//TODO these are duplicated across multiple services :S
  private def getNodeFromXml(xml: NodeSeq, nodeName: String): NodeSeq = xml \\ "DeclarationSearchDetails" \ "Declaration" \ nodeName
  private def getNodeFromXml(xml: NodeSeq, nodeName1: String, nodeName2: String): NodeSeq = xml \\ "DeclarationSearchDetails" \ "Declaration" \ nodeName1 \ nodeName2
}
