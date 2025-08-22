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

package unit.uk.gov.hmrc.individualsdetailsapi.audit

import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{times, verify}
import org.mockito.{ArgumentCaptor, Mockito}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.individualsdetailsapi.audit.AuditHelper
import uk.gov.hmrc.individualsdetailsapi.audit.models.*
import uk.gov.hmrc.individualsdetailsapi.domain.integrationframework.IfDetailsResponse
import uk.gov.hmrc.individualsdetailsapi.domain.{ContactDetails, Residence}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

class AuditHelperSpec extends AsyncWordSpec with Matchers with MockitoSugar {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val nino = "CS700100A"
  val correlationId = "test"
  val scopes = "test"
  val matchId = "80a6bb14-d888-436e-a541-4000674c60aa"
  val applicationId = "80a6bb14-d888-436e-a541-4000674c60bb"
  val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withHeaders("X-Application-Id" -> applicationId)
  val contactDetailsResponse: ContactDetails =
    ContactDetails(List.empty, List.empty, List.empty)
  val residencesResponse = Seq.empty[Residence]
  val ifResponse: IfDetailsResponse = IfDetailsResponse(None, None)
  val ifUrl =
    s"host/individuals/details/contact/nino/$nino?startDate=2019-01-01&endDate=2020-01-01&fields=some(vals(val1),val2)"
  val endpoint = "/test"

  val auditConnector: AuditConnector = mock[AuditConnector]

  val auditHelper = new AuditHelper(auditConnector)

