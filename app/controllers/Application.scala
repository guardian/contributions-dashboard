package controllers

import actors.WebSocketActor
import play.api.mvc._
import play.api.libs.streams.ActorFlow
import akka.actor.ActorSystem
import akka.stream.Materializer
import com.typesafe.scalalogging.StrictLogging

class Application(components: ControllerComponents, acquisitionTopicName: String, rankIndexTopicName: String)
                 (implicit system: ActorSystem, mat: Materializer) extends AbstractController(components) with StrictLogging {
  def index = Action {
    Ok(views.html.index())
  }

  def socket = WebSocket.accept[String, String] { request =>
    ActorFlow.actorRef { out =>
      WebSocketActor.props(out, acquisitionTopicName, rankIndexTopicName)
    }
  }
}
