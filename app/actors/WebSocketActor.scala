package actors

import actors.WebSocketActor.{Ping, RankIndexMessage}
import akka.actor._
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.Subscribe
import com.typesafe.scalalogging.StrictLogging
import models.RankIndexData
import ophan.thrift.event.Acquisition
import utils.JsonSerializer

import scala.concurrent.duration._

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
      out ! s"""{"echo": "$msg"}"""

    case acquisition: Acquisition =>
      out ! JsonSerializer(acquisition)

    case RankIndexMessage(rankIndexData) =>
      out ! JsonSerializer(rankIndexData)

    case Ping =>
      out ! s"""{"ping": true}"""
  }

  override def postStop() = {
    logger.info("Websocket closed")
  }
}
