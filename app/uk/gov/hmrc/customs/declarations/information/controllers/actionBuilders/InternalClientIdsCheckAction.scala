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

package uk.gov.hmrc.customs.declarations.information.controllers.actionBuilders

import play.api.mvc.{ActionRefiner, Result}
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse.errorBadRequest
import uk.gov.hmrc.customs.declarations.information.logging.InformationLogger
import uk.gov.hmrc.customs.declarations.information.model.actionbuilders.ActionBuilderModelHelper._
import uk.gov.hmrc.customs.declarations.information.model.actionbuilders.{InternalClientIdsRequest, ValidatedHeadersRequest}
import uk.gov.hmrc.customs.declarations.information.services.InformationConfigService

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

/** Action builder that checks use of authenticatedParty parameter
 * <ul>
 * <li/>INPUT - `ValidatedHeadersRequest`
 * <li/>OUTPUT - `InternalClientIdsRequest`
 * <li/>ERROR -
 * <ul>
 * <li/>400 if used when calling the /status endpoint
 * <li/>400 if called externally i.e. clientId not present in config
 * </ul>
 */
@Singleton
class InternalClientIdsCheckAction @Inject()(val logger: InformationLogger,
                                             val configService: InformationConfigService)
                                            (implicit ec: ExecutionContext)
  extends ActionRefiner[ValidatedHeadersRequest, InternalClientIdsRequest] {

  override def executionContext: ExecutionContext = ec
  override def refine[A](vhr: ValidatedHeadersRequest[A]): Future[Either[Result, InternalClientIdsRequest[A]]] = Future.successful {
    implicit val id: ValidatedHeadersRequest[A] = vhr
    val authenticatedParty = convertToBoolean(vhr.request.getQueryString("authenticatedParty"))
    val path = vhr.request.path
    logger.debug(s"path is $path and authenticatedParty is $authenticatedParty")

    if (path.endsWith("status") && vhr.request.getQueryString("authenticatedParty").isDefined) {
      logger.warn("rejected attempt to call status endpoint with authenticatedParty parameter")
      Left(errorBadRequest("authenticatedParty parameter not permitted in status request").XmlResult.withConversationId)
    } else if (authenticatedParty && !configService.informationConfig.internalClientIds.contains(vhr.clientId.value)) {
      Left(errorBadRequest("authenticatedParty parameter is for internal use only").XmlResult.withConversationId)
    }
    else {
      Right(InternalClientIdsRequest(vhr.conversationId, vhr.requestedApiVersion, vhr.clientId, authenticatedParty, vhr.request))
    }
  }
  
  private def convertToBoolean(param: Option[String]): Boolean = {
    param.fold(false){p =>
      if(p.equalsIgnoreCase("true")) true else false
    }
  }
  
}
