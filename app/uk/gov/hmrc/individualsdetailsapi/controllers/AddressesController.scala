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

import play.api.hal.Hal.state
import play.api.hal.HalLink
import play.api.libs.json.Json
import play.api.mvc.hal._
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.individualsdetailsapi.audit.AuditHelper
import uk.gov.hmrc.individualsdetailsapi.play.RequestHeaderUtils.{maybeCorrelationId, validateCorrelationId}
import uk.gov.hmrc.individualsdetailsapi.service.ScopesService
import uk.gov.hmrc.individualsdetailsapi.services.DetailsService

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class AddressesController @Inject()(
                                     val authConnector: AuthConnector,
                                     cc: ControllerComponents,
                                     scopeService: ScopesService,
                                     detailsService: DetailsService,
                                     implicit val auditHelper: AuditHelper
                                   )(implicit val ec: ExecutionContext)
    extends CommonController(cc)
    with PrivilegedAuthentication {

  def addresses(matchId: UUID): Action[AnyContent] = Action.async {
    implicit request =>
      val scopes = scopeService.getEndPointScopes("addresses")
      authenticate(scopes, matchId.toString) { authScopes =>

        val correlationId = validateCorrelationId(request)

        detailsService
          .getResidences(matchId, "addresses", authScopes)
          .map { addresses =>
            {
              val selfLink = HalLink("self", s"/individuals/details/addresses?matchId=$matchId")
              val addressesJsObject = Json.obj("residences" -> Json.toJson(addresses))
              val response = state(addressesJsObject) ++ selfLink

              auditHelper.auditResidencesApiResponse(
                correlationId.toString, matchId.toString, authScopes.mkString(","),
                request, selfLink.toString, addresses)

              Ok(response)
            }
          }
      } recover recoveryWithAudit(maybeCorrelationId(request), matchId.toString, "/individuals/details/addresses")
  }
}