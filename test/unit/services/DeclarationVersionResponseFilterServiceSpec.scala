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

package unit.services

import org.scalatest.Assertion
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers
import uk.gov.hmrc.customs.api.common.xml.ValidateXmlAgainstSchema
import uk.gov.hmrc.customs.declarations.information.services.VersionResponseFilterService
import util.VersionTestXMLData.{actualBackendVersionResponse, defaultDateTime, generateDeclarationVersionResponse, generateDeclarationStatusResponseContainingAllOptionalElements}
import util.UnitSpec

import scala.xml._
import scala.xml.transform.{RewriteRule, RuleTransformer}

class DeclarationVersionResponseFilterServiceSpec extends UnitSpec with MockitoSugar {
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
  val schemaFile = getSchema("/api/conf/1.0/schemas/wco/declaration/DeclarationInformationRetrievalVersionResponse.xsd")
  def xmlValidationService: ValidateXmlAgainstSchema = new ValidateXmlAgainstSchema(schemaFile.get)

  trait SetUp {
    implicit val service = new VersionResponseFilterService()
    val versionResponseWithAllValues: NodeSeq = service.transform(generateDeclarationVersionResponse(creationDate = defaultDateTime))
  }

  "Status Response Filter Service" should {

    "ensure output passes schema validation" in new SetUp {
      xmlValidationService.validate(versionResponseWithAllValues) should be(true)
    }

    "handle actual backend response" in new SetUp {
      val multiVersionResponsesWithAllValues: NodeSeq = service.transform(actualBackendVersionResponse)

      xmlValidationService.validate(multiVersionResponsesWithAllValues) should be(true)
    }

    "handle multiple DeclarationVersionDetails elements in backend response" in new SetUp {
      val numberOfDecVersions = 5
      val multiVersionResponsesWithAllValues: NodeSeq = service.transform(generateDeclarationVersionResponse(numberOfDecVersions, creationDate = defaultDateTime))

      xmlValidationService.validate(multiVersionResponsesWithAllValues) should be(true)

      val node = multiVersionResponsesWithAllValues \\ "DeclarationVersionDetails"

      node.size shouldBe numberOfDecVersions
    }

    "ensure element CreatedDateTime is present" in new SetUp {
      val node = commonPath(versionResponseWithAllValues) \ "CreatedDateTime" \ "DateTimeString"

      node.text shouldBe "20200715123000Z"
      node.head.attribute("formatCode").get.text shouldBe "304"
    }

    "ensure element LRN is present" in new SetUp {
      val node = commonPath(versionResponseWithAllValues) \ "LRN"

      node.text shouldBe "20GBAKZ81EQJ2WXYZ"
    }

    "ensure element ID is present" in new SetUp {
      val node = commonPath(versionResponseWithAllValues) \ "ID"

      node.text shouldBe "18GB9JLC3CU1LFGVR2"
    }

    "ensure element VersionID is present" in new SetUp {
      val node = commonPath(versionResponseWithAllValues) \ "VersionID"

      node.text shouldBe "1"
    }

    "handle missing VersionID element in backend response" in new SetUp {
      testForMissingElement("VersionID")
    }

    "handle missing ID element in backend response" in new SetUp {
      testForMissingElement("ID")
    }

    "handle future extension where all optional fields are returned" in new SetUp {
      val responsesWithAllValues: NodeSeq = service.transform(generateDeclarationStatusResponseContainingAllOptionalElements(defaultDateTime))

      xmlValidationService.validate(responsesWithAllValues) should be(true)
    }
  }

  private def testForMissingElement(missingElementName: String)(implicit service: VersionResponseFilterService): Assertion = {
    val missingElementSourceXml = createElementFilter(missingElementName, "n1").transform( generateDeclarationVersionResponse(creationDate = defaultDateTime) )
    val versionResponseWithMissingValues: NodeSeq = service.transform(missingElementSourceXml)

    xmlValidationService.validate(versionResponseWithMissingValues) should be(true)

    val node = commonPath(versionResponseWithMissingValues) \ missingElementName

    node.size shouldBe 0
  }

  private def commonPath(xml: NodeSeq): NodeSeq = xml \\ "DeclarationVersionDetails" \ "Declaration"
}
