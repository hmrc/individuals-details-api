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

package uk.gov.hmrc.individualsdetailsapi.audit.events

import javax.inject.Inject
import uk.gov.hmrc.individualsdetailsapi.audit.HttpExtendedAuditEvent

class ApiResponseEvent @Inject()(httpAuditEvent: HttpExtendedAuditEvent)
  extends ResponseEventBase(httpAuditEvent) {

  override def auditType = "ApiResponseEvent"
  override def transactionName = "AuditCall"
  override def apiVersion = "1.0"

}
