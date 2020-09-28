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

package unit.uk.gov.hmrc.individualsdetailsapi.controllers

import akka.stream.Materializer
import uk.gov.hmrc.individualsdetailsapi.controllers.MicroserviceHelloWorldController
import org.mockito.ArgumentMatchers._
import org.scalatest.{BeforeAndAfterEach, Matchers}
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status._
import play.api.test.FakeRequest
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.individualsdetailsapi.config.AppConfig
import unit.uk.gov.hmrc.individualsdetailsapi.utils.SpecBase

class MicroserviceHelloWorldControllerSpec
    extends SpecBase
    with MockitoSugar
    with Matchers
    with BeforeAndAfterEach {
  implicit lazy val materializer: Materializer = fakeApplication.materializer

  trait Fixture {
    implicit val hc = HeaderCarrier()
  }

  val conf = mock[AppConfig]
  val mockMicroserviceHelloWorldController =
    new MicroserviceHelloWorldController(conf, cc)

  "hello function" should {

    "return hello" in new Fixture {
      val result =
        await(mockMicroserviceHelloWorldController.hello()(FakeRequest()))
      status(result) shouldBe OK
      bodyOf(result) shouldBe "Hello world"
    }
  }

}
