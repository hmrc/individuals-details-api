package uk.gov.hmrc.individualsdetailsapi.domains

import play.api.libs.functional.syntax.unlift
import play.api.libs.json.{Format, JsPath, Json}
import play.api.libs.functional.syntax._
import uk.gov.hmrc.individualsdetailsapi.domains.integrationframework.IfResidence


case class Residence(residenceType: Option[String],
                     address: Option[Address],
                     inUse: Option[Boolean] )

case class Residences( residences: Seq[Residence])

object Residence {

  implicit val residenceFormat: Format[Residence] = Format(
    (
      (JsPath \ "residenceType").readNullable[String] and
      (JsPath \ "address").readNullable[Address] and
      (JsPath \ "inUse").readNullable[Boolean]
    )(Residence.apply _),
    (
      (JsPath \ "residenceType").writeNullable[String] and
      (JsPath \ "address").writeNullable[Address] and
      (JsPath \ "inUse").writeNullable[Boolean]
    )(unlift(Residence.unapply))
  )

  implicit val residencesFormat: Format[Residences] = Format(
    (JsPath \ "residence").read[Seq[Residence]].map(Residences.apply),
    (JsPath \ "residence").write[Seq[Residence]].contramap(x => x.residences)
  )

  def create(residenceType: Option[String], address: Option[Address], noLongerUsed: Option[Boolean]): Option[Residence] =
    (residenceType, address, noLongerUsed) match {
      case (None, None, None) => None
      case _                  => Some(new Residence(residenceType, address, noLongerUsed))
    }

  def create(residence: Option[IfResidence]): Option[Residence] = {

    val residenceType: Option[String] = residence.flatMap(_.residenceType)
    val address: Option[Address] = residence.flatMap(a => Address.create(a.address))
    val inUse: Option[Boolean] = residence.flatMap(_.noLongerUsed match {
      case Some("Y") => Some(false)
      case Some("N") => Some(true)
      case _ => None
    })

    create(residenceType, address, inUse)
  }
}
