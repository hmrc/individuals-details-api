/*
 * Copyright 2020 HM Revenue & Customs
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
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.individualsdetailsapi.service.ScopesService

import scala.concurrent.ExecutionContext

abstract class AddressesController @Inject()(
    cc: ControllerComponents,
    scopeService: ScopesService
)(implicit val ec: ExecutionContext)
    extends CommonController(cc)
    with PrivilegedAuthentication {

  def addresses(matchId: UUID): Action[AnyContent] = Action.async {
    implicit request =>
      val scopes =
        scopeService.getEndPointScopes("addresses")

      requiresPrivilegedAuthentication(scopes)
        .flatMap { authScopes =>
          //TODO implement routes and scopes
          throw new Exception("NOT_IMPLEMENTED")
        }
        .recover(recovery)
  }
}

class LiveAddressesController @Inject()(
    val authConnector: AuthConnector,
    cc: ControllerComponents,
    scopeService: ScopesService
)(implicit override val ec: ExecutionContext)
    extends AddressesController(cc, scopeService) {
  override val environment: String = Environment.PRODUCTION
}

class SandboxAddressesController @Inject()(
    val authConnector: AuthConnector,
    cc: ControllerComponents,
    scopeService: ScopesService
)(implicit override val ec: ExecutionContext)
    extends AddressesController(cc, scopeService) {
  override val environment: String = Environment.SANDBOX
}
