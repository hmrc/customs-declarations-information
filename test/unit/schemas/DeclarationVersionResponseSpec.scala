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

package unit.schemas

import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers
import uk.gov.hmrc.customs.api.common.xml.ValidateXmlAgainstSchema
import util.UnitSpec
import util.VersionTestXMLData.{expectedVersionPayloadRequest, validFilteredVersionResponseXML}

import scala.xml.{Elem, SAXException}

class DeclarationVersionResponseSpec extends UnitSpec with MockitoSugar with BeforeAndAfterEach {

  protected implicit val ec = Helpers.stubControllerComponents().executionContext

  import ValidateXmlAgainstSchema._
  val schemaFile = getSchema("/api/conf/1.0/schemas/wco/declaration/DeclarationInformationRetrievalVersionResponse.xsd")
  def xmlValidationService: ValidateXmlAgainstSchema = new ValidateXmlAgainstSchema(schemaFile.get)

  def getFirstValidationException(xml: Elem): SAXException = {
    val result = xmlValidationService.validateWithErrors(xml)
    result.isLeft shouldBe true

    result.left.get.head
  }

  private val invalidDeclarationVersionResponseXML = <taggie>content</taggie>

  "A version query response" should {
    "be successfully validated if correct" in {
      val result = xmlValidationService.validate(validFilteredVersionResponseXML)

      result should be(true)
    }

    "fail validation if is incorrect" in {
      val caught = getFirstValidationException(invalidDeclarationVersionResponseXML)

      caught.getMessage shouldBe "cvc-elt.1.a: Cannot find the declaration of element 'taggie'."

      Option(caught.getException) shouldBe None
    }

    "fail validation if is not filtered" in {
      val caught = getFirstValidationException(expectedVersionPayloadRequest)

      caught.getMessage shouldBe "cvc-elt.1.a: Cannot find the declaration of element 'ns1:retrieveDeclarationVersionRequest'."

      Option(caught.getException) shouldBe None
    }
  }

}
