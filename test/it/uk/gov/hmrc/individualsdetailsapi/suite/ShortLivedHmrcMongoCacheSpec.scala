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

package it.uk.gov.hmrc.individualsdetailsapi.suite

import org.mongodb.scala.model.Filters
import org.scalatest.{BeforeAndAfterEach, Matchers, WordSpec}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsString, Json, OFormat}
import uk.gov.hmrc.individualsdetailsapi.cache.{ShortLivedCache, ShortLivedHmrcMongoCache}
import uk.gov.hmrc.integration.ServiceSpec
import uk.gov.hmrc.mongo.MongoSpecSupport
import uk.gov.hmrc.mongo.play.json.Codecs.toBson
import unit.uk.gov.hmrc.individualsdetailsapi.utils.TestSupport

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global

class ShortLivedHmrcMongoCacheSpec
    extends WordSpec
    with Matchers
    with MongoSpecSupport
    with ServiceSpec
    with BeforeAndAfterEach
    with TestSupport {

  val cacheTtl = 60
  val id = UUID.randomUUID().toString
  val cachekey = "test-class-key"
  val testValue = TestClass("one", "two")

  override lazy val fakeApplication = new GuiceApplicationBuilder()
    .configure("mongodb.uri" -> mongoUri, "cache.ttlInSeconds" -> cacheTtl)
    .bindings(bindModules: _*)
    .build()

  val shortLivedCache = fakeApplication.injector.instanceOf[ShortLivedHmrcMongoCache]

  def externalServices: Seq[String] = Seq.empty

  override def beforeEach() {
    super.beforeEach()
    await(shortLivedCache.collection.drop().toFuture())
  }

  override def afterEach() {
    super.afterEach()
    await(shortLivedCache.collection.drop().toFuture())
  }

  "cache" should {
    "store the encrypted version of a value" in {
      await(shortLivedCache.cache(id, cachekey, testValue)(TestClass.format))
      retrieveRawCachedValue(id, cachekey) shouldBe JsString(
        "I9gl6p5GRucOfXOFmhtiYfePGl5Nnksdk/aJFXf0iVQ=")
    }

    "update a cached value for a given id and key" in {
      val newValue = TestClass("three", "four")

      await(shortLivedCache.cache(id, cachekey, testValue)(TestClass.format))
      retrieveRawCachedValue(id, cachekey) shouldBe JsString(
        "I9gl6p5GRucOfXOFmhtiYfePGl5Nnksdk/aJFXf0iVQ=")

      await(shortLivedCache.cache(id, cachekey, newValue)(TestClass.format))
      retrieveRawCachedValue(id, cachekey) shouldBe JsString(
        "6yAvgtwLMcdiqTvdRvLTVKSkY3JwUZ/TzklThFfSqvA=")
    }
  }

  "fetch" should {
    "retrieve the unencrypted cached value for a given id and key" in {
      await(shortLivedCache.cache(id, cachekey, testValue)(TestClass.format))
      await(
        shortLivedCache.fetchAndGetEntry[TestClass](id, cachekey)(
          TestClass.format)) shouldBe Some(testValue)
    }

    "return None if no cached value exists for a given id and key" in {
      await(
        shortLivedCache.fetchAndGetEntry[TestClass](id, cachekey)(
          TestClass.format)) shouldBe None
    }
  }

  private def retrieveRawCachedValue(id: String, key: String) = {
    await(shortLivedCache.collection.find(Filters.equal("cacheId", toBson(id)))
      .headOption
      .map {
        r =>
          r match {
            case Some(entry) => entry.encryptedValue
            case None => None
          }
      })
  }

  case class TestClass(one: String, two: String)

  object TestClass {
    implicit val format: OFormat[TestClass] = Json.format[TestClass]
  }
}
