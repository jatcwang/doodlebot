package doodlebot

import doodlebot.message.{Message => Msg}
import org.scalajs.dom
import org.scalajs.dom.experimental.Fetch.fetch
import org.scalajs.dom.experimental.{HttpMethod, RequestInit, Response}

import scala.scalajs.js
import js.JSConverters._
import scala.scalajs.js.{Dictionary, UndefOr, |}
import scala.scalajs.js.JSON

sealed abstract class Effect extends Product with Serializable
object Effect {
  final case object NoEffect extends Effect
  final case class Message(message: Msg) extends Effect
  final case class PostRequest(
    path: String,
    payload: js.Dictionary[String],
    success: js.Dictionary[js.Any] => Msg,
    failure: Map[String, List[String]] => Msg,
    headers: List[(String, String)]
  ) extends Effect
  final case class Tick(message: Msg) extends Effect

  val noEffect: Effect =
    NoEffect
  def message(message: Msg): Effect =
    Message(message)
  def request(
    path: String,
    payload: js.Dictionary[String],
    success: js.Dictionary[js.Any] => Msg,
    failure: Map[String, List[String]] => Msg,
    headers: List[(String, String)] = List.empty
  ) =
    PostRequest(path, payload, success, failure, headers)
  def tick(message: Msg): Effect =
    Tick(message)

  def run(effect: Effect): Unit =
    effect match {
      case NoEffect =>
        ()

      case Message(message) =>
        DoodleBot.loop(message)

      case PostRequest(path, payload, success, failure, hdrs) =>
        dom.console.log("Sending", payload, " to ", path, " with headers", hdrs.toString)

        val callbackFailure: Any => Unit | js.Thenable[Unit] = (data: Any) => {
          dom.console.log("Ajax request failed with", data.asInstanceOf[js.Any])

          val raw = data.asInstanceOf[js.Dictionary[js.Dictionary[js.Dictionary[js.Array[String]]]]]
          val converted = raw("errors")("messages").toMap.mapValues(_.toList)

          DoodleBot.loop(failure(converted))
        }

        val callbackSuccess: Dictionary[js.Any] => Unit | js.Thenable[Unit] = (data: js.Dictionary[js.Any]) => {
          dom.console.log("Ajax request succeeded with", data)
          DoodleBot.loop(success(data))
        }

        val theHeaders: js.Dictionary[String] = js.Dictionary()
        hdrs.foreach { hdr => theHeaders += hdr }

        dom.console.log(payload)
        fetch(path, js.Dynamic.literal(
          method = HttpMethod.POST,
          headers = (hdrs :+ ("Content-Type" -> "application/json")).toMap.toJSDictionary,
          body = JSON.stringify(payload),
        ).asInstanceOf[RequestInit])
          .`then`[js.Promise[js.Any]]((resp: Response) => resp.json())
          .asInstanceOf[js.Promise[js.Dictionary[js.Any]]]
          .`then`[Unit](callbackSuccess, UndefOr.any2undefOrA(callbackFailure))

      case Tick(message) =>
        dom.window.setInterval(() => DoodleBot.loop(message), 1000)
    }

}
