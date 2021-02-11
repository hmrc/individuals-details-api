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

package uk.gov.hmrc.individualsdetailsapi.controllers

import java.util.UUID
import javax.inject.Inject
import play.api.hal.Hal.state
import play.api.mvc.hal._
import play.api.hal.HalLink
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.individualsdetailsapi.audit.AuditHelper
import uk.gov.hmrc.individualsdetailsapi.play.RequestHeaderUtils.{maybeCorrelationId, validateCorrelationId}
import uk.gov.hmrc.individualsdetailsapi.service.ScopesService
import uk.gov.hmrc.individualsdetailsapi.services.{DetailsService, LiveDetailsService, SandboxDetailsService}

import scala.concurrent.ExecutionContext

abstract class AddressesController @Inject()(
    cc: ControllerComponents,
    scopeService: ScopesService,
    detailsService: DetailsService,
    implicit val auditHelper: AuditHelper)(implicit val ec: ExecutionContext)
    extends CommonController(cc)
    with PrivilegedAuthentication {

  def addresses(matchId: UUID): Action[AnyContent] = Action.async {
    implicit request =>
      val scopes = scopeService.getEndPointScopes("addresses")
      authenticate(scopes, matchId.toString) { authScopes =>

        auditHelper.auditAuthScopes(matchId.toString, authScopes.mkString(","), request)

        val correlationId = validateCorrelationId(request)

        detailsService
          .getResidences(matchId, "addresses", authScopes)
          .map { addresses =>
            {
              val selfLink = HalLink("self", s"/individuals/details/addresses?matchId=$matchId")
              val addressesJsObject = Json.obj("residences" -> Json.toJson(addresses))
              val response = state(addressesJsObject) ++ selfLink

              auditHelper.auditApiResponse(
                correlationId.toString, matchId.toString, Some(authScopes.mkString(",")),
                request, selfLink.toString, Json.toJson((response)))

              Ok(response)
            }
          }
      } recover recoveryWithAudit(maybeCorrelationId(request), matchId.toString, "/individuals/details/addresses")
  }
}

class LiveAddressesController @Inject()(
    val authConnector: AuthConnector,
    cc: ControllerComponents,
    scopeService: ScopesService,
    detailsService: LiveDetailsService,
    auditHelper: AuditHelper
)(implicit override val ec: ExecutionContext)
    extends AddressesController(cc, scopeService, detailsService, auditHelper) {
  override val environment: String = Environment.PRODUCTION
}

class SandboxAddressesController @Inject()(
    val authConnector: AuthConnector,
    cc: ControllerComponents,
    scopeService: ScopesService,
    detailsService: SandboxDetailsService,
    auditHelper: AuditHelper
)(implicit override val ec: ExecutionContext)
    extends AddressesController(cc, scopeService, detailsService, auditHelper) {
  override val environment: String = Environment.SANDBOX
}
