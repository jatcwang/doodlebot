package doodlebot

import java.util.UUID

import cats.data.{Validated, ValidatedNel}
import cats.effect.IO
import cats.implicits._
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax._
import io.circe.{Decoder, Encoder, Json, ObjectEncoder}
import org.http4s.EntityDecoder
import org.http4s.circe._
import doodlebot.validation.ValidationHelpers._

object models {
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

  object Login {
    implicit val entityDecoder: EntityDecoder[IO, Login] = {
      implicit val decoder: Decoder[Login] = deriveDecoder[Login]
      jsonOf[IO, Login]
    }
  }

  // Wrappers
  final case class Name(get: String) extends AnyVal
  object Name {
    def validate(str: String): ValidatedNel[String, Name] = {
      (lengthAtLeast(str, 6), onlyLettersOrDigits(str)).mapN {
        case (_, _) =>
          Name(str)
      }
    }

    implicit val encoder: Encoder[Name] = new Encoder[Name] {
      override def apply(a: Name): Json = a.get.asJson
    }

    implicit val decoder: Decoder[Name] = Decoder.decodeString.emap(str => validate(str).leftMap(_.head).toEither)
  }

  final case class Email(get: String) extends AnyVal

  object Email {
    def validate(str: String): ValidatedNel[String, Email] =
      Validated
        .condNel(str.contains("@"), (), "Email must contain the @ character")
        .map(_ => Email(str))

    implicit val decoder: Decoder[Email] = Decoder.decodeString.emap(str => validate(str).leftMap(_.head).toEither)
  }
  final case class Password(get: String) extends AnyVal
  object Password {
    def validate(str: String): ValidatedNel[String, Password] = {
      lengthAtLeast(str, 8)
        .map(_ => Password(str))
    }

    implicit val decoder: Decoder[Password] = Decoder.decodeString.emap(str => validate(str).leftMap(_.head).toEither)
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
    implicit val decoder: Decoder[User] = deriveDecoder[User]
  }
}
