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

package unit.schemas

import org.scalatest.BeforeAndAfterEach
import play.api.test.Helpers
import uk.gov.hmrc.customs.api.common.xml.ValidateXmlAgainstSchema
import util.SearchTestXMLData.{expectedSearchPayloadRequest, validFilteredSearchResponseXML}
import util.UnitSpec

import scala.concurrent.ExecutionContext
import scala.xml.{Elem, SAXException}

class DeclarationSearchResponseSpec extends UnitSpec with BeforeAndAfterEach {

  protected implicit val ec: ExecutionContext = Helpers.stubControllerComponents().executionContext

  import ValidateXmlAgainstSchema._

  val schemaFile = getSchema("/api/conf/1.0/schemas/wco/declaration/DeclarationInformationRetrievalSearchResponse.xsd")

  def xmlValidationService: ValidateXmlAgainstSchema = new ValidateXmlAgainstSchema(schemaFile.get)

  def getFirstValidationException(xml: Elem): SAXException = {
    val result = xmlValidationService.validateWithErrors(xml)
    result.isLeft shouldBe true

    result.swap.toOption.get.head
  }

  private val invalidDeclarationSearchResponseXML = <taggie>content</taggie>

  "A search query response" should {
    "be successfully validated if correct" in {
      val result = xmlValidationService.validate(validFilteredSearchResponseXML)

      result should be(true)
    }

    "fail validation if is incorrect" in {
      val caught = getFirstValidationException(invalidDeclarationSearchResponseXML)

      caught.getMessage shouldBe "cvc-elt.1.a: Cannot find the declaration of element 'taggie'."

      Option(caught.getException) shouldBe None
    }

    "fail validation if is not filtered" in {
      val caught = getFirstValidationException(expectedSearchPayloadRequest)

      caught.getMessage shouldBe "cvc-elt.1.a: Cannot find the declaration of element 'n1:retrieveDeclarationSummaryDataRequest'."

      Option(caught.getException) shouldBe None
    }
  }

}
