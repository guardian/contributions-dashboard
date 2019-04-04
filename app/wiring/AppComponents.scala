package wiring

import actors.{AcquisitionPublisher, KinesisActor}
import play.api.ApplicationLoader.Context
import play.api.{BuiltInComponentsFromContext, NoHttpFiltersComponents}
import play.api.routing.Router
import router.Routes
import controllers._

class AppComponents(context: Context) extends BuiltInComponentsFromContext(context) with NoHttpFiltersComponents with AssetsComponents {

  val topicName = "acquisition-topic"
  val streamName = context.initialConfiguration.get[String]("kinesis.streamName")

  val kinesisActor = {
    val stage = context.initialConfiguration.get[String]("stage")
    val acquisitionPublisher = actorSystem.actorOf(AcquisitionPublisher.props(topicName), "acquisitionPublisher")

    actorSystem
      .actorOf(KinesisActor.props(acquisitionPublisher, streamName, stage)
      .withDispatcher("kinesis-dispatcher"))
  }

  override lazy val router: Router = new Routes(
    httpErrorHandler,
    new Application(controllerComponents, topicName)(actorSystem, materializer),
    assets
  )
}
