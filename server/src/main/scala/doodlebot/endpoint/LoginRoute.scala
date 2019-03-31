package doodlebot
package endpoint

import cats.effect.IO
import doodlebot.action.Store
import doodlebot.models._
import doodlebot.validation.InputError
import io.circe.syntax._
import org.http4s.HttpService
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl

object LoginRoute extends Http4sDsl[IO] {

  val route: HttpService[IO] = HttpService[IO] {
    case req @ POST -> Root / "login" => {
      req.as[Login].flatMap { loginInfo =>
        Store.login(loginInfo) match {
          case Left(error) => {
            val errors = FormErrors(error match {
              case Store.NameDoesNotExist(name) =>
                InputError("name", "Nobody has signed up with this name")
              case Store.PasswordIncorrect =>
                InputError("password", "Your password is incorrect")
            })
            BadRequest(errors.asJson)
          }
          case Right(session) => {
            Ok(Authenticated(loginInfo.name, session).asJson)
          }
        }
      }
    }
  }

}
