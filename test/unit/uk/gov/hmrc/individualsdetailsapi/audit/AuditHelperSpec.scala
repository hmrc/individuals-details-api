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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{times, verify}
import org.mockito.{ArgumentCaptor, Mockito}
import org.scalatest.{AsyncWordSpec, Matchers}
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import play.api.test.FakeRequest
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.individualsdetailsapi.audit.{AuditHelper, DefaultHttpExtendedAuditEvent}
import uk.gov.hmrc.individualsdetailsapi.domain.integrationframework.{IfContactDetail, IfDetailsResponse}
import uk.gov.hmrc.individualsdetailsapi.domain.{ContactDetails, Residence}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent

class AuditHelperSpec extends AsyncWordSpec with Matchers with MockitoSugar {

  implicit val hc = HeaderCarrier()

  val nino = "CS700100A"
  val correlationId = "test"
  val scopes = "test"
  val matchId = "80a6bb14-d888-436e-a541-4000674c60aa"
  val request = FakeRequest()
  val contactDetailsResponse = ContactDetails(List.empty, List.empty, List.empty)
  val residencesResponse = Seq.empty[Residence]
  val ifRespone = IfDetailsResponse(None,None)
  val ifUrl =
    s"host/individuals/details/contact/nino/$nino?startDate=2019-01-01&endDate=2020-01-01&fields=some(vals(val1),val2)"
  val endpoint = "/test"

  val auditConnector = mock[AuditConnector]
  val httpExtendedAuditEvent = new DefaultHttpExtendedAuditEvent("individuals-details-api")

  val auditHelper = new AuditHelper(auditConnector)

  "Auth helper" should {

    "auditAuthScopes" in {

      Mockito.reset(auditConnector)

      val captor = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])

      auditHelper.auditAuthScopes(matchId, scopes, request)

      verify(auditConnector, times(1)).sendExtendedEvent(captor.capture())(any(), any())

      val result = Json.parse(
        """
          |{
          |  "apiVersion": "1.0",
          |  "matchId": "80a6bb14-d888-436e-a541-4000674c60aa",
          |  "scopes": "test",
          |  "method": "GET",
          |  "deviceID": "-",
          |  "ipAddress": "-",
          |  "referrer": "-",
          |  "Authorization": "-",
          |  "input": "Request to /",
          |  "userAgentString": "-"
          |}
          |""".stripMargin)

