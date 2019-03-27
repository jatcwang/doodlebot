package doodlebot

import java.util.UUID

import io.circe.syntax._
import cats.data.ValidatedNel
import cats.implicits._
import io.circe.{Encoder, Json, ObjectEncoder}
import io.circe.generic.semiauto.deriveEncoder

object models {
  import doodlebot.syntax.validation._
  import doodlebot.validation.Predicate._
  import doodlebot.validation._

  final case class Log(offset: Int, messages: List[Message])

  object Log {
    implicit val encoder: ObjectEncoder[Log] = deriveEncoder[Log]
  }

  final case class Message(author: String, message: String)

  object Message {
    implicit val encoder: ObjectEncoder[Message] = deriveEncoder[Message]
  }

  final case class FormErrors(errors: InputError) extends Exception {}

  object FormErrors {
    implicit val encoder: ObjectEncoder[FormErrors] = deriveEncoder[FormErrors]
  }

  // Messages from the client
  final case class Login(name: Name, password: Password)

  // Wrappers
  final case class Name(get: String) extends AnyVal
  object Name {
    def validate(name: String): ValidatedNel[String, Name] =
      name.validate(lengthAtLeast(6) and onlyLettersOrDigits).map(n => Name(n))
    implicit val encoder: Encoder[Name] = new Encoder[Name] {
      override def apply(a: Name): Json = a.get.asJson
    }
  }
  final case class Email(get: String) extends AnyVal
  object Email {
    def validate(email: String): ValidatedNel[String, Email] =
      email.validate(containsAllChars("@.")).map(e => Email(e))
  }
  final case class Password(get: String) extends AnyVal
  object Password {
    def validate(password: String): ValidatedNel[String, Password] =
      password.validate(lengthAtLeast(8)).map(p => Password(p))
  }
  final case class Session(uuid: UUID = UUID.randomUUID()) extends AnyVal

  object Session {
    implicit val encoder: Encoder[Session] = new Encoder[Session] {
      override def apply(s: Session): Json = s.uuid.asJson
    }
  }

  final case class Authenticated(name: Name, session: Session)

  object Authenticated {
    implicit val encoder: ObjectEncoder[Authenticated] = deriveEncoder[Authenticated]
  }

  // State
  final case class User(name: Name, email: Email, password: Password)
  object User {
    def validate(name: String, email: String, password: String): ValidatedNel[String, User] =
      (Name.validate(name), Email.validate(email), Password.validate(password)).mapN { User.apply _ }
  }
}
