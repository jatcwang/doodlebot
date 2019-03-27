package doodlebot
package endpoint

import cats.effect.IO
import cats.implicits._
import doodlebot.action.Store
import doodlebot.models._
import doodlebot.validation.InputError
import io.circe.syntax._
import org.http4s.HttpService
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl

object LoginRoute extends Http4sDsl[IO] {
  object NameParam extends QueryParamDecoderMatcher[String]("password")
  object PasswordParam extends QueryParamDecoderMatcher[String]("password")

  val route = HttpService[IO] {
    case POST -> Root / "login" :? NameParam(nameStr) +& PasswordParam(passwordStr) => {
      IO {
        val name = Name(nameStr)
        val login = Login(name, Password(passwordStr))
        Store
          .login(login) match {
          case Left(error) => {
              FormErrors(error match {
                case Store.NameDoesNotExist(name) =>
                  InputError("name", "Nobody has signed up with this name")
                case Store.PasswordIncorrect =>
                  InputError("password", "Your password is incorrect")
              }).asLeft
            }
          case Right(session) => {
              Authenticated(name, session).asRight
            }
        }
      }.flatMap {
        case Left(error)          => BadRequest(error.asJson)
        case Right(authenticated) => Ok(authenticated.asJson)
      }
    }
  }

}
