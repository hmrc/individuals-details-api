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
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.BeforeAndAfterEach
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsString, Json, OFormat}
import uk.gov.hmrc.individualsdetailsapi.cache.CacheRepository
import uk.gov.hmrc.integration.ServiceSpec
import uk.gov.hmrc.mongo.play.json.Codecs.toBson
import unit.uk.gov.hmrc.individualsdetailsapi.utils.TestSupport

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global

class CacheRepositorySpec
    extends AnyWordSpec
    with Matchers
    with ServiceSpec
    with BeforeAndAfterEach
    with TestSupport {

  val cacheTtl = 60
  val id = UUID.randomUUID().toString
  val cachekey = "test-class-key"
  val testValue = TestClass("one", "two")

  protected def databaseName: String = "test-" + this.getClass.getSimpleName
  protected def mongoUri: String     = s"mongodb://localhost:27017/$databaseName"

  override lazy val fakeApplication = new GuiceApplicationBuilder()
    .configure("mongodb.uri" -> mongoUri, "cache.ttlInSeconds" -> cacheTtl)
    .bindings(bindModules: _*)
    .build()

  val cacheRepository = fakeApplication.injector.instanceOf[CacheRepository]

  def externalServices: Seq[String] = Seq.empty

  override def beforeEach() {
    super.beforeEach()
    await(cacheRepository.collection.drop().toFuture())
  }

  override def afterEach() {
    super.afterEach()
    await(cacheRepository.collection.drop().toFuture())
  }

  "cache" should {
    "store the encrypted version of a value" in {
      await(cacheRepository.cache(id, testValue)(TestClass.format))
      retrieveRawCachedValue(id) shouldBe JsString(
        "I9gl6p5GRucOfXOFmhtiYfePGl5Nnksdk/aJFXf0iVQ=")
    }
  }

  "fetch" should {
    "retrieve the unencrypted cached value for a given id and key" in {
      await(cacheRepository.cache(id, testValue)(TestClass.format))
      await(
        cacheRepository.fetchAndGetEntry[TestClass](id)(
          TestClass.format)) shouldBe Some(testValue)
    }

    "return None if no cached value exists for a given id and key" in {
      await(
        cacheRepository.fetchAndGetEntry[TestClass](id)(
          TestClass.format)) shouldBe None
    }
  }

  private def retrieveRawCachedValue(id: String) = {
    await(cacheRepository.collection.find(Filters.equal("id", toBson(id)))
      .headOption
      .map {
        case Some(entry) => entry.data.individualsDetails
        case None => None
      })
  }

  case class TestClass(one: String, two: String)

  object TestClass {
    implicit val format: OFormat[TestClass] = Json.format[TestClass]
  }
}
