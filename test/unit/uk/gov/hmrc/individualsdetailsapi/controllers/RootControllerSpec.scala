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

package unit.uk.gov.hmrc.individualsdetailsapi.controllers

import akka.stream.Materializer
import org.mockito.ArgumentMatchers.{any, refEq, eq => eqTo}
import org.mockito.Mockito
import org.mockito.Mockito.{times, verify, verifyNoInteractions, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsJson, _}
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.TooManyRequestException
import uk.gov.hmrc.individualsdetailsapi.audit.AuditHelper
import uk.gov.hmrc.individualsdetailsapi.config.InternalEndpointConfig
import uk.gov.hmrc.individualsdetailsapi.controllers.RootController
import uk.gov.hmrc.individualsdetailsapi.domain.{MatchNotFoundException, MatchedCitizen}
import uk.gov.hmrc.individualsdetailsapi.services.{DetailsService, ScopesHelper, ScopesService}
import unit.uk.gov.hmrc.individualsdetailsapi.utils.SpecBase

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class RootControllerSpec extends SpecBase with MockitoSugar {

  val matchId: UUID = UUID.fromString("2b2e7e84-102f-4338-93f9-1950b35d822b");
  val sampleCorrelationId = "188e9400-b636-4a3b-80ba-230a8c72b92a"
  val validCorrelationHeader = ("CorrelationId", sampleCorrelationId)

  implicit lazy val materializer: Materializer = fakeApplication.materializer
  implicit lazy val ec: ExecutionContext =
    fakeApplication.injector.instanceOf[ExecutionContext]

  trait Fixture extends ScopesConfigHelper {

    implicit lazy val ec = fakeApplication.injector.instanceOf[ExecutionContext]

    lazy val scopeService: ScopesService = mock[ScopesService]
    lazy val scopeHelper: ScopesHelper = new ScopesHelper(scopeService)
    val mockDetailsService = mock[DetailsService]
    val mockAuthConnector: AuthConnector = mock[AuthConnector]
    val mockAuditHelper: AuditHelper = mock[AuditHelper]

    when(
      mockAuthConnector.authorise(
        eqTo(Enrolment("test-scope")),
        refEq(Retrievals.allEnrolments))(any(), any()))
      .thenReturn(Future.successful(Enrolments(Set(Enrolment("test-scope")))))

    val scopes: Iterable[String] =
      Iterable("test-scope")

    val rootController =
      new RootController(
        mockAuthConnector,
        cc,
        scopeService,
        scopeHelper,
        mockDetailsService,
        mockAuditHelper
      )

    when(scopeService.getAllScopes).thenReturn(scopes.toList)
    when(scopeService.getInternalEndpoints(any())).thenReturn(
      Seq(
        InternalEndpointConfig(
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

          Mockito.reset(rootController.auditHelper)

          val fakeRequest = FakeRequest("GET", s"/")
            .withHeaders(validCorrelationHeader)

          when(mockDetailsService.resolve(eqTo(matchId))(any()))
            .thenReturn(Future.successful(
              MatchedCitizen(matchId, nino = Nino("AB123456C"))))

          val result = rootController.root(matchId)(fakeRequest)

          status(result) shouldBe OK

          verify(rootController.auditHelper, times(1)).
            auditApiResponse(any(), any(), any(), any(), any())(any())

        }

        "return 404 (not found) for an invalid matchId" in new Fixture {

          val fakeRequest = FakeRequest("GET", s"/")
            .withHeaders(validCorrelationHeader)

          when(mockDetailsService.resolve(eqTo(matchId))(any()))
            .thenReturn(Future.failed(new MatchNotFoundException))

          val result = rootController.root(matchId)(fakeRequest)

          status(result) shouldBe NOT_FOUND

          contentAsJson(result) shouldBe Json.obj(
            "code" -> "NOT_FOUND",
            "message" -> "The resource can not be found"
          )
          verify(rootController.auditHelper, times(1))
            .auditApiFailure(any(), any(), any(), any(), any())(any())
        }

        "return 401 when the bearer token does not have valid enrolment" in new Fixture {

          when(mockAuthConnector.authorise(any(), any())(any(), any()))
            .thenReturn(Future.failed(InsufficientEnrolments()))

          val fakeRequest = FakeRequest("GET", s"/")
            .withHeaders(validCorrelationHeader)

          val result = rootController.root(matchId)(fakeRequest)

          status(result) shouldBe UNAUTHORIZED
          verifyNoInteractions(mockDetailsService)
          verify(rootController.auditHelper, times(1))
            .auditApiFailure(any(), any(), any(), any(), any())(any())
        }

        "return error when no scopes are supplied" in new Fixture {
          when(scopeService.getAllScopes).thenReturn(List())

          val fakeRequest = FakeRequest("GET", s"/")
            .withHeaders(validCorrelationHeader)

          val result =
            intercept[Exception] {
              await(rootController.root(matchId)(fakeRequest))
            }
          assert(result.getMessage == "No scopes defined")
        }

        "return 401 when bearer token is expired" in new Fixture {

          when(mockAuthConnector.authorise(any(), any())(any(), any()))
            .thenReturn(Future.failed(BearerTokenExpired()))

          val fakeRequest = FakeRequest("GET", s"/").withHeaders(validCorrelationHeader)

          val result = rootController.root(matchId)(fakeRequest)

          status(result) shouldBe UNAUTHORIZED
          verifyNoInteractions(mockDetailsService)
          verify(rootController.auditHelper, times(1))
            .auditApiFailure(any(), any(), any(), any(), any())(any())
        }

        "return 429 when too many requests received" in new Fixture {

          when(mockAuthConnector.authorise(any(), any())(any(), any()))
            .thenReturn(Future.failed(new TooManyRequestException("Too many")))

          val fakeRequest = FakeRequest("GET", s"/").withHeaders(validCorrelationHeader)

          val result = rootController.root(matchId)(fakeRequest)

          status(result) shouldBe TOO_MANY_REQUESTS
          verifyNoInteractions(mockDetailsService)
          verify(rootController.auditHelper, times(1))
            .auditApiFailure(any(), any(), any(), any(), any())(any())
        }

        "return 500 when an unspecified Exception is thrown" in new Fixture {

          when(mockAuthConnector.authorise(any(), any())(any(), any()))
            .thenReturn(Future.failed(new Exception()))

          val fakeRequest = FakeRequest("GET", s"/").withHeaders(validCorrelationHeader)

          val result = rootController.root(matchId)(fakeRequest)

          status(result) shouldBe INTERNAL_SERVER_ERROR
          verifyNoInteractions(mockDetailsService)
          verify(rootController.auditHelper, times(1))
            .auditApiFailure(any(), any(), any(), any(), any())(any())
        }
      }
    }
  }
}
