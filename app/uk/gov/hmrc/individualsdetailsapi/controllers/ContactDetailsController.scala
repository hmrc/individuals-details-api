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

import javax.inject.{Inject, Singleton}
import play.api.hal.Hal.state
import play.api.hal.Hal.links
import play.api.mvc.hal._
import play.api.hal.HalLink
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.individualsdetailsapi.audit.AuditHelper
import uk.gov.hmrc.individualsdetailsapi.play.RequestHeaderUtils.{maybeCorrelationId, validateCorrelationId}
import uk.gov.hmrc.individualsdetailsapi.service.ScopesService
import uk.gov.hmrc.individualsdetailsapi.services.{DetailsService, LiveDetailsService, SandboxDetailsService}

import scala.concurrent.ExecutionContext

abstract class ContactDetailsController @Inject()(
    cc: ControllerComponents,
    scopeService: ScopesService,
    detailsService: DetailsService,
    implicit val auditHelper: AuditHelper)(implicit val ec: ExecutionContext)
    extends CommonController(cc)
    with PrivilegedAuthentication {

  def contactDetails(matchId: UUID): Action[AnyContent] = Action.async {
    implicit request =>
      val scopes = scopeService.getEndPointScopes("contact-details")
      authenticate(scopes, matchId.toString) { authScopes =>

        val correlationId = validateCorrelationId(request)

        detailsService
          .getContactDetails(matchId, "contact-details", authScopes)
          .map { contactDetails =>
            {
              val selfLink = HalLink("self", s"/individuals/details/contact-details?matchId=$matchId")
              val obj = contactDetails.fold(Json.obj().as[JsValue])(cd => Json.toJson(cd))
              val response = state(Json.obj("contactDetails" -> obj)) ++ selfLink

              auditHelper.auditContactDetailsApiResponse(
                correlationId.toString, matchId.toString, authScopes.mkString(","),
                request, selfLink.toString, contactDetails)

              Ok(response)
            }
          }
      } recover recoveryWithAudit(maybeCorrelationId(request), matchId.toString, "/individuals/details/contact-details")
  }
}

@Singleton
class LiveContactDetailsController @Inject()(
    val authConnector: AuthConnector,
    cc: ControllerComponents,
    scopeService: ScopesService,
    detailsService: LiveDetailsService,
    auditHelper: AuditHelper
)(implicit override val ec: ExecutionContext)
    extends ContactDetailsController(cc, scopeService, detailsService, auditHelper) {

  override val environment = Environment.PRODUCTION

}

@Singleton
class SandboxContactDetailsController @Inject()(
    val authConnector: AuthConnector,
    cc: ControllerComponents,
    scopeService: ScopesService,
    detailsService: SandboxDetailsService,
    auditHelper: AuditHelper
)(implicit override val ec: ExecutionContext)
    extends ContactDetailsController(cc, scopeService, detailsService, auditHelper) {

  override val environment = Environment.SANDBOX

}
