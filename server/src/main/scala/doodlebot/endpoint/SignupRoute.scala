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

object SignupRoute extends Http4sDsl[IO] {

  private object NameParam extends QueryParamDecoderMatcher[String]("name")
  private object EmailParam extends QueryParamDecoderMatcher[String]("email")
  private object PasswordParam extends QueryParamDecoderMatcher[String]("password")

  val route = HttpService[IO] {
    case POST -> Root / "signup" :? NameParam(name) +& EmailParam(email) +& PasswordParam(password) => {
      IO {
        import doodlebot.syntax.validation._

        val validatedUser: Either[FormErrors, User] =
          (
            Name.validate(name).forInput("name"),
            Email.validate(email).forInput("email"),
            Password.validate(password).forInput("password")
          ).mapN { (n, e, p) =>
              User(n, e, p)
            }
            .toEither
            .leftMap(errs => FormErrors(errs))

        validatedUser.flatMap { user =>
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
                Authenticated(Name(name), session).asRight
              }
            )

        }
      }.flatMap {
        case Left(error)          => BadRequest(error.asJson)
        case Right(authenticated) => Ok(authenticated.asJson)
      }
    }
  }

}
