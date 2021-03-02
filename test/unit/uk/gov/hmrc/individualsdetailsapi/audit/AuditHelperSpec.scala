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

package unit.uk.gov.hmrc.individualsdetailsapi.audit

import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{times, verify}
import org.mockito.{ArgumentCaptor, Mockito}
import org.scalatest.{AsyncWordSpec, Matchers}
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.FakeRequest
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.individualsdetailsapi.audit.AuditHelper
import uk.gov.hmrc.individualsdetailsapi.audit.models._
import uk.gov.hmrc.individualsdetailsapi.domain.integrationframework.IfDetailsResponse
import uk.gov.hmrc.individualsdetailsapi.domain.{ContactDetails, Residence}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

class AuditHelperSpec extends AsyncWordSpec with Matchers with MockitoSugar {

  implicit val hc = HeaderCarrier()

  val nino = "CS700100A"
  val correlationId = "test"
  val scopes = "test"
  val matchId = "80a6bb14-d888-436e-a541-4000674c60aa"
  val request = FakeRequest()
  val contactDetailsResponse = ContactDetails(List.empty, List.empty, List.empty)
  val residencesResponse = Seq.empty[Residence]
  val ifResponse = IfDetailsResponse(None,None)
  val ifUrl =
    s"host/individuals/details/contact/nino/$nino?startDate=2019-01-01&endDate=2020-01-01&fields=some(vals(val1),val2)"
  val endpoint = "/test"

  val auditConnector = mock[AuditConnector]

  val auditHelper = new AuditHelper(auditConnector)

  "Auth helper" should {

    "auditAuthScopes" in {

      Mockito.reset(auditConnector)

      val captor = ArgumentCaptor.forClass(classOf[ScopesAuditEventModel])

      auditHelper.auditAuthScopes(matchId, scopes, request)

      verify(auditConnector, times(1)).sendExplicitAudit(eqTo("AuthScopesAuditEvent"),
        captor.capture())(any(), any(), any())

      val capturedEvent = captor.getValue.asInstanceOf[ScopesAuditEventModel]
      capturedEvent.asInstanceOf[ScopesAuditEventModel].apiVersion shouldEqual "1.0"
      capturedEvent.asInstanceOf[ScopesAuditEventModel].matchId shouldEqual matchId
      capturedEvent.asInstanceOf[ScopesAuditEventModel].scopes shouldBe scopes

    }

    "auditResidencesApiResponse" in {

      Mockito.reset(auditConnector)

      val captor = ArgumentCaptor.forClass(classOf[ResidencesApiResponseEventModel])

      auditHelper.auditResidencesApiResponse(correlationId, matchId, scopes, request, endpoint, residencesResponse)

      verify(auditConnector, times(1)).sendExplicitAudit(eqTo("ApiResponseEvent"),
        captor.capture())(any(), any(), any())

      val capturedEvent = captor.getValue.asInstanceOf[ResidencesApiResponseEventModel]
      capturedEvent.matchId shouldEqual matchId
      capturedEvent.correlationId shouldEqual Some(correlationId)
      capturedEvent.scopes shouldBe scopes
      capturedEvent.returnLinks shouldBe endpoint
      capturedEvent.residences shouldBe residencesResponse
      capturedEvent.apiVersion shouldBe "1.0"

    }

    "auditContactDetailsApiResponse" in {

      Mockito.reset(auditConnector)

      val captor = ArgumentCaptor.forClass(classOf[ContactDetailsApiResponseEventModel])

      auditHelper.auditContactDetailsApiResponse(correlationId, matchId, scopes, request, endpoint, Some(contactDetailsResponse))

      verify(auditConnector, times(1)).sendExplicitAudit(eqTo("ApiResponseEvent"),
        captor.capture())(any(), any(), any())

      val capturedEvent = captor.getValue.asInstanceOf[ContactDetailsApiResponseEventModel]
      capturedEvent.matchId shouldEqual matchId
      capturedEvent.correlationId shouldEqual Some(correlationId)
      capturedEvent.scopes shouldBe scopes
      capturedEvent.returnLinks shouldBe endpoint
      capturedEvent.contactDetails shouldBe Some(contactDetailsResponse)
      capturedEvent.apiVersion shouldBe "1.0"

    }

    "auditApiFailure" in {

      Mockito.reset(auditConnector)

      val msg = "Something went wrong"

      val captor = ArgumentCaptor.forClass(classOf[ApiFailureResponseEventModel])

      auditHelper.auditApiFailure(Some(correlationId), matchId, request, "/test", msg)

      verify(auditConnector, times(1)).sendExplicitAudit(eqTo("ApiFailureEvent"),
        captor.capture())(any(), any(), any())

      val capturedEvent = captor.getValue.asInstanceOf[ApiFailureResponseEventModel]
      capturedEvent.matchId shouldEqual matchId
      capturedEvent.correlationId shouldEqual Some(correlationId)
      capturedEvent.requestUrl shouldEqual endpoint
      capturedEvent.response shouldEqual msg

    }

    "auditIfApiResponse" in {

      Mockito.reset(auditConnector)

      val captor = ArgumentCaptor.forClass(classOf[IfApiResponseEventModel])

      auditHelper.auditIfApiResponse(correlationId, matchId, request, ifUrl, ifResponse)

      verify(auditConnector, times(1)).sendExplicitAudit(eqTo("IfApiResponseEvent"),
        captor.capture())(any(), any(), any())

      val capturedEvent = captor.getValue.asInstanceOf[IfApiResponseEventModel]
      capturedEvent.asInstanceOf[IfApiResponseEventModel].matchId shouldEqual matchId
      capturedEvent.asInstanceOf[IfApiResponseEventModel].correlationId shouldEqual correlationId
      capturedEvent.asInstanceOf[IfApiResponseEventModel].requestUrl shouldBe ifUrl
      capturedEvent.asInstanceOf[IfApiResponseEventModel].ifDetails shouldBe ifResponse

    }

    "auditIfApiFailure" in {

      Mockito.reset(auditConnector)

      val msg = "Something went wrong"

      val captor = ArgumentCaptor.forClass(classOf[ApiFailureResponseEventModel])

      auditHelper.auditIfApiFailure(correlationId, matchId, request, ifUrl, msg)

      verify(auditConnector, times(1)).sendExplicitAudit(eqTo("IfApiFailureEvent"),
        captor.capture())(any(), any(), any())

      val capturedEvent = captor.getValue.asInstanceOf[ApiFailureResponseEventModel]
      capturedEvent.matchId shouldEqual matchId
      capturedEvent.correlationId shouldEqual Some(correlationId)
      capturedEvent.requestUrl shouldEqual ifUrl
      capturedEvent.response shouldEqual msg

    }

  }

}
