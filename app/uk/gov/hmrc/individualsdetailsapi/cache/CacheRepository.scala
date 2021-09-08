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

package uk.gov.hmrc.individualsdetailsapi.cache

import org.mongodb.scala.model.Indexes.ascending
import org.mongodb.scala.model.{Filters, FindOneAndReplaceOptions, IndexModel, IndexOptions}
import play.api.Configuration
import play.api.libs.json.{Format, JsValue, Json, OFormat}
import uk.gov.hmrc.crypto._
import uk.gov.hmrc.crypto.json.{JsonDecryptor, JsonEncryptor}
import uk.gov.hmrc.individualsdetailsapi.cache.InsertResult.{AlreadyExists, InsertSucceeded}
import uk.gov.hmrc.individualsdetailsapi.cache.MongoErrors.Duplicate
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.Codecs.toBson
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import java.time.Instant
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future, future}

// $COVERAGE-OFF$

case class Data(individualsDetails: JsValue)

object Data {
  implicit val format: OFormat[Data] = Json.format[Data]
}

case class Entry(cacheId: String, data: Data, createdAt: Instant)

object Entry {
  implicit val format: OFormat[Entry] = Json.format[Entry]
}

sealed trait InsertResult

object InsertResult {
  case object InsertSucceeded extends InsertResult
  case object AlreadyExists extends InsertResult
}

object MongoErrors {
  object Duplicate {
    def unapply(ex: Exception): Option[Exception] =
      if (ex.getMessage.contains("E11000")) Some(ex) else None
  }
}

@Singleton
class CacheRepository @Inject()(val cacheConfig: CacheRepositoryConfiguration,
                                configuration: Configuration,
                                mongo: MongoComponent)(implicit ec: ExecutionContext
) extends PlayMongoRepository[Entry](
  mongoComponent = mongo,
  collectionName = cacheConfig.collName,
  domainFormat   = Entry.format,
  replaceIndexes = true,
  indexes        = Seq(IndexModel(ascending("cacheId"), IndexOptions().
    name("_cacheId").
    expireAfter(cacheConfig.cacheTtl, TimeUnit.SECONDS).
    unique(true)))) {

  implicit lazy val crypto: CompositeSymmetricCrypto = new ApplicationCrypto(
    configuration.underlying).JsonCrypto

  def cache[T](id: String, value: T)(
    implicit formats: Format[T]) = {

    val jsonEncryptor           = new JsonEncryptor[T]()
    val encryptedValue: JsValue = jsonEncryptor.writes(Protected[T](value))
    val entry                   = new Entry(id, new Data(encryptedValue), Instant.now)

    collection
      .insertOne(entry)
      .toFuture
      .map(_ => InsertSucceeded)
      .recover {
        case Duplicate(_) => AlreadyExists
      }
  }

  def fetchAndGetEntry[T](id: String)(
    implicit formats: Format[T]): Future[Option[T]] = {
    val decryptor = new JsonDecryptor[T]()

    collection
      .find(Filters.equal("cacheId", toBson(id)))
      .headOption
      .map {
        case Some(entry) => decryptor.reads(entry.data.individualsDetails).asOpt map (_.decryptedValue)
        case None => None
      }
  }
}

@Singleton
class CacheRepositoryConfiguration @Inject()(configuration: Configuration) {

  lazy val cacheEnabled = configuration
    .getOptional[Boolean](
      "cache.enabled"
    )
    .getOrElse(true)

  lazy val cacheTtl = configuration
    .getOptional[Int](
      "cache.ttlInSeconds"
    )
    .getOrElse(60 * 15)

  lazy val collName = configuration
    .getOptional[String](
      "cache.collName"
    )
    .getOrElse("individuals-details-cache")

}
