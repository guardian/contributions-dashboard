package actors

import actors.WebSocketActor.{Ping, RankIndexMessage}
import akka.actor._
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.Subscribe
import com.typesafe.scalalogging.StrictLogging
import io.circe.Encoder
import models.RankIndexData
import ophan.thrift.event.Acquisition

import scala.concurrent.duration._

case class MessageToClient[T : Encoder](messageType: String, payload: Option[T] = None)

case object MessageToClient {
  import io.circe.generic.auto._
  import io.circe.syntax._
  import ophan.thrift.event.Acquisition
  import com.gu.fezziwig.CirceScroogeMacros._
  import models.RankIndexData

  def ping = serialize(MessageToClient[Unit]("ping"))
  def echo(payload: String) = serialize(MessageToClient[String]("echo", Some(payload)))
  def acquisition(payload: Acquisition) = serialize(MessageToClient[Acquisition]("acquisition", Some(payload)))
  def rankIndex(payload: List[RankIndexData]) = serialize(MessageToClient[List[RankIndexData]]("rankIndex", Some(payload)))

  private def serialize[T : Encoder](message: MessageToClient[T]): String = message.asJson.noSpaces
}

object WebSocketActor {
  def props(out: ActorRef, acquisitionTopicName: String, rankIndexTopicName: String) =
    Props(new WebSocketActor(out, acquisitionTopicName, rankIndexTopicName))

  case object Ping
  case class RankIndexMessage(rankIndexData: List[RankIndexData])
}

class WebSocketActor(out: ActorRef, acquisitionTopicName: String, rankIndexTopicName: String) extends Actor with StrictLogging {

  logger.info("New websocket")

  private val mediator = DistributedPubSub(context.system).mediator
  mediator ! Subscribe(acquisitionTopicName, self)
  mediator ! Subscribe(rankIndexTopicName, self)

  context.system.scheduler.schedule(30.seconds, 30.seconds, self, Ping)(context.dispatcher)

  def receive = {
    case msg: String =>
      out ! MessageToClient.echo(msg)

    case acquisition: Acquisition =>
      out ! MessageToClient.acquisition(acquisition)

    case RankIndexMessage(rankIndexData) =>
      out ! MessageToClient.rankIndex(rankIndexData)

    case Ping =>
      out ! MessageToClient.ping
  }

  override def postStop() = {
    logger.info("Websocket closed")
  }
}
