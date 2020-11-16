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
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
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
import uk.gov.hmrc.individualsdetailsapi.controllers.{
  LiveAddressesController,
  SandboxAddressesController
}
import uk.gov.hmrc.individualsdetailsapi.service.ScopesService
import unit.uk.gov.hmrc.individualsdetailsapi.utils.SpecBase

import scala.concurrent.{ExecutionContext, Future}

class AddressesControllerSpec extends SpecBase with MockitoSugar {
  implicit lazy val materializer: Materializer = fakeApplication.materializer
  implicit lazy val ec: ExecutionContext =
    fakeApplication.injector.instanceOf[ExecutionContext]

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

    val liveAddressesController =
      new LiveAddressesController(
        fakeAuthConnector(myRetrievals),
        cc,
        scopeService
      )

    val sandboxAddressesController =
      new SandboxAddressesController(
        fakeAuthConnector(myRetrievals),
        cc,
        scopeService
      )

    when(scopeService.getEndPointScopes(any())).thenReturn(scopes)
  }

  "addresses controller" when {

    "the live controller" should {

      "addresses function" should {

        "throw an exception" in new Fixture {

          val fakeRequest =
            FakeRequest("GET", s"/addresses/")

          val result =
            intercept[Exception] {
              await(liveAddressesController.addresses()(fakeRequest))
            }
          assert(result.getMessage == "NOT_IMPLEMENTED")
        }

        "return error when no scopes" in new Fixture {
          when(scopeService.getEndPointScopes(any())).thenReturn(None)

          val fakeRequest =
            FakeRequest("GET", s"/addresses/")

          val result =
            intercept[Exception] {
              await(liveAddressesController.addresses()(fakeRequest))
            }
          assert(result.getMessage == "No scopes defined")
        }
      }
    }

    "the sandbox controller" should {

      "contact details function" should {

        "throw an exception" in new Fixture {

          val fakeRequest =
            FakeRequest("GET", s"/sandbox/addresses/")

          val result =
            intercept[Exception] {
              await(sandboxAddressesController.addresses()(fakeRequest))
            }
          assert(result.getMessage == "NOT_IMPLEMENTED")
        }
      }
    }
  }
}
