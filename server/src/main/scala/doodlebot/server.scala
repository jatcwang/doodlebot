package doodlebot

import cats.effect._
import doodlebot.models.{Email, Name, Password, User}
import fs2.StreamApp
import org.http4s.dsl.Http4sDsl
import org.http4s.server.blaze.BlazeBuilder

import scala.concurrent.ExecutionContext.Implicits.global

object DoodleBot extends StreamApp[IO] with Http4sDsl[IO] {
  override def stream(args: List[String], requestShutdown: IO[Unit]): fs2.Stream[IO, StreamApp.ExitCode] = {
    import doodlebot.endpoint._

    import doodlebot.action.Store

    Store.signup(User(Name("tester"), Email("tester@example.com"), Password("password")))

    BlazeBuilder[IO]
      .bindHttp(8080, "localhost")
      .mountService(ChatRoute.route)
      .mountService(LoginRoute.route)
      .mountService(SignupRoute.route)
      .mountService(StaticRoute.route)
      .serve
  }


}
