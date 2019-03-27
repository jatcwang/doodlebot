package doodlebot
package validation

import cats.Monoid
import cats.data.NonEmptyList
import cats.implicits._
import io.circe.ObjectEncoder
import io.circe.generic.semiauto.deriveEncoder

final case class InputError(messages: Map[String,NonEmptyList[String]])
object InputError {

  implicit val encoder: ObjectEncoder[InputError] = deriveEncoder[InputError]

  val empty: InputError = InputError(Map.empty)

  def apply(name: String, messages: NonEmptyList[String]): InputError =
    InputError(Map(name -> messages))

  def apply(name: String, message: String): InputError =
    InputError(Map(name -> NonEmptyList.of(message)))

  implicit object inputErrorInstances extends Monoid[InputError] {
    override def combine(a1: InputError, a2: InputError): InputError =
      InputError(a1.messages |+| a2.messages)

    override def empty: InputError =
      InputError(Map.empty)
  }
}
