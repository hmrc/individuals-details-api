/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.individualsdetailsapi.cron

import org.bson.types.ObjectId
import org.mongodb.scala.*
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.Indexes.ascending
import org.mongodb.scala.model.Updates.*
import org.mongodb.scala.model.{IndexModel, IndexOptions}
import org.mongodb.scala.result.UpdateResult
import play.api.Logging
import uk.gov.hmrc.individualsdetailsapi.cache.{CacheRepositoryConfiguration, ModifiedDetails}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class CronService @Inject() (
  mongo: MongoComponent,
  val cacheConfig: CacheRepositoryConfiguration
)(implicit val ec: ExecutionContext)
    extends PlayMongoRepository[ModifiedDetails](
      mongoComponent = mongo,
      collectionName = cacheConfig.collName,
      domainFormat = ModifiedDetails.format,
      replaceIndexes = true,
      indexes = Seq(
        IndexModel(
          ascending("id"),
          IndexOptions()
            .name("_id")
            .unique(true)
            .background(false)
            .sparse(true)
        ),
        IndexModel(
          ascending("modifiedDetails.lastUpdated"),
          IndexOptions()
            .name("lastUpdatedIndex")
            .background(false)
            .expireAfter(cacheConfig.cacheTtl.toLong, TimeUnit.SECONDS)
        )
      )
    ) with Logging {
  def updateItem(date: LocalDateTime): Unit = {
    logger.info("Updating Item")
    val filter = equal("_id", ObjectId("68dd200155b84efadd4a074e"))
    val update = set("modifiedDetails.lastUpdated", date)
    val observable: Observable[UpdateResult] = collection.updateMany(filter, update)

    observable.subscribe(new Observer[UpdateResult] {
      override def onNext(result: UpdateResult): Unit = println(result)

      override def onError(e: Throwable): Unit = println("Failed: " + e.getMessage)

      override def onComplete(): Unit = println("Completed")
    })
  }
}
