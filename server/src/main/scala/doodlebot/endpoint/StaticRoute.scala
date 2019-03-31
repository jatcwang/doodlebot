package doodlebot
package endpoint

import cats.effect.IO
import org.http4s.{Header, Headers, HttpService, Response}
import org.http4s.dsl.Http4sDsl
import fs2.io.readInputStream

object StaticRoute extends Http4sDsl[IO] {

  private val resourceLoader = this.getClass

  private val CHUNK_SIZE = 1024

  private val CONTENT_TYPE = "Content-Type"
  private val APPLICATION_JAVASCRIPT = "application/javascript"
  private val TEXT_HTML = "text/html"

  val route: HttpService[IO] = HttpService[IO] {
    case GET -> Root => {
      IO {
        Response[IO](
          Ok,
          body    = readInputStream(IO(resourceLoader.getResourceAsStream("/index.html")), chunkSize = CHUNK_SIZE),
          headers = Headers(Header(CONTENT_TYPE, TEXT_HTML))
        )
      }
    }
    case GET -> "ui" /: rest => {
      IO {
        val resourcePath = rest.toList.mkString("/", "/", "")
        println(s"Loading resource $resourcePath")
        Option(resourceLoader.getResourceAsStream(resourcePath)) match {
          case Some(inputStream) =>
            Response[IO](
              Ok,
              body    = readInputStream(IO(inputStream), chunkSize = CHUNK_SIZE),
              headers = Headers(Header(CONTENT_TYPE, APPLICATION_JAVASCRIPT))
            )
          case None => Response[IO](NotFound)
        }
      }
    }
  }

}
