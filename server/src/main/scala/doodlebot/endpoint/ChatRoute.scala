package doodlebot
package endpoint

import java.util.UUID

import cats.data.{Kleisli, OptionT}
import cats.effect.IO
import doodlebot.action.Store
import doodlebot.models.{Message, Name, Session}
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

  //TODOO: This is not secure!!
  private val unauthedRoute = AuthedService[AuthedUser, IO] {
    case POST -> Root / "message" :? NameParam(name) +& MessageParam(message) as user => {
      val msg = Message(name, message)
      Ok(Store.message(msg))
    }
    case POST -> Root / "poll" :? OffsetParam(offset) as user => {
      Ok(Store.poll(offset).asJson)
    }
  }

  val route = AuthMiddleware.withFallThrough(authenticateUser).apply(unauthedRoute)

}
