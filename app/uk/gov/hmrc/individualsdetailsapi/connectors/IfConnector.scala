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

import java.util.UUID

import javax.inject.Inject
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.RequestHeader
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.logging.Authorization
import uk.gov.hmrc.http.{
  BadRequestException,
  HeaderCarrier,
  HttpClient,
  NotFoundException,
  TooManyRequestException,
  Upstream4xxResponse
}
import uk.gov.hmrc.individualsdetailsapi.audit.AuditHelper
import uk.gov.hmrc.individualsdetailsapi.audit.models.{
  ApiIfAuditRequest,
  ApiIfFailureAuditRequest
}
import uk.gov.hmrc.individualsdetailsapi.domain.integrationframework.IfDetailsResponse
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try}

class IfConnector @Inject()(servicesConfig: ServicesConfig, http: HttpClient, val auditHelper: AuditHelper)
                           (implicit ec: ExecutionContext) {

  private val baseUrl = servicesConfig.baseUrl("integration-framework")

  private val integrationFrameworkBearerToken =
    servicesConfig.getString(
      "microservice.services.integration-framework.authorization-token"
    )

  private val integrationFrameworkEnvironment = servicesConfig.getString(
    "microservice.services.integration-framework.environment"
  )

  private val emptyResponse = IfDetailsResponse(None, None)

  def fetchDetails(nino: Nino, filter: Option[String], matchId: String)
                  (implicit hc: HeaderCarrier,
                   request: RequestHeader,
                   ec: ExecutionContext) = {

    val endpoint = "IfConnector::fetchDetails"

    val detailsUrl =
      s"$baseUrl/individuals/details/contact/nino/$nino${filter.map(f => s"?fields=$f").getOrElse("")}"

    call(detailsUrl, endpoint, matchId)

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

  private def header(extraHeaders: (String, String)*)
                    (implicit hc: HeaderCarrier) =
    hc.copy(
        authorization =
          Some(Authorization(s"Bearer $integrationFrameworkBearerToken")))
      .withExtraHeaders(
        Seq("Environment" -> integrationFrameworkEnvironment) ++ extraHeaders: _*)

  private def call(url: String, endpoint: String, matchId: String)
                  (implicit hc: HeaderCarrier, request: RequestHeader, ec: ExecutionContext) =
    recover[IfDetailsResponse](http.GET[IfDetailsResponse](url)(implicitly, header(), ec) map {
        response =>
          Logger.debug(s"$endpoint - Response: $response")

          auditHelper.auditIfApiResponse(
            ApiIfAuditRequest(extractCorrelationId(request), None, Some(matchId), request, url, Json.toJson(response))
          )

          response
      },
      emptyResponse,
      ApiIfFailureAuditRequest(extractCorrelationId(request), None, Some(matchId), request, url))

  private def recover[A](x: Future[A], emptyResponse: A, apiIfFailedAuditRequest: ApiIfFailureAuditRequest)
                        (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] = x.recoverWith {
      case notFound: NotFoundException => {
        auditHelper.auditIfApiFailure(apiIfFailedAuditRequest,
                                      notFound.getMessage)
        Future.successful(emptyResponse)
      }
      case Upstream4xxResponse(msg, 429, _, _) => {
        Logger.warn(s"IF Rate limited: $msg")
        auditHelper.auditIfApiFailure(apiIfFailedAuditRequest,
                                      s"IF Rate limited: $msg")
        Future.failed(new TooManyRequestException(msg))
      }
      case Upstream4xxResponse(msg, _, _, _) => {
        auditHelper.auditIfApiFailure(apiIfFailedAuditRequest, msg)
        Future.failed(
          new IllegalArgumentException(
            s"Integration Framework returned INVALID_REQUEST"))
      }
      case e: Exception => {
        auditHelper.auditIfApiFailure(apiIfFailedAuditRequest, e.getMessage)
        Future.failed(e)
      }
    }

}
