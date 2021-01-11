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

package uk.gov.hmrc.individualsdetailsapi.services

import java.util.UUID

import javax.inject.{Inject, Named, Singleton}
import org.joda.time.{Interval, LocalDate}
import uk.gov.hmrc.http.{HeaderCarrier, Upstream5xxResponse}
import uk.gov.hmrc.individualsdetailsapi.connectors.{
  IfConnector,
  IndividualsMatchingApiConnector
}
import uk.gov.hmrc.individualsdetailsapi.domain.{
  ContactDetails,
  Individual,
  MatchNotFoundException,
  MatchedCitizen,
  Residence,
  Residences,
  SandboxDetailsData
}
import uk.gov.hmrc.individualsdetailsapi.domain.SandboxDetailsData._
import uk.gov.hmrc.individualsdetailsapi.domain.integrationframework.{
  IfContactDetail,
  IfDetails,
  IfDetailsResponse,
  IfResidence
}
import uk.gov.hmrc.individualsdetailsapi.service.{ScopesHelper, ScopesService}
import uk.gov.hmrc.individualsdetailsapi.services.cache.{CacheId, CacheService}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.Future.{failed, successful}

trait DetailsService {

  implicit val localDateOrdering: Ordering[LocalDate] =
    Ordering.fromLessThan(_ isBefore _)

  def resolve(matchId: UUID)(implicit hc: HeaderCarrier): Future[MatchedCitizen]

  def retrieveAndMap[T](
      matchId: UUID,
      endpoint: String,
      scopes: Iterable[String])(responseMapper: IfDetailsResponse => T)(
      implicit hc: HeaderCarrier,
      ec: ExecutionContext): Future[T]

  def getContactDetails(matchId: UUID,
                        endpoint: String,
                        scopes: Iterable[String])(
      implicit hc: HeaderCarrier,
      ec: ExecutionContext): Future[Option[ContactDetails]] = {

    retrieveAndMap[Option[ContactDetails]](matchId, endpoint, scopes) {
      response =>
        response.contactDetails.flatMap(ContactDetails.create)
    }
  }

  def getResidences(matchId: UUID, endpoint: String, scopes: Iterable[String])(
      implicit hc: HeaderCarrier,
      ec: ExecutionContext): Future[Seq[Residence]] = {

    retrieveAndMap[Seq[Residence]](matchId, endpoint, scopes) { response =>
      {
        response.residences
          .getOrElse(Seq())
          .map(Residence.create)
          .filter(_.isDefined)
          .map(_.get)
      }
    }
  }
}

@Singleton
class SandboxDetailsService @Inject()(
    cacheService: CacheService,
    ifConnector: IfConnector,
    scopesService: ScopesService,
    scopesHelper: ScopesHelper,
    individualsMatchingApiConnector: IndividualsMatchingApiConnector)
    extends DetailsService {

  override def resolve(matchId: UUID)(
      implicit hc: HeaderCarrier): Future[MatchedCitizen] =
    if (matchId.equals(sandboxMatchId))
      successful(MatchedCitizen(sandboxMatchId, sandboxNino))
    else failed(new MatchNotFoundException)

  def retrieveAndMap[T](
      matchId: UUID,
      endpoint: String,
      scopes: Iterable[String])(responseMapper: IfDetailsResponse => T)(
      implicit hc: HeaderCarrier,
      ec: ExecutionContext): Future[T] = {

    SandboxDetailsData.findByMatchId(matchId) match {
      case Some(i) =>
        Future.successful(IfDetailsResponse(i.contactDetails, i.residences)) map responseMapper
      case None => Future.failed(new MatchNotFoundException)
    }
  }
}

@Singleton
class LiveDetailsService @Inject()(
    individualsMatchingApiConnector: IndividualsMatchingApiConnector,
    ifConnector: IfConnector,
    scopesService: ScopesService,
    scopesHelper: ScopesHelper,
    @Named("retryDelay") retryDelay: Int,
    cacheService: CacheService)(implicit val ec: ExecutionContext)
    extends DetailsService {

  override def resolve(matchId: UUID)(
      implicit hc: HeaderCarrier): Future[MatchedCitizen] =
    individualsMatchingApiConnector.resolve(matchId)

  def retrieveAndMap[T](
      matchId: UUID,
      endpoint: String,
      scopes: Iterable[String])(responseMapper: IfDetailsResponse => T)(
      implicit hc: HeaderCarrier,
      ec: ExecutionContext): Future[T] = {

    val cacheId = CacheId(
      matchId,
      scopesService.getValidFieldsForCacheKey(scopes.toList, List(endpoint)))

    cacheService.get(
      cacheId, {
        resolve(matchId).flatMap(ninoMatch => {
          val fieldsQuery =
            scopesHelper.getQueryStringFor(scopes.toList, endpoint)
          ifConnector.fetchDetails(ninoMatch.nino,
                                   Option(fieldsQuery).filter(_.nonEmpty))
        })
      }
    ) map responseMapper
  }
}