  "Auth helper" should {

    "auditAuthScopes" in {

      Mockito.reset(auditConnector)

      val captor = ArgumentCaptor.forClass(classOf[ScopesAuditEventModel])

      auditHelper.auditAuthScopes(matchId, scopes, request)

      verify(auditConnector, times(1)).sendExplicitAudit[ScopesAuditEventModel](
        eqTo("AuthScopesAuditEvent"),
        captor.capture()
      )(using any(), any(), any())

      val capturedEvent = captor.getValue.asInstanceOf[ScopesAuditEventModel]
      capturedEvent.apiVersion shouldEqual "1.0"
      capturedEvent.matchId shouldEqual matchId
      capturedEvent.scopes shouldBe scopes
      capturedEvent.applicationId shouldBe applicationId

    }

    "auditResidencesApiResponse" in {

      Mockito.reset(auditConnector)

      val captor =
        ArgumentCaptor.forClass(classOf[ResidencesApiResponseEventModel])

      auditHelper.auditResidencesApiResponse(correlationId, matchId, scopes, request, endpoint, residencesResponse)

      verify(auditConnector, times(1))
        .sendExplicitAudit[ResidencesApiResponseEventModel](
          eqTo("ApiResponseEvent"),
          captor.capture()
        )(using any(), any(), any())

      val capturedEvent =
        captor.getValue.asInstanceOf[ResidencesApiResponseEventModel]
      capturedEvent.matchId shouldEqual matchId
      capturedEvent.correlationId shouldEqual Some(correlationId)
      capturedEvent.scopes shouldBe scopes
      capturedEvent.applicationId shouldBe applicationId
      capturedEvent.returnLinks shouldBe endpoint
      capturedEvent.residences shouldBe residencesResponse
      capturedEvent.apiVersion shouldBe "1.0"

    }

    "auditContactDetailsApiResponse" in {

      Mockito.reset(auditConnector)

      val captor =
        ArgumentCaptor.forClass(classOf[ContactDetailsApiResponseEventModel])

      auditHelper
        .auditContactDetailsApiResponse(correlationId, matchId, scopes, request, endpoint, Some(contactDetailsResponse))

      verify(auditConnector, times(1))
        .sendExplicitAudit[ContactDetailsApiResponseEventModel](
          eqTo("ApiResponseEvent"),
          captor.capture()
        )(using any(), any(), any())

      val capturedEvent =
        captor.getValue.asInstanceOf[ContactDetailsApiResponseEventModel]
      capturedEvent.matchId shouldEqual matchId
      capturedEvent.correlationId shouldEqual Some(correlationId)
      capturedEvent.scopes shouldBe scopes
      capturedEvent.applicationId shouldBe applicationId
      capturedEvent.returnLinks shouldBe endpoint
      capturedEvent.contactDetails shouldBe Some(contactDetailsResponse)
      capturedEvent.apiVersion shouldBe "1.0"

    }

    "auditApiResponse" in {
      Mockito.reset(auditConnector)
      val captor = ArgumentCaptor.forClass(classOf[ApiResponseEventModel])
      auditHelper.auditApiResponse(correlationId, matchId, scopes, request, endpoint)
      verify(auditConnector, times(1)).sendExplicitAudit[ApiResponseEventModel](
        eqTo("ApiResponseEvent"),
        captor.capture()
      )(using any(), any(), any())
      val capturedEvent = captor.getValue.asInstanceOf[ApiResponseEventModel]
      capturedEvent.matchId shouldEqual matchId
      capturedEvent.correlationId shouldEqual Some(correlationId)
      capturedEvent.scopes shouldBe scopes
      capturedEvent.applicationId shouldBe applicationId
      capturedEvent.returnLinks shouldBe endpoint
    }

    "auditApiFailure" in {

      Mockito.reset(auditConnector)

      val msg = "Something went wrong"

      val captor =
        ArgumentCaptor.forClass(classOf[ApiFailureResponseEventModel])

      auditHelper.auditApiFailure(Some(correlationId), matchId, request, "/test", msg)

      verify(auditConnector, times(1))
        .sendExplicitAudit[ApiFailureResponseEventModel](
          eqTo("ApiFailureEvent"),
          captor.capture()
        )(using any(), any(), any())

      val capturedEvent =
        captor.getValue.asInstanceOf[ApiFailureResponseEventModel]
      capturedEvent.matchId shouldEqual matchId
      capturedEvent.correlationId shouldEqual Some(correlationId)
      capturedEvent.applicationId shouldBe applicationId
      capturedEvent.requestUrl shouldEqual endpoint
      capturedEvent.response shouldEqual msg

    }

    "auditIfApiResponse" in {

      Mockito.reset(auditConnector)

      val captor = ArgumentCaptor.forClass(classOf[IfApiResponseEventModel])

      auditHelper.auditIfApiResponse(correlationId, matchId, request, ifUrl, ifResponse)

      verify(auditConnector, times(1))
        .sendExplicitAudit[IfApiResponseEventModel](
          eqTo("IntegrationFrameworkApiResponseEvent"),
          captor.capture()
        )(using any(), any(), any())

      val capturedEvent = captor.getValue.asInstanceOf[IfApiResponseEventModel]
      capturedEvent.matchId shouldEqual matchId
      capturedEvent.correlationId shouldEqual correlationId
      capturedEvent.applicationId shouldBe applicationId
      capturedEvent.requestUrl shouldBe ifUrl
      capturedEvent.integrationFrameworkDetails shouldBe ifResponse

    }

    "auditIfApiFailure" in {

      Mockito.reset(auditConnector)

      val msg = "Something went wrong"

      val captor =
        ArgumentCaptor.forClass(classOf[ApiFailureResponseEventModel])

      auditHelper.auditIfApiFailure(correlationId, matchId, request, ifUrl, msg)

      verify(auditConnector, times(1))
        .sendExplicitAudit[ApiFailureResponseEventModel](
          eqTo("IntegrationFrameworkApiFailureEvent"),
          captor.capture()
        )(using any(), any(), any())

      val capturedEvent =
        captor.getValue.asInstanceOf[ApiFailureResponseEventModel]
      capturedEvent.matchId shouldEqual matchId
      capturedEvent.correlationId shouldEqual Some(correlationId)
      capturedEvent.applicationId shouldBe applicationId
      capturedEvent.requestUrl shouldEqual ifUrl
      capturedEvent.response shouldEqual msg

    }

  }

}
