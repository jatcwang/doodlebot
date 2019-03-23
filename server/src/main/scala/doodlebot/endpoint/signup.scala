package doodlebot
package endpoint

import cats.implicits._
import io.finch._
import io.finch.syntax._
import doodlebot.action.Store
import doodlebot.validation.InputError

object Signup {
  import model._

  val signup: Endpoint[Authenticated] =
    post("signup" :: param("name") :: param("email") :: param("password")) { (name: String, email: String, password: String) =>
      import doodlebot.syntax.validation._

      val validatedUser: Either[FormErrors,User] =
        (
          Name.validate(name).forInput("name"),
          Email.validate(email).forInput("email"),
          Password.validate(password).forInput("password")
        ).mapN { (n, e, p) => User(n, e, p) }.toEither.leftMap(errs => FormErrors(errs))

      val result: Either[FormErrors,Authenticated] =
        validatedUser.flatMap { user =>
          Store.signup(user).fold(
            fa = errors => {
              val errs =
                errors.foldLeft(InputError.empty){ (accum, elt) =>
                  elt match {
                    case Store.EmailAlreadyExists(email) =>
                      accum |+| InputError("email", "This email is already taken")
                    case Store.NameAlreadyExists(name) =>
                      accum |+| InputError("name", "This name is already taken")
                  }
              }

              FormErrors(errs).asLeft
            },
            fb = session => {
              Authenticated(name, session.get.toString).asRight
            }
          )
        }

      result.fold(BadRequest, Ok)
    }
}
