package doodlebot
package endpoint

import java.util.UUID

import cats.data.{Kleisli, OptionT}
import cats.effect.IO
import cats.implicits._
import doodlebot.action.Store
import doodlebot.models.{Message, Name, Session}
import io.circe.Json
import io.circe.syntax._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.AuthMiddleware
import org.http4s.util.CaseInsensitiveString
import org.http4s.{AuthedService, Request}
import sun.misc.BASE64Decoder

object ChatRoute extends Http4sDsl[IO] {

  private object NameParam extends QueryParamDecoderMatcher[String]("name")
  private object MessageParam extends QueryParamDecoderMatcher[String]("message")

  private object OffsetParam extends QueryParamDecoderMatcher[Int]("offset")

  final case class AuthedUser(name: String)

  val authenticateUser: Kleisli[OptionT[IO, ?], Request[IO], AuthedUser] =
    Kleisli(
      request =>
        OptionT(IO {
          request.headers.get(CaseInsensitiveString("authorization")) match {
            case Some(authHeader) =>
              authHeader.value.split(" ") match {
                case Array(scheme, params) =>
                  if (scheme.toLowerCase == "basic") {
                    new String(new BASE64Decoder().decodeBuffer(params)).split(":", 2) match {
                      case Array(name, session) =>
                        try {
                          val n = Name(name)
                          val s = Session(UUID.fromString(session))
                          if (Store.authenticated(n, s)) {
                            Some(AuthedUser(name))
                          } else None
                        } catch {
                          case _: IllegalArgumentException =>
                            None
                        }

                      case _ => None
                    }
                  } else {
                    None
                  }
                case _ => None
              }
            case None => None
          }
        })
    )

  private val unauthedRoute = AuthedService[AuthedUser, IO] {
    case authedRequest @ POST -> Root / "message" as user => {
      authedRequest.req.as[Json].flatMap { json =>
        json.hcursor.downField("message").as[String] match {
          case Left(err) => BadRequest(err.toString)
          case Right(message) => Ok(Store.message(Message(user.name, message)))
        }
      }
    }
    case authedRequest @ POST -> Root / "poll" as user => {
      authedRequest.req.as[Json].flatMap { json =>
        json.hcursor.downField("offset").as[String].leftMap(failure => failure.toString).flatMap(strOffset => {
          try {
            Right(strOffset.toInt)
          }
          catch {
            case e: NumberFormatException => Left("offset not a number")
          }
        }) match {
          case Left(errMsg) => BadRequest(errMsg)
          case Right(offset) => Ok(Store.poll(offset).asJson)
        }
      }
    }
  }

  val route = AuthMiddleware.withFallThrough(authenticateUser).apply(unauthedRoute)

}
