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

package it.uk.gov.hmrc.individualsdetailsapi.connectors

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.{
  aResponse,
  configureFor,
  equalTo,
  get,
  stubFor,
  urlPathMatching
}
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.{times, verify}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import testUtils.TestHelpers
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{
  BadRequestException,
  HeaderCarrier,
  HttpClient,
  Upstream5xxResponse
}
import uk.gov.hmrc.individualsdetailsapi.connectors.IfConnector
import unit.uk.gov.hmrc.individualsdetailsapi.utils.SpecBase
import play.api.test.FakeRequest
import uk.gov.hmrc.individualsdetailsapi.audit.AuditHelper
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.ExecutionContext

class IfConnectorSpec
    extends SpecBase
    with BeforeAndAfterEach
    with TestHelpers
    with MockitoSugar {
  val stubPort = sys.env.getOrElse("WIREMOCK", "11122").toInt
  val stubHost = "localhost"
  val wireMockServer = new WireMockServer(wireMockConfig().port(stubPort))
  val integrationFrameworkAuthorizationToken = "IF_TOKEN"
  val integrationFrameworkEnvironment = "IF_ENVIRONMENT"
  val clientId = "CLIENT_ID"

  def externalServices: Seq[String] = Seq.empty

  override lazy val fakeApplication = new GuiceApplicationBuilder()
    .bindings(bindModules: _*)
    .configure(
      "microservice.services.integration-framework.host" -> "localhost",
      "microservice.services.integration-framework.port" -> "11122",
      "microservice.services.integration-framework.authorization-token" -> integrationFrameworkAuthorizationToken,
      "microservice.services.integration-framework.environment" -> integrationFrameworkEnvironment
    )
    .build()

  implicit val ec: ExecutionContext =
    fakeApplication.injector.instanceOf[ExecutionContext]

  trait Setup {
    val matchId = "80a6bb14-d888-436e-a541-4000674c60aa"
    val sampleCorrelationId = "188e9400-b636-4a3b-80ba-230a8c72b92a"
    val sampleCorrelationIdHeader = ("CorrelationId" -> sampleCorrelationId)

    implicit val hc = HeaderCarrier()

    val config = fakeApplication.injector.instanceOf[ServicesConfig]
    val httpClient = fakeApplication.injector.instanceOf[HttpClient]
    val auditHelper = mock[AuditHelper]
    val underTest = new IfConnector(config, httpClient, auditHelper)
  }

  override def beforeEach() {
    wireMockServer.start()
    configureFor(stubHost, stubPort)
  }

  override def afterEach() {
    wireMockServer.stop()
  }

  val detailsData = createValidIFDetailsResponse()

  "fetch details" should {
    val nino = Nino("NA000799C")

    "Fail when IF returns an error" in new Setup {

      Mockito.reset(underTest.auditHelper)

      stubFor(
        get(urlPathMatching(s"/individuals/details/contact/nino/$nino"))
          .willReturn(aResponse().withStatus(500)))

      intercept[Upstream5xxResponse] {
        await(
          underTest.fetchDetails(nino, None, matchId)(
            hc,
            FakeRequest().withHeaders(sampleCorrelationIdHeader),
            ec
          )
        )
      }

      verify(underTest.auditHelper,
        times(1)).auditIfApiFailure(any(), any(), any(), any(), any(), any())(any())

    }

    "Fail when IF returns a bad request" in new Setup {

      Mockito.reset(underTest.auditHelper)

      stubFor(
        get(urlPathMatching(s"/individuals/details/contact/nino/$nino"))
          .willReturn(aResponse().withStatus(400)))

      intercept[BadRequestException] {
        await(
          underTest.fetchDetails(nino, None, matchId)(
            hc,
            FakeRequest().withHeaders(sampleCorrelationIdHeader),
            ec
          )
        )
      }

      verify(underTest.auditHelper,
        times(1)).auditIfApiFailure(any(), any(), any(), any(), any(), any())(any())

    }

    "for standard response" in new Setup {

      Mockito.reset(underTest.auditHelper)

      stubFor(
        get(urlPathMatching(s"/individuals/details/contact/nino/$nino"))
          .withHeader(
            "Authorization",
            equalTo(s"Bearer $integrationFrameworkAuthorizationToken"))
          .withHeader("Environment", equalTo(integrationFrameworkEnvironment))
          .willReturn(aResponse()
            .withStatus(200)
            .withBody(Json.toJson(detailsData).toString())))

      val result = await(
        underTest.fetchDetails(nino, None, matchId)(
          hc,
          FakeRequest().withHeaders(sampleCorrelationIdHeader),
          ec
        )
      )

      result shouldBe detailsData

      verify(underTest.auditHelper,
        times(1)).auditIfApiResponse(any(), any(), any(), any(), any(), any())(any())

    }
  }
}
