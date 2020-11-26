/*
 * Copyright 2020 HM Revenue & Customs
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

import javax.inject.Inject
import org.joda.time.{Interval, LocalDate}
import play.api.Logger
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.logging.Authorization
import uk.gov.hmrc.http.{
  HeaderCarrier,
  HttpClient,
  NotFoundException,
  TooManyRequestException,
  Upstream4xxResponse
}
import uk.gov.hmrc.individualsdetailsapi.domains.integrationframework.{
  IfDetails,
  IfDetailsResponse
}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.{ExecutionContext, Future}

class IfConnector @Inject()(
    servicesConfig: ServicesConfig,
    http: HttpClient)(implicit ec: ExecutionContext) {

  private val baseUrl = servicesConfig.baseUrl("integration-framework")
  private val integrationFrameworkBearerToken =
    servicesConfig.getString(
      "microservice.services.integration-framework.authorization-token")
  private val integrationFrameworkEnvironment =
    servicesConfig.getString(
      "microservice.services.integration-framework.environment")

  private val emptyResponse =
    IfDetailsResponse(IfDetails(None, None), None, None)

  def fetchDetails(nino: Nino, filter: Option[String])(
      implicit hc: HeaderCarrier,
      ec: ExecutionContext) = {

    val detailsUrl = s"$baseUrl/individuals/details/nino/$nino?fields=$filter"

    recover[IfDetailsResponse](
      http.GET[IfDetailsResponse](detailsUrl)(implicitly, header(), ec),
      emptyResponse)

  }

  private def header(extraHeaders: (String, String)*)(
      implicit hc: HeaderCarrier) =
    // The correlationId should be passed in by the caller and will already be present in hc
    hc.copy(
        authorization =
          Some(Authorization(s"Bearer $integrationFrameworkBearerToken")))
      .withExtraHeaders(
        Seq("Environment" -> integrationFrameworkEnvironment) ++ extraHeaders: _*)

  private def recover[A](x: Future[A], emptyResponse: A): Future[A] =
    x.recoverWith {
      case _: NotFoundException => Future.successful(emptyResponse)
      case Upstream4xxResponse(msg, 429, _, _) => {
        Logger.warn(s"IF Rate limited: $msg")
        Future.failed(new TooManyRequestException(msg))
      }
    }

}
