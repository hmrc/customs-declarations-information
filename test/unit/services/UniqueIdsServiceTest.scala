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

import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.hmrc.customs.declarations.information.model.{ConversationId, CorrelationId}
import uk.gov.hmrc.customs.declarations.information.services.{UniqueIdsService, UuidService}

import java.util.UUID

class UniqueIdsServiceTest extends AnyWordSpec with Matchers with BeforeAndAfterEach {

  private val mockUuidService: UuidService = mock(classOf[UuidService])
  private val uniqueIdsService = new UniqueIdsService(mockUuidService)

  override protected def beforeEach(): Unit = {
    reset(mockUuidService)
    when(mockUuidService.uuid()).thenReturn(UUID.randomUUID())
  }

  "UniqueIdsService" should {

    "return a correlationId" in {
      uniqueIdsService.correlation shouldBe a[CorrelationId]
      verify(mockUuidService, times(1)).uuid()
    }

    "return a conversationId" in {
      uniqueIdsService.conversation shouldBe a[ConversationId]
      verify(mockUuidService, times(1)).uuid()
    }
  }
}
