package doodlebot
package endpoint

import cats.effect.IO
import cats.implicits._
import doodlebot.action.Store
import doodlebot.models._
import doodlebot.validation.InputError
import io.circe.syntax._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.{EntityDecoder, HttpService}

object SignupRoute extends Http4sDsl[IO] {

  private implicit val userEntityDecoder: EntityDecoder[IO, User] = jsonOf[IO, User]

  val route = HttpService[IO] {
    case req @ POST -> Root / "signup" => {
      req
        .as[User]
        .map { user =>
          Store
            .signup(user)
            .fold(
              errors => {
                val errs =
                  errors.foldLeft(InputError.empty) { (accum, elt) =>
                    elt match {
                      case Store.EmailAlreadyExists(email) =>
                        accum |+| InputError("email", "This email is already taken")
                      case Store.NameAlreadyExists(name) =>
                        accum |+| InputError("name", "This name is already taken")
                    }
                  }
                FormErrors(errs).asLeft
              },
              session => {
                Authenticated(user.name, session).asRight
              }
            )
        }
        .flatMap {
          case Left(error)          => BadRequest(error.asJson)
          case Right(authenticated) => Ok(authenticated.asJson)
        }
    }
  }

}
