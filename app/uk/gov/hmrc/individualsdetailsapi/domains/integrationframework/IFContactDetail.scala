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

package uk.gov.hmrc.individualsdetailsapi.domains.integrationframework

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

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

import scala.util.matching.Regex

case class IFContactDetail(code: Int, detailType: String, detail: String)

object IFContactDetail {

  implicit val contactDetailsFormat: Format[IFContactDetail] = Format(
    (
      (JsPath \ "code").read[Int](min[Int](1).keepAnd(max[Int](9999))) and
        (JsPath \ "type")
          .read[String](minLength[String](1).keepAnd(maxLength[String](35))) and
        (JsPath \ "detail")
          .read[String](minLength[String](3).keepAnd(maxLength[String](72)))
    )(IFContactDetail.apply _),
    (
      (JsPath \ "code").write[Int] and
        (JsPath \ "type").write[String] and
        (JsPath \ "detail").write[String]
    )(unlift(IFContactDetail.unapply))
  )
}