      val capturedEvent = captor.getValue
      capturedEvent.asInstanceOf[ExtendedDataEvent].auditSource shouldEqual "individuals-details-api"
      capturedEvent.asInstanceOf[ExtendedDataEvent].auditType shouldEqual "AuthScopesAuditEvent"
      capturedEvent.asInstanceOf[ExtendedDataEvent].detail shouldBe result

    }

    "auditResidencesApiResponse" in {

      Mockito.reset(auditConnector)

      val captor = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])

      auditHelper.auditResidencesApiResponse(correlationId, matchId, scopes, request, endpoint, residencesResponse)

      verify(auditConnector, times(1)).sendExtendedEvent(captor.capture())(any(), any())

      val result = Json.parse(
        """
          |{
          |  "apiVersion": "1.0",
          |  "matchId": "80a6bb14-d888-436e-a541-4000674c60aa",
          |  "correlationId": "test",
          |  "scopes": "test",
          |  "requestUrl":"/test",
          |  "response": "[\"some\",\"json\"]",
          |  "method": "GET",
          |  "deviceID": "-",
          |  "ipAddress": "-",
          |  "referrer": "-",
          |  "Authorization": "-",
          |  "input": "Request to /",
          |  "userAgentString": "-"
          |}
          |""".stripMargin)

      val capturedEvent = captor.getValue
      capturedEvent.asInstanceOf[ExtendedDataEvent].auditSource shouldEqual "individuals-details-api"
      capturedEvent.asInstanceOf[ExtendedDataEvent].auditType shouldEqual "ApiResponseEvent"
      capturedEvent.asInstanceOf[ExtendedDataEvent].detail shouldBe result

    }

    "auditContactDetailsApiResponse" in {

      Mockito.reset(auditConnector)

      val captor = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])

      auditHelper.auditContactDetailsApiResponse(correlationId, matchId, scopes, request, endpoint, Some(contactDetailsResponse))

      verify(auditConnector, times(1)).sendExtendedEvent(captor.capture())(any(), any())

      val result = Json.parse(
        """
          |{
          |  "apiVersion": "1.0",
          |  "matchId": "80a6bb14-d888-436e-a541-4000674c60aa",
          |  "correlationId": "test",
          |  "scopes": "test",
          |  "requestUrl":"/test",
          |  "response": "[\"some\",\"json\"]",
          |  "method": "GET",
          |  "deviceID": "-",
          |  "ipAddress": "-",
          |  "referrer": "-",
          |  "Authorization": "-",
          |  "input": "Request to /",
          |  "userAgentString": "-"
          |}
          |""".stripMargin)

      val capturedEvent = captor.getValue
      capturedEvent.asInstanceOf[ExtendedDataEvent].auditSource shouldEqual "individuals-details-api"
      capturedEvent.asInstanceOf[ExtendedDataEvent].auditType shouldEqual "ApiResponseEvent"
      capturedEvent.asInstanceOf[ExtendedDataEvent].detail shouldBe result

    }

    "auditApiFailure" in {

      Mockito.reset(auditConnector)

      val msg = "Something went wrong"

      val captor = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])

      auditHelper.auditApiFailure(Some(correlationId), matchId, request, "/test", msg)

      verify(auditConnector, times(1)).sendExtendedEvent(captor.capture())(any(), any())

      val result = Json.parse(
        """
          |{
          |  "apiVersion": "1.0",
          |  "matchId": "80a6bb14-d888-436e-a541-4000674c60aa",
          |  "correlationId": "test",
          |  "requestUrl":"/test",
          |  "response": "Something went wrong",
          |  "method": "GET",
          |  "deviceID": "-",
          |  "ipAddress": "-",
          |  "referrer": "-",
          |  "Authorization": "-",
          |  "input": "Request to /",
          |  "userAgentString": "-"
          |}
          |""".stripMargin)

      val capturedEvent = captor.getValue
      capturedEvent.asInstanceOf[ExtendedDataEvent].auditSource shouldEqual "individuals-details-api"
      capturedEvent.asInstanceOf[ExtendedDataEvent].auditType shouldEqual "ApiFailureEvent"
      capturedEvent.asInstanceOf[ExtendedDataEvent].detail shouldBe result

    }

    "auditIfApiResponse" in {

      Mockito.reset(auditConnector)

      val captor = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])

      auditHelper.auditIfApiResponse(correlationId, matchId, request, ifUrl, ifRespone)

      verify(auditConnector, times(1)).sendExtendedEvent(captor.capture())(any(), any())

      val result = Json.parse(
        """
          |{
          |  "apiVersion": "1.0",
          |  "matchId": "80a6bb14-d888-436e-a541-4000674c60aa",
          |  "correlationId": "test",
          |  "scopes": "test",
          |  "requestUrl": "host/individuals/details/contact/nino/CS700100A?startDate=2019-01-01&endDate=2020-01-01&fields=some(vals(val1),val2)",
          |  "response": "[\"some\",\"json\"]",
          |  "method": "GET",
          |  "deviceID": "-",
          |  "ipAddress": "-",
          |  "referrer": "-",
          |  "Authorization": "-",
          |  "input": "Request to /",
          |  "userAgentString": "-"
          |}
          |""".stripMargin)

      val capturedEvent = captor.getValue
      capturedEvent.asInstanceOf[ExtendedDataEvent].auditSource shouldEqual "individuals-details-api"
      capturedEvent.asInstanceOf[ExtendedDataEvent].auditType shouldEqual "IfApiResponseEvent"
      capturedEvent.asInstanceOf[ExtendedDataEvent].detail shouldBe result

    }

    "auditIfApiFailure" in {

      Mockito.reset(auditConnector)

      val msg = "Something went wrong"

      val captor = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])

      auditHelper.auditIfApiFailure(correlationId, matchId, request, ifUrl, msg)

      verify(auditConnector, times(1)).sendExtendedEvent(captor.capture())(any(), any())

      val result = Json.parse(
        """
          |{
          |  "apiVersion": "1.0",
          |  "matchId": "80a6bb14-d888-436e-a541-4000674c60aa",
          |  "correlationId": "test",
          |  "scopes": "test",
          |  "requestUrl": "host/individuals/details/contact/nino/CS700100A?startDate=2019-01-01&endDate=2020-01-01&fields=some(vals(val1),val2)",
          |  "response": "Something went wrong",
          |  "method": "GET",
          |  "deviceID": "-",
          |  "ipAddress": "-",
          |  "referrer": "-",
          |  "Authorization": "-",
          |  "input": "Request to /",
          |  "userAgentString": "-"
          |}
          |""".stripMargin)

      val capturedEvent = captor.getValue
      capturedEvent.asInstanceOf[ExtendedDataEvent].auditSource shouldEqual "individuals-details-api"
      capturedEvent.asInstanceOf[ExtendedDataEvent].auditType shouldEqual "IfApiFailureEvent"
      capturedEvent.asInstanceOf[ExtendedDataEvent].detail shouldBe result

    }

  }

}
