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
import uk.gov.hmrc.individualsdetailsapi.controllers.{
  LiveMicroserviceHelloWorldController,
  SandboxMicroserviceHelloWorldController
}
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status._
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.auth.core.{
  AuthConnector,
  Enrolment,
  EnrolmentIdentifier,
  Enrolments
}
import uk.gov.hmrc.http.HeaderCarrier
import unit.uk.gov.hmrc.individualsdetailsapi.utils.SpecBase
import uk.gov.hmrc.individualsdetailsapi.service.ScopesService
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers._

import scala.concurrent.{ExecutionContext, Future}

class MicroserviceHelloWorldControllerSpec extends SpecBase with MockitoSugar {

  implicit lazy val materializer: Materializer = fakeApplication.materializer

  private val enrolments = Enrolments(
    Set(
      Enrolment("read:hello-scopes-1",
                Seq(EnrolmentIdentifier("FOO", "BAR")),
                "Activated"),
      Enrolment("read:hello-scopes-2",
                Seq(EnrolmentIdentifier("FOO2", "BAR2")),
                "Activated"),
      Enrolment("read:hello-scopes-3",
                Seq(EnrolmentIdentifier("FOO3", "BAR3")),
                "Activated")
    )
  )

  private def fakeAuthConnector(stubbedRetrievalResult: Future[_]) =
    new AuthConnector {

      def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(
          implicit hc: HeaderCarrier,
          ec: ExecutionContext): Future[A] = {
        stubbedRetrievalResult.map(_.asInstanceOf[A])
      }
    }

  private def myRetrievals = Future.successful(
    enrolments
  )

  trait Fixture {

    val scopeService = mock[ScopesService]

    val scopes: Iterable[String] =
      Iterable("read:hello-scopes-1", "read:hello-scopes-2")

    val liveMicroserviceHelloWorldController =
      new LiveMicroserviceHelloWorldController(
        fakeAuthConnector(myRetrievals),
        cc,
        scopeService
      )

    val sandboxMicroserviceHelloWorldController =
      new SandboxMicroserviceHelloWorldController(
        fakeAuthConnector(myRetrievals),
        cc,
        scopeService
      )

    when(scopeService.getEndPointScopes(any())).thenReturn(scopes)
  }

  "hello world controller" when {

    "the live controller" should {

      "hello  function" should {

        "return hello world" in new Fixture {

          val fakeRequest =
            FakeRequest("GET", s"/hello-world/")

          val result =
            await(liveMicroserviceHelloWorldController.hello()(fakeRequest))
          status(result) shouldBe OK
          bodyOf(result) shouldBe "Hello world"
        }
      }

      "hello Scopes" should {

        "return scope list" in new Fixture {

          val fakeRequest =
            FakeRequest("GET", s"/hello-scopes/")

          val result =
            await(
              liveMicroserviceHelloWorldController.helloScopes()(fakeRequest))
          status(result) shouldBe OK
          bodyOf(result) should be(
            "List(read:hello-scopes-1, read:hello-scopes-2, read:hello-scopes-3)")
        }
      }
    }

    "the sandbox controller" should {

      "hello  function" should {

        "return hello world" in new Fixture {

          val fakeRequest =
            FakeRequest("GET", s"/hello-world/")

          val result =
            await(sandboxMicroserviceHelloWorldController.hello()(fakeRequest))
          status(result) shouldBe OK
          bodyOf(result) shouldBe "Hello world"
        }
      }

      "hello Scopes" should {

        "return scope list" in new Fixture {

          val fakeRequest =
            FakeRequest("GET", s"/hello-scopes/")

          val result =
            await(
              sandboxMicroserviceHelloWorldController.helloScopes()(
                fakeRequest))
          status(result) shouldBe OK
          bodyOf(result) should be(
            "List(read:hello-scopes-1, read:hello-scopes-2)")
        }
      }
    }
  }
}
