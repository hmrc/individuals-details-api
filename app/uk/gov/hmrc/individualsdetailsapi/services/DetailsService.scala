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

package uk.gov.hmrc.individualsdetailsapi.services

import play.api.mvc.RequestHeader
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.individualsdetailsapi.connectors.{IfConnector, IndividualsMatchingApiConnector}
import uk.gov.hmrc.individualsdetailsapi.domain.*
import uk.gov.hmrc.individualsdetailsapi.domain.integrationframework.IfDetailsResponse
import uk.gov.hmrc.individualsdetailsapi.services.cache.{CacheId, CacheService}

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DetailsService @Inject() (
  individualsMatchingApiConnector: IndividualsMatchingApiConnector,
  ifConnector: IfConnector,
  scopesService: ScopesService,
  scopesHelper: ScopesHelper,
  cacheService: CacheService
)(implicit val ec: ExecutionContext) {

  def getContactDetails(matchId: UUID, endpoint: String, scopes: Iterable[String])(implicit
    hc: HeaderCarrier,
    request: RequestHeader,
    ec: ExecutionContext
  ): Future[Option[ContactDetails]] =
    retrieveAndMap[Option[ContactDetails]](matchId, endpoint, scopes) { response =>
      response.contactDetails.flatMap(ContactDetails.convert)
    }

  def getResidences(matchId: UUID, endpoint: String, scopes: Iterable[String])(implicit
    hc: HeaderCarrier,
    request: RequestHeader,
    ec: ExecutionContext
  ): Future[Seq[Residence]] =
    retrieveAndMap[Seq[Residence]](matchId, endpoint, scopes) { response =>
      response.residences
        .getOrElse(Seq())
        .map(Residence.convert)
        .filter(_.isDefined)
        .map(_.get)
    }

  def resolve(matchId: UUID)(implicit hc: HeaderCarrier): Future[MatchedCitizen] =
    individualsMatchingApiConnector.resolve(matchId)

  def retrieveAndMap[T](matchId: UUID, endpoint: String, scopes: Iterable[String])(
    responseMapper: IfDetailsResponse => T
  )(implicit hc: HeaderCarrier, request: RequestHeader, ec: ExecutionContext): Future[T] = {

    val cacheId = CacheId(matchId, scopesService.getValidFieldsForCacheKey(scopes.toList, List(endpoint)))

    cacheService.get(
      cacheId,
      resolve(matchId).flatMap { ninoMatch =>
        val fieldsQuery =
          scopesHelper.getQueryStringFor(scopes.toList, List(endpoint))
        ifConnector.fetchDetails(ninoMatch.nino, Option(fieldsQuery).filter(_.nonEmpty), matchId.toString)
      }
    ) map responseMapper
  }
}
