package actors

import actors.WebSocketActor.Ping
import akka.actor._
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.Subscribe
import com.typesafe.scalalogging.StrictLogging
import ophan.thrift.event.Acquisition
import utils.AcquisitionSerializer

import scala.concurrent.duration._

object WebSocketActor {
  def props(out: ActorRef, topicName: String) = Props(new WebSocketActor(out, topicName))

  case object Ping
}

class WebSocketActor(out: ActorRef, topicName: String) extends Actor with StrictLogging {

  logger.info("New websocket")

  private val mediator = DistributedPubSub(context.system).mediator
  mediator ! Subscribe(topicName, self)

  context.system.scheduler.schedule(30.seconds, 30.seconds, self, Ping)(context.dispatcher)

  def receive = {
    case msg: String =>
      out ! s"""{"echo": "$msg"}"""

    case acquisition: Acquisition =>
      out ! AcquisitionSerializer(acquisition)

    case Ping =>
      out ! s"""{"ping": true}"""
  }

  override def postStop() = {
    logger.info("Websocket closed")
  }
}
