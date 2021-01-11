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
import uk.gov.hmrc.customs.declarations.information.services.StatusResponseFilterService
import util.StatusTestXMLData.{actualBackendStatusResponse, defaultDateTime, generateDeclarationStatusResponse, generateDeclarationStatusResponseContainingAllOptionalElements}
import util.UnitSpec

import scala.xml._
import scala.xml.transform.{RewriteRule, RuleTransformer}

class DeclarationStatusResponseFilterServiceSpec extends UnitSpec with MockitoSugar {
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
  val schemaFile = getSchema("/api/conf/1.0/schemas/wco/declaration/DeclarationInformationRetrievalStatusResponse.xsd")
  def xmlValidationService: ValidateXmlAgainstSchema = new ValidateXmlAgainstSchema(schemaFile.get)

  trait SetUp {
    implicit val service = new StatusResponseFilterService()
    val statusResponseWithAllValues: NodeSeq = service.transform(generateDeclarationStatusResponse(acceptanceOrCreationDate = defaultDateTime))
  }

  "Status Response Filter Service" should {

    "ensure output passes schema validation" in new SetUp {
      xmlValidationService.validate(statusResponseWithAllValues) should be(true)
    }

    "handle actual MDG response" in new SetUp {
      val multuStatusResponsesWithAllValues: NodeSeq = service.transform(actualBackendStatusResponse)

      xmlValidationService.validate(multuStatusResponsesWithAllValues) should be(true)
    }

    "handle multiple DeclarationStatusDetails elements in MDG response" in new SetUp {
      val numberOfDecStatuses = 5
      val multuStatusResponsesWithAllValues: NodeSeq = service.transform(generateDeclarationStatusResponse(numberOfDecStatuses, acceptanceOrCreationDate = defaultDateTime))

      xmlValidationService.validate(multuStatusResponsesWithAllValues) should be(true)

      val node = multuStatusResponsesWithAllValues \\ "DeclarationStatusDetails"

      node.size shouldBe numberOfDecStatuses
    }

    "ensure element ReceivedDateTime is present" in new SetUp {
      val node = commonPath(statusResponseWithAllValues) \ "ReceivedDateTime" \ "DateTimeString"

      node.text shouldBe "20200615122900Z"
      node.head.attribute("formatCode").get.text shouldBe "304"
    }

    "ensure element GoodsReleasedDateTime is present" in new SetUp {
      val node = commonPath(statusResponseWithAllValues) \ "GoodsReleasedDateTime" \ "DateTimeString"

      node.text shouldBe "20200615123100Z"
      node.head.attribute("formatCode").get.text shouldBe "304"
    }

    "ensure element ROE is present" in new SetUp {
      val node = commonPath(statusResponseWithAllValues) \ "ROE"

      node.text shouldBe "6"
    }

    "ensure element ICS is present" in new SetUp {
      val node = commonPath(statusResponseWithAllValues) \ "ICS"

      node.text shouldBe "15"
    }

    "ensure element IRC is present" in new SetUp {
      val node = commonPath(statusResponseWithAllValues) \ "IRC"

      node.text shouldBe "000"
    }

    "ensure element AcceptanceDateTime is present" in new SetUp {
      val node = commonPath(statusResponseWithAllValues) \ "AcceptanceDateTime" \ "DateTimeString"

      node.text shouldBe "20200615123000Z"
      node.head.attribute("formatCode").get.text shouldBe "304"
    }

    "ensure element ID is present" in new SetUp {
      val node = commonPath(statusResponseWithAllValues) \ "ID"

      node.text shouldBe "18GB9JLC3CU1LFGVR2"
    }

    "ensure element VersionID is present" in new SetUp {
      val node = commonPath(statusResponseWithAllValues) \ "VersionID"

      node.text shouldBe "1"
    }

    "handle missing ReceivedDateTime element in MDG response" in new SetUp {
      testForMissingElement("ReceivedDateTime")
    }

    "handle missing GoodsReleasedDateTime element in MDG response" in new SetUp {
      testForMissingElement("GoodsReleasedDateTime")
    }

    "handle missing ROE element in MDG response" in new SetUp {
      testForMissingElement("ROE")
    }

    "handle missing IRC element in MDG response" in new SetUp {
      testForMissingElement("IRC")
    }

    "handle missing AcceptanceDateTime element in MDG response" in new SetUp {
      testForMissingElement("AcceptanceDateTime")
    }

    "handle missing ID element in MDG response" in new SetUp {
      testForMissingElement("ID")
    }

    "handle missing VersionID element in MDG response" in new SetUp {
      testForMissingElement("VersionID")
    }

    "handle future extension where all optional fields are returned" in new SetUp {
      val responsesWithAllValues: NodeSeq = service.transform(generateDeclarationStatusResponseContainingAllOptionalElements(defaultDateTime))

      xmlValidationService.validate(responsesWithAllValues) should be(true)
    }
  }

  private def testForMissingElement(missingElementName: String)(implicit service: StatusResponseFilterService): Assertion = {
    val missingElementSourceXml = createElementFilter(missingElementName, "n1").transform( generateDeclarationStatusResponse(acceptanceOrCreationDate = defaultDateTime) )
    val statusResponseWithMissingValues: NodeSeq = service.transform(missingElementSourceXml)

    xmlValidationService.validate(statusResponseWithMissingValues) should be(true)

    val node = commonPath(statusResponseWithMissingValues) \ missingElementName

    node.size shouldBe 0
  }

  private def commonPath(xml: NodeSeq): NodeSeq = xml \\ "DeclarationStatusDetails" \ "Declaration"
}
