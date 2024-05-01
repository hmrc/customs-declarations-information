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

package uk.gov.hmrc.customs.declarations.information.services

import com.google.inject.Singleton
import uk.gov.hmrc.customs.declarations.information.model.{ConversationId, CorrelationId}
import javax.inject.Inject

//TODO can be an object? and why not just call the uuidService directly?
@Singleton
class UniqueIdsService @Inject()(uuidService: UuidService) {
  def generateUniqueConversationId: ConversationId = ConversationId(uuidService.uuid())
  def generateUniqueCorrelationId: CorrelationId = CorrelationId(uuidService.uuid())
}
