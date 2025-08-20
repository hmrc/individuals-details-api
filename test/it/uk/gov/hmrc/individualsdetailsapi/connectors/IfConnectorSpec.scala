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

package it.uk.gov.hmrc.individualsdetailsapi.connectors

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.{times, verify}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.Application
import testUtils.TestHelpers
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames, InternalServerException, NotFoundException}
import uk.gov.hmrc.individualsdetailsapi.audit.AuditHelper
import uk.gov.hmrc.individualsdetailsapi.connectors.IfConnector
import uk.gov.hmrc.individualsdetailsapi.domain.integrationframework.{IfContactDetail, IfDetailsResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import unit.uk.gov.hmrc.individualsdetailsapi.utils.SpecBase

import scala.concurrent.ExecutionContext

class IfConnectorSpec extends SpecBase with BeforeAndAfterEach with TestHelpers with MockitoSugar {
  val stubPort: Int = sys.env.getOrElse("WIREMOCK", "11122").toInt
  val stubHost = "localhost"
  val wireMockServer = new WireMockServer(wireMockConfig().port(stubPort))
  val integrationFrameworkAuthorizationToken = "IF_TOKEN"
  val integrationFrameworkEnvironment = "IF_ENVIRONMENT"
  val clientId = "CLIENT_ID"

  def externalServices: Seq[String] = Seq.empty

  override def fakeApplication(): Application = new GuiceApplicationBuilder()
    .bindings(bindModules*)
    .configure(
      "cache.enabled"                                                   -> false,
      "microservice.services.integration-framework.host"                -> "127.0.0.1",
      "microservice.services.integration-framework.port"                -> "11122",
      "microservice.services.integration-framework.authorization-token" -> integrationFrameworkAuthorizationToken,
      "microservice.services.integration-framework.environment"         -> integrationFrameworkEnvironment
    )
    .build()

  implicit val ec: ExecutionContext =
    fakeApplication().injector.instanceOf[ExecutionContext]

  trait Setup {
    val matchId = "80a6bb14-d888-436e-a541-4000674c60aa"
    val sampleCorrelationId = "188e9400-b636-4a3b-80ba-230a8c72b92a"
    val sampleCorrelationIdHeader: (String, String) = "CorrelationId" -> sampleCorrelationId

    implicit val hc: HeaderCarrier = HeaderCarrier()

    val config: ServicesConfig = fakeApplication().injector.instanceOf[ServicesConfig]
    val httpClient: HttpClientV2 = fakeApplication().injector.instanceOf[HttpClientV2]
    val auditHelper: AuditHelper = mock[AuditHelper]
    val underTest = new IfConnector(config, httpClient, auditHelper)
  }

  override def beforeEach(): Unit = {
    wireMockServer.start()
    configureFor(stubHost, stubPort)
  }

  override def afterEach(): Unit =
    wireMockServer.stop()

  val detailsData: IfDetailsResponse = createValidIFDetailsResponse()
  val emptyData: IfDetailsResponse = createEmptyDetailsResponse()

  "fetch details" should {
    val nino = Nino("NA000799C")

    "Fail when IF returns an error" in new Setup {

      Mockito.reset(underTest.auditHelper)

      stubFor(
        get(urlPathMatching(s"/individuals/details/contact/nino/$nino"))
          .willReturn(aResponse().withStatus(500))
      )

      intercept[InternalServerException] {
        await(
          underTest.fetchDetails(nino, None, matchId)(using
            hc,
            FakeRequest().withHeaders(sampleCorrelationIdHeader),
            ec
          )
        )
      }

      verify(underTest.auditHelper, times(1)).auditIfApiFailure(any(), any(), any(), any(), any())(using any())

    }

    "Fail when IF returns a bad request" in new Setup {

      Mockito.reset(underTest.auditHelper)

      stubFor(
        get(urlPathMatching(s"/individuals/details/contact/nino/$nino"))
          .willReturn(aResponse().withStatus(400).withBody("BAD_REQUEST"))
      )

      intercept[InternalServerException] {
        await(
          underTest.fetchDetails(nino, None, matchId)(using
            hc,
            FakeRequest().withHeaders(sampleCorrelationIdHeader),
            ec
          )
        )
      }

      verify(underTest.auditHelper, times(1)).auditIfApiFailure(any(), any(), any(), any(), any())(using any())
    }

    "Return an empty dataset for PERSON_NOT_FOUND" in new Setup {

      Mockito.reset(underTest.auditHelper)

      stubFor(
        get(urlPathMatching(s"/individuals/details/contact/nino/$nino"))
          .willReturn(aResponse().withStatus(404).withBody("PERSON_NOT_FOUND"))
      )

      val result: IfDetailsResponse = await(
        underTest.fetchDetails(nino, None, matchId)(using
          hc,
          FakeRequest().withHeaders(sampleCorrelationIdHeader),
          ec
        )
      )

      result shouldBe emptyData

      verify(underTest.auditHelper, times(1)).auditIfApiFailure(any(), any(), any(), any(), any())(using any())

    }

    "Fail when IF returns a NOT_FOUND" in new Setup {

      Mockito.reset(underTest.auditHelper)

      stubFor(
        get(urlPathMatching(s"/individuals/details/contact/nino/$nino"))
          .willReturn(aResponse().withStatus(404).withBody("NOT_FOUND"))
      )

      intercept[NotFoundException] {
        await(
          underTest.fetchDetails(nino, None, matchId)(using
            hc,
            FakeRequest().withHeaders(sampleCorrelationIdHeader),
            ec
          )
        )
      }
      verify(underTest.auditHelper, times(1)).auditIfApiFailure(any(), any(), any(), any(), any())(using any())
    }

    "Audit error when IF returns invalid data" in new Setup {

      Mockito.reset(underTest.auditHelper)

      val invalidData: IfDetailsResponse = detailsData.copy(
        contactDetails = Option(
          Seq(
            IfContactDetail(
              code = 7,
              detailType = "DAYTIME TELEPHONE",
              detail = ""
            )
          )
        )
      )

      stubFor(
        get(urlPathMatching(s"/individuals/details/contact/nino/$nino"))
          .willReturn(
            aResponse()
              .withStatus(200)
              .withBody(Json.toJson(invalidData).toString())
          )
      )

      intercept[InternalServerException] {
        await(
          underTest.fetchDetails(nino, None, matchId)(using
            hc,
            FakeRequest().withHeaders(sampleCorrelationIdHeader),
            ec
          )
        )
      }

      verify(underTest.auditHelper, times(1))
        .auditIfApiFailure(any(), any(), any(), any(), any())(using any())
    }

    "for standard response" in new Setup {

      Mockito.reset(underTest.auditHelper)

      stubFor(
        get(urlPathMatching(s"/individuals/details/contact/nino/$nino"))
          .withHeader(HeaderNames.authorisation, equalTo(s"Bearer $integrationFrameworkAuthorizationToken"))
          .withHeader("Environment", equalTo(integrationFrameworkEnvironment))
          .withHeader("CorrelationId", equalTo(sampleCorrelationId))
          .willReturn(
            aResponse()
              .withStatus(200)
              .withBody(Json.toJson(detailsData).toString())
          )
      )

      val result: IfDetailsResponse = await(
        underTest.fetchDetails(nino, None, matchId)(using
          hc,
          FakeRequest().withHeaders(sampleCorrelationIdHeader),
          ec
        )
      )

      result shouldBe detailsData

      verify(underTest.auditHelper, times(1)).auditIfApiResponse(any(), any(), any(), any(), any())(using any())

    }
  }
}
