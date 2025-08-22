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

import org.mockito.ArgumentMatchers.{any, eq as eqTo, refEq}
import org.mockito.Mockito.{times, verify, verifyNoInteractions, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsJson, *}
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.{AuthConnector, Enrolment, Enrolments, InsufficientEnrolments}
import uk.gov.hmrc.individualsdetailsapi.audit.AuditHelper
import uk.gov.hmrc.individualsdetailsapi.controllers.AddressesController
import uk.gov.hmrc.individualsdetailsapi.domain.{Address, MatchNotFoundException, Residence}
import uk.gov.hmrc.individualsdetailsapi.services.{DetailsService, ScopesService}
import unit.uk.gov.hmrc.individualsdetailsapi.utils.SpecBase

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class AddressesControllerSpec extends SpecBase with MockitoSugar {

  val matchId: UUID = UUID.fromString("2b2e7e84-102f-4338-93f9-1950b35d822b")
  val sampleCorrelationId = "188e9400-b636-4a3b-80ba-230a8c72b92a"
  val validCorrelationHeader: (String, String) = ("CorrelationId", sampleCorrelationId)

  trait Fixture extends ScopesConfigHelper {

    implicit lazy val ec: ExecutionContext = fakeApplication().injector.instanceOf[ExecutionContext]

    lazy val scopeService: ScopesService = mock[ScopesService]
    val mockDetailsService: DetailsService = mock[DetailsService]
    val mockAuthConnector: AuthConnector = mock[AuthConnector]
    val mockAuditHelper: AuditHelper = mock[AuditHelper]

    when(
      mockAuthConnector.authorise(eqTo(Enrolment("test-scope")), refEq(Retrievals.allEnrolments))(using any(), any())
    )
      .thenReturn(Future.successful(Enrolments(Set(Enrolment("test-scope")))))

    val scopes: Iterable[String] =
      Iterable("test-scope")

    val addressesController =
      new AddressesController(
        mockAuthConnector,
        cc,
        scopeService,
        mockDetailsService,
        mockAuditHelper
      )

    when(scopeService.getEndPointScopes(any())).thenReturn(scopes)

    val residences: Seq[Residence] = Seq(
      Residence(
        residenceType = Option("NOMINATED"),
        address = Option(
          Address(
            line1 = Option("24 Trinity Street"),
            line2 = Option("Dawley Bank"),
            line3 = Option("Telford"),
            line4 = Option("Shropshire"),
            line5 = Option("UK"),
            postcode = Option("TF3 4ER")
          )
        ),
        inUse = Option(true)
      ),
      Residence(
        residenceType = Option("BASE"),
        address = Option(
          Address(
            line1 = Option("La Petite Maison"),
            line2 = Option("Rue de Bastille"),
            line3 = Option("Vieux Ville"),
            line4 = Option("Dordogne"),
            line5 = Option("France"),
            postcode = None
          )
        ),
        inUse = Option(false)
      )
    )
  }

  "AddressesController" when {

    "calling addresses" when {

      "using the live controller" should {

        "return addresses when successful" in new Fixture {

          val fakeRequest = FakeRequest("GET", s"/addresses/")
            .withHeaders(validCorrelationHeader)

          when(
            mockDetailsService
              .getResidences(eqTo(matchId), eqTo("addresses"), eqTo(Set("test-scope")))(using any(), any(), any())
          )
            .thenReturn(Future.successful(residences))

          val result: Future[Result] = addressesController.addresses(matchId)(fakeRequest)

          status(result) shouldBe OK
          verify(addressesController.auditHelper, times(1))
            .auditResidencesApiResponse(any(), any(), any(), any(), any(), any())(using any())
        }

        "return 404 (not found) for an invalid matchId" in new Fixture {

          val fakeRequest = FakeRequest("GET", s"/addresses/")
            .withHeaders(validCorrelationHeader)

          when(
            mockDetailsService
              .getResidences(eqTo(matchId), eqTo("addresses"), eqTo(Set("test-scope")))(using any(), any(), any())
          )
            .thenReturn(Future.failed(new MatchNotFoundException))

          val result: Future[Result] = addressesController.addresses(matchId)(fakeRequest)

          status(result) shouldBe NOT_FOUND

          contentAsJson(result) shouldBe Json.obj(
            "code"    -> "NOT_FOUND",
            "message" -> "The resource can not be found"
          )

          verify(addressesController.auditHelper, times(1))
            .auditApiFailure(any(), any(), any(), any(), any())(using any())
        }

        "return 401 when the bearer token does not have valid enrolment" in new Fixture {

          when(mockAuthConnector.authorise(any(), any())(using any(), any()))
            .thenReturn(Future.failed(InsufficientEnrolments()))

          val fakeRequest = FakeRequest("GET", s"/addresses/")
            .withHeaders(validCorrelationHeader)

          val result: Future[Result] = addressesController.addresses(matchId)(fakeRequest)

          status(result) shouldBe UNAUTHORIZED
          verifyNoInteractions(mockDetailsService)
        }

        "return error when no scopes are supplied" in new Fixture {
          when(scopeService.getEndPointScopes(any())).thenReturn(List.empty)

          val fakeRequest = FakeRequest("GET", s"/addresses/")
            .withHeaders(validCorrelationHeader)

          val result: Exception =
            intercept[Exception] {
              await(addressesController.addresses(matchId)(fakeRequest))
            }
          assert(result.getMessage == "No scopes defined")
        }

        "return bad request with correct error message when missing CorrelationId" in new Fixture {
          val fakeRequest = FakeRequest("GET", s"/addresses/")

          when(
            mockDetailsService
              .getResidences(eqTo(matchId), eqTo("addresses"), eqTo(List("test-scope")))(using any(), any(), any())
          )
            .thenReturn(Future.successful(residences))

          val result: Future[Result] = addressesController.addresses(matchId)(fakeRequest)

          status(result) shouldBe BAD_REQUEST
          contentAsJson(result) shouldBe Json.parse(
            """{
              |    "code": "INVALID_REQUEST",
              |    "message": "CorrelationId is required"
              |}""".stripMargin
          )
          verify(addressesController.auditHelper, times(1))
            .auditApiFailure(any(), any(), any(), any(), any())(using any())
        }

        "return bad request with correct error message when CorrelationId is malformed" in new Fixture {
          val fakeRequest = FakeRequest("GET", s"/addresses/")
            .withHeaders("CorrelationId" -> "invalidId")

          when(
            mockDetailsService
              .getResidences(eqTo(matchId), eqTo("addresses"), eqTo(List("test-scope")))(using any(), any(), any())
          )
            .thenReturn(Future.successful(residences))

          val result: Future[Result] = addressesController.addresses(matchId)(fakeRequest)

          status(result) shouldBe BAD_REQUEST
          contentAsJson(result) shouldBe Json.parse(
            """{
              |    "code": "INVALID_REQUEST",
              |    "message": "Malformed CorrelationId"
              |}""".stripMargin
          )
          verify(addressesController.auditHelper, times(1))
            .auditApiFailure(any(), any(), any(), any(), any())(using any())
        }

      }
    }
  }
}
