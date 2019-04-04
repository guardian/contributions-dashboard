package actors

import akka.actor._
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.Publish
import com.typesafe.scalalogging.StrictLogging
import ophan.thrift.event.Acquisition

object AcquisitionPublisher {
  def props(topicName: String) = Props(new AcquisitionPublisher(topicName))
}

class AcquisitionPublisher(topicName: String) extends Actor with StrictLogging {

  private val mediator = DistributedPubSub(context.system).mediator

  def receive = {
    case acquisition: Acquisition =>
      mediator ! Publish(topicName, acquisition)
  }
}
