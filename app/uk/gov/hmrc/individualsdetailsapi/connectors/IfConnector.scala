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

package uk.gov.hmrc.individualsdetailsapi.connectors

import play.api.Logger
import play.api.mvc.RequestHeader
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.logging.Authorization
import uk.gov.hmrc.individualsdetailsapi.audit.AuditHelper
import uk.gov.hmrc.individualsdetailsapi.domain.integrationframework.IfDetailsResponse
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import java.util.UUID

import javax.inject.Inject
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, HttpClient, InternalServerException, JsValidationException, NotFoundException, TooManyRequestException, Upstream4xxResponse, Upstream5xxResponse}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try}

class IfConnector @Inject()(
    servicesConfig: ServicesConfig,
    http: HttpClient,
    val auditHelper: AuditHelper)(implicit ec: ExecutionContext) {

  private val baseUrl = servicesConfig.baseUrl("integration-framework")

  private val integrationFrameworkBearerToken =
    servicesConfig.getString(
      "microservice.services.integration-framework.authorization-token"
    )

  private val integrationFrameworkEnvironment = servicesConfig.getString(
    "microservice.services.integration-framework.environment"
  )

  private val emptyResponse = IfDetailsResponse(None, None)

  def fetchDetails(nino: Nino, filter: Option[String], matchId: String)(
      implicit hc: HeaderCarrier,
      request: RequestHeader,
      ec: ExecutionContext) = {

    val detailsUrl =
      s"$baseUrl/individuals/details/contact/nino/$nino${filter.map(f => s"?fields=$f").getOrElse("")}"

    call(detailsUrl, matchId)

  }

  private def extractCorrelationId(requestHeader: RequestHeader) =
    requestHeader.headers.get("CorrelationId") match {
      case Some(uuidString) =>
        Try(UUID.fromString(uuidString)) match {
          case Success(_) => uuidString
          case _          => throw new BadRequestException("Malformed CorrelationId")
        }
      case None => throw new BadRequestException("CorrelationId is required")
    }

  private def header(extraHeaders: (String, String)*)(
      implicit hc: HeaderCarrier) =
    hc.copy(
        authorization =
          Some(Authorization(s"Bearer $integrationFrameworkBearerToken")))
      .withExtraHeaders(
        Seq("Environment" -> integrationFrameworkEnvironment) ++ extraHeaders: _*)

  private def call(url: String, matchId: String)
                      (implicit hc: HeaderCarrier, request: RequestHeader, ec: ExecutionContext) =
    recover(http.GET[IfDetailsResponse](url)(implicitly, header(), ec) map { response =>
      auditHelper.auditIfApiResponse(extractCorrelationId(request), matchId, request, url, response)
      response
    }, extractCorrelationId(request), matchId, request, url)

  private def recover(x: Future[IfDetailsResponse],
                         correlationId: String,
                         matchId: String,
                         request: RequestHeader,
                         requestUrl: String)
                        (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[IfDetailsResponse] = x.recoverWith {

    case validationError: JsValidationException => {
      Logger.warn("Integration Framework JsValidationException encountered")
      auditHelper.auditIfApiFailure(correlationId, matchId, request, requestUrl, s"Error parsing IF response: ${validationError.errors}")
      Future.failed(new InternalServerException("Something went wrong."))
    }
    case notFound: NotFoundException => {
      auditHelper.auditIfApiFailure(correlationId, matchId, request, requestUrl, notFound.getMessage)
      
      notFound.message.contains("PERSON_NOT_FOUND") match {
        case true => Future.successful(emptyResponse)
        case _    => {
          Logger.warn("Integration Framework NotFoundException encountered")
          Future.failed(notFound)
        }
      }
    }
    case Upstream5xxResponse(msg, code, _, _) => {
      Logger.warn(s"Integration Framework Upstream5xxResponse encountered: $code")
      auditHelper.auditIfApiFailure(correlationId, matchId, request, requestUrl, s"Internal Server error: $msg")
      Future.failed(new InternalServerException("Something went wrong."))
    }
    case Upstream4xxResponse(msg, 429, _, _) => {
      Logger.warn(s"Integration Framework Rate limited: $msg")
      auditHelper.auditIfApiFailure(correlationId, matchId, request, requestUrl, s"IF Rate limited: $msg")
      Future.failed(new TooManyRequestException(msg))
    }
    case Upstream4xxResponse(msg, code, _, _) => {
      Logger.warn(s"Integration Framework Upstream4xxResponse encountered: $code")
      auditHelper.auditIfApiFailure(correlationId, matchId, request, requestUrl, msg)
      Future.failed(new InternalServerException("Something went wrong."))
    }
    case e: Exception => {
      Logger.warn(s"Integration Framework Exception encountered")
      auditHelper.auditIfApiFailure(correlationId, matchId, request, requestUrl, e.getMessage)
      Future.failed(new InternalServerException("Something went wrong."))
    }
  }
}
