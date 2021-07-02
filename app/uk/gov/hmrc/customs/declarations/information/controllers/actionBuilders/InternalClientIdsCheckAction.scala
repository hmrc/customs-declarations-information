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
  
  val declarationSubmissionChannelErrorCode = "CDS60011"

  override def refine[A](vhr: ValidatedHeadersRequest[A]): Future[Either[Result, InternalClientIdsRequest[A]]] = Future.successful {
    implicit val id: ValidatedHeadersRequest[A] = vhr
    val declarationSubmissionChannel = vhr.request.getQueryString("declarationSubmissionChannel")
    val path = vhr.request.path
    logger.debug(s"path is $path and declarationSubmissionChannel is $declarationSubmissionChannel")

    if (path.endsWith("status") && declarationSubmissionChannel.isDefined) {

      logger.warn("rejected attempt to call status endpoint with declarationSubmissionChannel parameter") //maybe this check not needed
      Left(errorBadRequest("declarationSubmissionChannel parameter not permitted in status request").XmlResult.withConversationId)

    } else if (declarationSubmissionChannel.isDefined && declarationSubmissionChannel.get.compareTo("AuthenticatedPartyOnly") != 0) {

      logger.info(s"declarationSubmissionChannel parameter passed is invalid: $declarationSubmissionChannel")
      Left(errorBadRequest("Invalid declarationSubmissionChannel parameter", declarationSubmissionChannelErrorCode).XmlResult.withConversationId)

    } else if (declarationSubmissionChannel.isDefined && declarationSubmissionChannel.get.compareTo("AuthenticatedPartyOnly") == 0
      && !configService.informationConfig.internalClientIds.contains(vhr.clientId.value)) {

      logger.info(s"declarationSubmissionChannel parameter passed but clientId: ${vhr.clientId.value} is not an internal clientId")
      Left(errorBadRequest("Invalid declarationSubmissionChannel parameter", declarationSubmissionChannelErrorCode).XmlResult.withConversationId)

    } else {
      Right(InternalClientIdsRequest(vhr.conversationId, vhr.requestedApiVersion, vhr.clientId, declarationSubmissionChannel, vhr.request))
    }
  }

}
