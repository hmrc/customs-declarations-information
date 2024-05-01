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

package uk.gov.hmrc.customs.declarations.information.action

import play.api.http.HttpEntity
import play.api.mvc.{ActionRefiner, Result}
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse.{BadRequestCode, errorBadRequest}
import uk.gov.hmrc.customs.declarations.information.config.ConfigService
import uk.gov.hmrc.customs.declarations.information.logging.InformationLogger
import uk.gov.hmrc.customs.declarations.information.model.ActionBuilderModelHelper._
import uk.gov.hmrc.customs.declarations.information.model.{DeclarationFullRequest, HasConversationId, InternalClientIdsRequest}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

/** Action builder that checks use of declarationVersion parameter
 * <ul>
 * <li/>INPUT - `InternalClientIdsRequest`
 * <li/>OUTPUT - `DeclarationFullRequest`
 * <li/>ERROR -
 * <ul>
 * <li/>400 if used when calling the /status endpoint
 * <li/>400 if called externally i.e. clientId not present in config
 * </ul>
 */
@Singleton
class DeclarationFullCheckAction @Inject()(val logger: InformationLogger,
                                           val configService: ConfigService)
                                          (implicit ec: ExecutionContext) extends ActionRefiner[InternalClientIdsRequest, DeclarationFullRequest] {
  override def executionContext: ExecutionContext = ec

  override def refine[A](icir: InternalClientIdsRequest[A]): Future[Either[Result, DeclarationFullRequest[A]]] = Future.successful {
    implicit val id: InternalClientIdsRequest[A] = icir
    val declarationVersion = icir.request.getQueryString("declarationVersion")
    logger.debug(s"path is ${icir.request.path} and declarationVersion is $declarationVersion")

    validateDeclarationVersion(declarationVersion) match {
      case Right(dv) => Right(DeclarationFullRequest(icir.conversationId, icir.requestedApiVersion, icir.clientId, icir.declarationSubmissionChannel, dv, icir.request))
      case Left(error) =>
        logger.warn(s"Rejected full declaration information request with status code ${error.httpStatusCode} and body\n ${error.XmlResult.body.asInstanceOf[HttpEntity.Strict].data.utf8String}")
        Left(error.XmlResult.withConversationId)
    }
  }

  def validateDeclarationVersion(declarationVersion: Option[String])(implicit request: HasConversationId): Either[ErrorResponse, Option[Int]] = {
    declarationVersion match {
      case Some(dv) => try {
        val validInteger = Integer.parseInt(dv.trim)
        if (validInteger > 0) {
          Right(Some(validInteger))
        } else {
          logger.info(s"declarationVersion query parameter was invalid: $dv")
          Left(errorBadRequest("Invalid declarationVersion parameter", BadRequestCode))
        }
      } catch {
        case nfe: NumberFormatException =>
          logger.info(s"declarationVersion query parameter was invalid: $dv exception: $nfe")
          Left(errorBadRequest("Invalid declarationVersion parameter", BadRequestCode))
      }
      case None => Right(None)
    }
  }
}
