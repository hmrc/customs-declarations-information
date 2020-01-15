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

package unit.services

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}

import org.mockito.Mockito.when
import org.scalatest.Assertion
import org.scalatestplus.mockito.MockitoSugar
import play.api.Configuration
import play.api.test.Helpers
import uk.gov.hmrc.customs.declarations.information.logging.InformationLogger
import uk.gov.hmrc.customs.declarations.information.services.{InformationConfigService, StatusResponseFilterService}
import uk.gov.hmrc.customs.declarations.information.xml.HelperXMLUtils
import uk.gov.hmrc.play.test.UnitSpec
import util.StatusTestXMLData.{defaultDateTime, generateDeclarationStatusResponse, generateExportDeclarationStatusResponse}
import util.XmlValidationService

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

  trait SetUp {
    val mockInformationLogger: InformationLogger = mock[InformationLogger]
    val mockInformationConfigService: InformationConfigService = mock[InformationConfigService]
    implicit val service = new StatusResponseFilterService(mockInformationLogger, mockInformationConfigService)
    val statusResponseWithAllValues: NodeSeq = service.transform(generateDeclarationStatusResponse(acceptanceOrCreationDate = defaultDateTime))
  }

  "Status Response Filter Service" should {

    "ensure response passes schema validation" in new SetUp {
      val node = generateDeclarationStatusResponse(acceptanceOrCreationDate = defaultDateTime)

      println(node.head)
      HelperXMLUtils.extractNamespaceBindings(node.head.asInstanceOf[Elem]).foreach(ns => println(s"${ns.prefix}->${ns.uri}"))

      validateAgainstSchema(statusResponseWithAllValues.head)
    }

    "ensure ReceivedDateTime is present" in new SetUp {
      val node = commonPath(statusResponseWithAllValues) \ "ReceivedDateTime" \ "DateTimeString"

      node.text shouldBe "20200615122900Z"
      node.head.attribute("formatCode").get.text shouldBe "304"
    }

    "ensure GoodsReleasedDateTime is present" in new SetUp {
      val node = commonPath(statusResponseWithAllValues) \ "GoodsReleasedDateTime" \ "DateTimeString"

      node.text shouldBe "20200615123100Z"
      node.head.attribute("formatCode").get.text shouldBe "304"
    }

    "ensure ROE is present" in new SetUp {
      val node = commonPath(statusResponseWithAllValues) \ "ROE"

      node.text shouldBe "6"
    }

    "ensure ICS is present" in new SetUp {
      val node = commonPath(statusResponseWithAllValues) \ "ICS"

      node.text shouldBe "15"
    }

    "ensure IRC is present" in new SetUp {
      val node = commonPath(statusResponseWithAllValues) \ "IRC"

      node.text shouldBe "000"
    }

    "ensure AcceptanceDateTime is present" in new SetUp {
      val node = commonPath(statusResponseWithAllValues) \ "AcceptanceDateTime" \ "DateTimeString"

      node.text shouldBe "20200615123000Z"
      node.head.attribute("formatCode").get.text shouldBe "304"
    }

    "ensure ID is present" in new SetUp {
      val node = commonPath(statusResponseWithAllValues) \ "ID"

      node.text shouldBe "18GB9JLC3CU1LFGVR2"
    }

    "ensure VersionID is present" in new SetUp {
      val node = commonPath(statusResponseWithAllValues) \ "VersionID"

      node.text shouldBe "1"
    }

    "ensure transformer can handle multiple DeclarationStatusDetails elements" in new SetUp {
      val numberOfDecStatuses = 5
      val multuStatusResponsesWithAllValues: NodeSeq = service.transform(generateDeclarationStatusResponse(numberOfDecStatuses, acceptanceOrCreationDate = defaultDateTime))

      validateAgainstSchema(multuStatusResponsesWithAllValues.head)

      val node = multuStatusResponsesWithAllValues \\ "DeclarationStatusDetails"

      node.size shouldBe numberOfDecStatuses
    }

    "ensure transformer can handle missing ReceivedDateTime element" in new SetUp {
      testForMissingElement("ReceivedDateTime")
    }

    "ensure transformer can handle missing GoodsReleasedDateTime element" in new SetUp {
      testForMissingElement("GoodsReleasedDateTime")
    }

    "ensure transformer can handle missing ROE element" in new SetUp {
      testForMissingElement("ROE")
    }

    "ensure transformer can handle missing IRC element" in new SetUp {
      testForMissingElement("IRC")
    }

    "ensure transformer can handle missing AcceptanceDateTime element" in new SetUp {
      testForMissingElement("AcceptanceDateTime")
    }

    "ensure transformer can handle missing ID element" in new SetUp {
      testForMissingElement("ID")
    }

    "ensure transformer can handle missing VersionID element" in new SetUp {
      testForMissingElement("VersionID")
    }

    "ensure transformer can handle a full populated wco export schema" in new SetUp {
      val transformed: NodeSeq = service.transform(generateExportDeclarationStatusResponse())

      val pp = new scala.xml.PrettyPrinter(1024, 4)
//      println(pp.format(transformed.head))

      Files.write(Paths.get("gen_out.xml"), pp.format(transformed.head).getBytes(StandardCharsets.UTF_8))

//      validateAgainstSchema(transformed.head)
    }
  }

  private def validateAgainstSchema(xml: NodeSeq): Assertion = {
    val mockConfiguration = mock[Configuration]
    val propertyName: String = "xsd.locations.statusqueryresponse"
    val xsdLocations: Seq[String] = Seq("/api/conf/1.0/schemas/wco/declaration/declarationInformationRetrievalStatusResponse.xsd")

    when(mockConfiguration.getOptional[Seq[String]](propertyName)).thenReturn(Some(xsdLocations))
    when(mockConfiguration.getOptional[Int]("xml.max-errors")).thenReturn(None)

    val xmlValidationService = new XmlValidationService(mockConfiguration, schemaPropertyName = propertyName) {}

    await(xmlValidationService.validate(xml)) should be(())
  }

  private def testForMissingElement(missingElementName: String)(implicit service: StatusResponseFilterService): Assertion = {
    val missingElementSourceXml = createElementFilter(missingElementName, "n1").transform( generateDeclarationStatusResponse(acceptanceOrCreationDate = defaultDateTime) )
    val statusResponseWithMissingValues: NodeSeq = service.transform(missingElementSourceXml)

    validateAgainstSchema(statusResponseWithMissingValues.head)

    val node = commonPath(statusResponseWithMissingValues) \ missingElementName

    node.size shouldBe 0
  }

  private def commonPath(xml: NodeSeq): NodeSeq = xml \\ "DeclarationStatusDetails" \ "Declaration"
}
