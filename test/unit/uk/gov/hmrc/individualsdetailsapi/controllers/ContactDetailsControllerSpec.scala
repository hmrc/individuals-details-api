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

import org.apache.pekko.stream.Materializer
import org.mockito.ArgumentMatchers.{any, eq => eqTo, refEq}
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.{AuthConnector, Enrolment, Enrolments, InsufficientEnrolments}
import uk.gov.hmrc.individualsdetailsapi.audit.AuditHelper
import uk.gov.hmrc.individualsdetailsapi.controllers.ContactDetailsController
import uk.gov.hmrc.individualsdetailsapi.domain.{ContactDetails, MatchNotFoundException}
import uk.gov.hmrc.individualsdetailsapi.services.{DetailsService, ScopesService}
import unit.uk.gov.hmrc.individualsdetailsapi.utils.SpecBase

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class ContactDetailsControllerSpec extends SpecBase with MockitoSugar {

  val matchId: UUID = UUID.fromString("2b2e7e84-102f-4338-93f9-1950b35d822b");
  val sampleCorrelationId = "188e9400-b636-4a3b-80ba-230a8c72b92a"
  val validCorrelationHeader = ("CorrelationId", sampleCorrelationId)

  implicit lazy val materializer: Materializer = fakeApplication.materializer
  implicit lazy val ec: ExecutionContext = fakeApplication.injector.instanceOf[ExecutionContext]

  trait Fixture extends ScopesConfigHelper {

    implicit lazy val ec = fakeApplication.injector.instanceOf[ExecutionContext]

    lazy val scopeService: ScopesService = mock[ScopesService]
    val mockDetailsService = mock[DetailsService]
    val mockAuthConnector: AuthConnector = mock[AuthConnector]
    val mockAuditHelper: AuditHelper = mock[AuditHelper]

    when(mockAuthConnector.authorise(eqTo(Enrolment("test-scope")), refEq(Retrievals.allEnrolments))(any(), any()))
      .thenReturn(Future.successful(Enrolments(Set(Enrolment("test-scope")))))

    val scopes: Iterable[String] =
      Iterable("test-scope")

    val contactDetailsController =
      new ContactDetailsController(
        mockAuthConnector,
        cc,
        scopeService,
        mockDetailsService,
        mockAuditHelper
      )

    when(scopeService.getEndPointScopes(any())).thenReturn(scopes)
  }

  "ContactDetailsController" when {

    "calling contactDetails" when {

      "using the live controller" should {

        "return contact-details when successful" in new Fixture {

          val fakeRequest = FakeRequest("GET", s"/contact-details/")
            .withHeaders(validCorrelationHeader)

          when(
            mockDetailsService
              .getContactDetails(eqTo(matchId), eqTo("contact-details"), eqTo(Set("test-scope")))(any(), any(), any())
          )
            .thenReturn(
              Future.successful(
                Some(
                  ContactDetails(
                    daytimeTelephones = List("0123 456789"),
                    eveningTelephones = List("0123 456789"),
                    mobileTelephones = List("0123 456789")
                  )
                )
              )
            )

          val result = contactDetailsController.contactDetails(matchId)(fakeRequest)

          status(result) shouldBe OK
          verify(contactDetailsController.auditHelper, times(1))
            .auditContactDetailsApiResponse(any(), any(), any(), any(), any(), any())(any())
        }

        "return 404 (not found) for an invalid matchId" in new Fixture {

          val fakeRequest = FakeRequest("GET", s"/contact-details/")
            .withHeaders(validCorrelationHeader)

          when(
            mockDetailsService
              .getContactDetails(eqTo(matchId), eqTo("contact-details"), eqTo(Set("test-scope")))(any(), any(), any())
          )
            .thenReturn(Future.failed(new MatchNotFoundException))

          val result = contactDetailsController.contactDetails(matchId)(fakeRequest)

          status(result) shouldBe NOT_FOUND

          contentAsJson(result) shouldBe Json.obj(
            "code"    -> "NOT_FOUND",
            "message" -> "The resource can not be found"
          )
          verify(contactDetailsController.auditHelper, times(1))
            .auditApiFailure(any(), any(), any(), any(), any())(any())
        }

        "return 401 when the bearer token does not have valid enrolment" in new Fixture {

          when(mockAuthConnector.authorise(any(), any())(any(), any()))
            .thenReturn(Future.failed(InsufficientEnrolments()))

          val fakeRequest = FakeRequest("GET", s"/contact-details/")
            .withHeaders(validCorrelationHeader)

          val result = contactDetailsController.contactDetails(matchId)(fakeRequest)

          status(result) shouldBe UNAUTHORIZED
          verifyNoInteractions(mockDetailsService)
          verify(contactDetailsController.auditHelper, times(1))
            .auditApiFailure(any(), any(), any(), any(), any())(any())
        }

        "return error when no scopes are supplied" in new Fixture {
          when(scopeService.getEndPointScopes(any())).thenReturn(None)

          val fakeRequest = FakeRequest("GET", s"/contact-details/")
            .withHeaders(validCorrelationHeader)

          val result =
            intercept[Exception] {
              await(contactDetailsController.contactDetails(matchId)(fakeRequest))
            }
          assert(result.getMessage == "No scopes defined")
        }

        "return Bad Request with correct error message when CorrelationId header is missing" in new Fixture {

          val fakeRequest = FakeRequest("GET", s"/contact-details/")

          when(
            mockDetailsService
              .getContactDetails(eqTo(matchId), eqTo("contact-details"), eqTo(List("test-scope")))(any(), any(), any())
          )
            .thenReturn(
              Future.successful(
                Some(
                  ContactDetails(
                    daytimeTelephones = List("0123 456789"),
                    eveningTelephones = List("0123 456789"),
                    mobileTelephones = List("0123 456789")
                  )
                )
              )
            )

          val result = contactDetailsController.contactDetails(matchId)(fakeRequest)

          status(result) shouldBe BAD_REQUEST
          contentAsJson(result) shouldBe Json.parse(
            """{
              |    "code": "INVALID_REQUEST",
              |    "message": "CorrelationId is required"
              |}""".stripMargin
          )
          verify(contactDetailsController.auditHelper, times(1))
            .auditApiFailure(any(), any(), any(), any(), any())(any())
        }

        "throws an Exception when CorrelationId header s missing" in new Fixture {

          val fakeRequest = FakeRequest("GET", s"/contact-details/")
            .withHeaders("CorrelationId" -> "invalidId")

          when(
            mockDetailsService
              .getContactDetails(eqTo(matchId), eqTo("contact-details"), eqTo(List("test-scope")))(any(), any(), any())
          )
            .thenReturn(
              Future.successful(
                Some(
                  ContactDetails(
                    daytimeTelephones = List("0123 456789"),
                    eveningTelephones = List("0123 456789"),
                    mobileTelephones = List("0123 456789")
                  )
                )
              )
            )

          val result = contactDetailsController.contactDetails(matchId)(fakeRequest)

          status(result) shouldBe BAD_REQUEST
          contentAsJson(result) shouldBe Json.parse(
            """{
              |    "code": "INVALID_REQUEST",
              |    "message": "Malformed CorrelationId"
              |}""".stripMargin
          )
          verify(contactDetailsController.auditHelper, times(1))
            .auditApiFailure(any(), any(), any(), any(), any())(any())
        }
      }
    }
  }
}
