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

import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps, UpstreamErrorResponse}
import uk.gov.hmrc.individualsdetailsapi.domain.JsonFormatters._
import uk.gov.hmrc.individualsdetailsapi.domain.{MatchNotFoundException, MatchedCitizen}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class IndividualsMatchingApiConnector @Inject() (servicesConfig: ServicesConfig, http: HttpClientV2)(implicit
  ec: ExecutionContext
) {

  private[connectors] val serviceUrl =
    servicesConfig.baseUrl("individuals-matching-api")

  def resolve(matchId: UUID)(implicit hc: HeaderCarrier): Future[MatchedCitizen] =
    http.get(url"$serviceUrl/match-record/$matchId").execute[MatchedCitizen] recover {
      case UpstreamErrorResponse(_, 404, _, _) =>
        throw new MatchNotFoundException
    }

}
