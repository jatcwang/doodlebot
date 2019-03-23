package doodlebot
package endpoint

import io.finch._
import io.finch.syntax._
import cats.implicits._
import doodlebot.action.Store
import doodlebot.validation.InputError

object Login {
  import doodlebot.model._

  val login: Endpoint[Authenticated] = post("login" :: param("name") :: param("password")) { (name: String, password: String) =>
    val login = model.Login(Name(name), Password(password))
    val result: Either[FormErrors,Authenticated] =
      Store.login(login).fold(
        fa = error => {
          FormErrors(
            error match {
              case Store.NameDoesNotExist(name) =>
                InputError("name", "Nobody has signed up with this name")
              case Store.PasswordIncorrect =>
                InputError("password", "Your password is incorrect")
            }
          ).asLeft
        },

        fb = session => {
          Authenticated(name, session.get.toString).asRight
        }
      )

    result.fold(BadRequest, Ok)
  }
}
