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

package uk.gov.hmrc.individualsdetailsapi.handlers

import javax.inject.Inject
import play.api.Configuration
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.config.HttpAuditEvent
import uk.gov.hmrc.play.bootstrap.http.{ErrorResponse, JsonErrorHandler}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CustomErrorHandler @Inject()(auditConnector: AuditConnector,
                                   httpAuditEvent: HttpAuditEvent,
                                   configuration: Configuration)
    extends JsonErrorHandler(auditConnector, httpAuditEvent, configuration) {
//TODO - implement at a later date (see individuals-income)
}
