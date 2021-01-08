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

package unit.uk.gov.hmrc.individualsdetailsapi.controllers

import java.util.UUID

import akka.stream.Materializer
import org.mockito.ArgumentMatchers.{any, refEq, eq => eqTo}
import org.mockito.Mockito.{verifyNoInteractions, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsJson, _}
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.{
  AuthConnector,
  Enrolment,
  Enrolments,
  InsufficientEnrolments
}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.individualsdetailsapi.config.EndpointConfig
import uk.gov.hmrc.individualsdetailsapi.controllers.{
  LiveRootController,
  SandboxRootController
}
import uk.gov.hmrc.individualsdetailsapi.domain.{
  MatchNotFoundException,
  MatchedCitizen
}
import uk.gov.hmrc.individualsdetailsapi.service.{ScopesHelper, ScopesService}
import uk.gov.hmrc.individualsdetailsapi.services.{
  LiveDetailsService,
  SandboxDetailsService
}
import unit.uk.gov.hmrc.individualsdetailsapi.utils.SpecBase

import scala.concurrent.{ExecutionContext, Future}

class RootControllerSpec extends SpecBase with MockitoSugar {

  val matchId: UUID = UUID.fromString("2b2e7e84-102f-4338-93f9-1950b35d822b");

  implicit lazy val materializer: Materializer = fakeApplication.materializer
  implicit lazy val ec: ExecutionContext =
    fakeApplication.injector.instanceOf[ExecutionContext]

  trait Fixture extends ScopesConfigHelper {

    implicit lazy val ec = fakeApplication.injector.instanceOf[ExecutionContext]

    lazy val scopeService: ScopesService = mock[ScopesService]
    lazy val scopeHelper: ScopesHelper = new ScopesHelper(scopeService)
    val mockLiveDetailsService = mock[LiveDetailsService]
    val mockSandboxDetailsService = mock[SandboxDetailsService]
    val mockAuthConnector: AuthConnector = mock[AuthConnector]

    when(
      mockAuthConnector.authorise(
        eqTo(Enrolment("test-scope")),
        refEq(Retrievals.allEnrolments))(any(), any()))
      .thenReturn(Future.successful(Enrolments(Set(Enrolment("test-scope")))))

    val scopes: Iterable[String] =
      Iterable("test-scope")

    val liveRootController =
      new LiveRootController(
        mockAuthConnector,
        cc,
        scopeService,
        scopeHelper,
        mockLiveDetailsService
      )

    val sandboxRootController =
      new SandboxRootController(
        mockAuthConnector,
        cc,
        scopeService,
        scopeHelper,
        mockSandboxDetailsService
      )

    when(scopeService.getAllScopes).thenReturn(scopes.toList)
    when(scopeService.getEndpoints(any())).thenReturn(
      Seq(
        EndpointConfig(
          name = "endpointName",
          link = "endpoint/link",
          title = "endpointTitle",
          fields = Map("fieldId" -> "data/path"),
          filters = Map()
        )
      )
    )
  }

  "RootController" when {

    "calling root" when {

      "using the live controller" should {

        "return response when successful" in new Fixture {

          val fakeRequest = FakeRequest("GET", s"/")

          when(mockLiveDetailsService.resolve(eqTo(matchId))(any()))
            .thenReturn(Future.successful(
              MatchedCitizen(matchId, nino = Nino("AB123456C"))))

          val result = liveRootController.root(matchId)(fakeRequest)

          status(result) shouldBe OK
        }

        "return 404 (not found) for an invalid matchId" in new Fixture {

          val fakeRequest = FakeRequest("GET", s"/")

          when(mockLiveDetailsService.resolve(eqTo(matchId))(any()))
            .thenReturn(Future.failed(new MatchNotFoundException))

          val result = liveRootController.root(matchId)(fakeRequest)

          status(result) shouldBe NOT_FOUND

          contentAsJson(result) shouldBe Json.obj(
            "code" -> "NOT_FOUND",
            "message" -> "The resource can not be found"
          )
        }

        "return 401 when the bearer token does not have valid enrolment" in new Fixture {

          when(mockAuthConnector.authorise(any(), any())(any(), any()))
            .thenReturn(Future.failed(InsufficientEnrolments()))

          val fakeRequest = FakeRequest("GET", s"/")

          val result = liveRootController.root(matchId)(fakeRequest)

          status(result) shouldBe UNAUTHORIZED
          verifyNoInteractions(mockLiveDetailsService)
        }

        "return error when no scopes are supplied" in new Fixture {
          when(scopeService.getAllScopes).thenReturn(List())

          val fakeRequest =
            FakeRequest("GET", s"/")

          val result =
            intercept[Exception] {
              await(liveRootController.root(matchId)(fakeRequest))
            }
          assert(result.getMessage == "No scopes defined")
        }
      }

      "using the sandbox controller" should {

        "return response when successful" in new Fixture {

          val fakeRequest = FakeRequest("GET", s"/")

          when(mockSandboxDetailsService.resolve(eqTo(matchId))(any()))
            .thenReturn(Future.successful(
              MatchedCitizen(matchId, nino = Nino("AB123456C"))))

          val result = sandboxRootController.root(matchId)(fakeRequest)

          status(result) shouldBe OK
        }

        "return 404 (not found) for an invalid matchId" in new Fixture {

          val fakeRequest = FakeRequest("GET", s"/")

          when(mockSandboxDetailsService.resolve(eqTo(matchId))(any()))
            .thenReturn(Future.failed(new MatchNotFoundException))

          val result = sandboxRootController.root(matchId)(fakeRequest)

          status(result) shouldBe NOT_FOUND

          contentAsJson(result) shouldBe Json.obj(
            "code" -> "NOT_FOUND",
            "message" -> "The resource can not be found"
          )
        }

        "return error when no scopes are supplied" in new Fixture {

          when(scopeService.getAllScopes).thenReturn(List())

          val fakeRequest =
            FakeRequest("GET", s"/")

          val result =
            intercept[Exception] {
              await(sandboxRootController.root(matchId)(fakeRequest))
            }
          assert(result.getMessage == "No scopes defined")
        }
      }
    }
  }
}
