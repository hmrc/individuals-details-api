/*
 * Copyright 2023 HM Revenue & Customs
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

import play.api.Logging
import play.api.mvc.RequestHeader
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.UpstreamErrorResponse.Upstream5xxResponse
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames, InternalServerException, JsValidationException, NotFoundException, StringContextOps, TooManyRequestException, UpstreamErrorResponse}
import uk.gov.hmrc.individualsdetailsapi.audit.AuditHelper
import uk.gov.hmrc.individualsdetailsapi.domain.integrationframework.IfDetailsResponse
import uk.gov.hmrc.individualsdetailsapi.play.RequestHeaderUtils
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class IfConnector @Inject() (servicesConfig: ServicesConfig, http: HttpClientV2, val auditHelper: AuditHelper)
    extends Logging {

  private val baseUrl = servicesConfig.baseUrl("integration-framework")

  private val integrationFrameworkBearerToken =
    servicesConfig.getString(
      "microservice.services.integration-framework.authorization-token"
    )

  private val integrationFrameworkEnvironment = servicesConfig.getString(
    "microservice.services.integration-framework.environment"
  )

  private val emptyResponse = IfDetailsResponse(None, None)

  def fetchDetails(nino: Nino, filter: Option[String], matchId: String)(implicit
    hc: HeaderCarrier,
    request: RequestHeader,
    ec: ExecutionContext
  ): Future[IfDetailsResponse] = {

    val detailsUrl =
      s"$baseUrl/individuals/details/contact/nino/$nino${filter.map(f => s"?fields=$f").getOrElse("")}"

    call(detailsUrl, matchId)
  }

  private def extractCorrelationId(requestHeader: RequestHeader) =
    RequestHeaderUtils.validateCorrelationId(requestHeader).toString

  private def setHeaders(requestHeader: RequestHeader) = Seq(
    HeaderNames.authorisation -> s"Bearer $integrationFrameworkBearerToken",
    "Environment"             -> integrationFrameworkEnvironment,
    "CorrelationId"           -> extractCorrelationId(requestHeader)
  )

  private def call(url: String, matchId: String)(implicit
    hc: HeaderCarrier,
    request: RequestHeader,
    ec: ExecutionContext
  ) =
    recover(
      http.get(url"$url").transform(_.addHttpHeaders(setHeaders(request)*)).execute[IfDetailsResponse] map { response =>
        auditHelper.auditIfApiResponse(extractCorrelationId(request), matchId, request, url, response)
        response
      },
      extractCorrelationId(request),
      matchId,
      request,
      url
    )

  private def recover(
    x: Future[IfDetailsResponse],
    correlationId: String,
    matchId: String,
    request: RequestHeader,
    requestUrl: String
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[IfDetailsResponse] = x.recoverWith {

    case validationError: JsValidationException =>
      logger.warn("Integration Framework JsValidationException encountered")
      auditHelper.auditIfApiFailure(
        correlationId,
        matchId,
        request,
        requestUrl,
        s"Error parsing IF response: ${validationError.errors}"
      )
      Future.failed(new InternalServerException("Something went wrong."))
    case UpstreamErrorResponse(msg, 404, _, _) =>
      auditHelper.auditIfApiFailure(correlationId, matchId, request, requestUrl, msg)

      if (msg.contains("PERSON_NOT_FOUND")) {
        Future.successful(emptyResponse)
      } else {
        logger.warn("Integration Framework NotFoundException encountered")
        Future.failed(new NotFoundException(msg))
      }
    case Upstream5xxResponse(UpstreamErrorResponse(msg, code, _, _)) =>
      logger.warn(s"Integration Framework Upstream5xxResponse encountered: $code")
      auditHelper.auditIfApiFailure(correlationId, matchId, request, requestUrl, s"Internal Server error: $msg")
      Future.failed(new InternalServerException("Something went wrong."))
    case UpstreamErrorResponse(msg, 429, _, _) =>
      logger.warn(s"Integration Framework Rate limited: $msg")
      auditHelper.auditIfApiFailure(correlationId, matchId, request, requestUrl, s"IF Rate limited: $msg")
      Future.failed(new TooManyRequestException(msg))
    case UpstreamErrorResponse(msg, code, _, _) =>
      logger.warn(s"Integration Framework Upstream4xxResponse encountered: $code")
      auditHelper.auditIfApiFailure(correlationId, matchId, request, requestUrl, msg)
      Future.failed(new InternalServerException("Something went wrong."))
  }
}
