package wiring

import actors.{AcquisitionPublisher, KinesisActor, RankIndexActor}
import play.api.ApplicationLoader.Context
import play.api.{BuiltInComponentsFromContext, NoHttpFiltersComponents}
import play.api.routing.Router
import router.Routes
import controllers._

class AppComponents(context: Context) extends BuiltInComponentsFromContext(context) with NoHttpFiltersComponents with AssetsComponents {

  val acquisitionTopicName = "acquisition-topic"
  val rankIndexTopicName = "rank-index-topic"
  val streamName = context.initialConfiguration.get[String]("kinesis.streamName")

  val kinesisActor = {
    val stage = context.initialConfiguration.get[String]("stage")
    val acquisitionPublisher = actorSystem.actorOf(AcquisitionPublisher.props(acquisitionTopicName), "acquisitionPublisher")

    actorSystem
      .actorOf(KinesisActor.props(acquisitionPublisher, streamName, stage)
      .withDispatcher("kinesis-dispatcher"))
  }

  val rankIndexActor = {
    val capiKey = context.initialConfiguration.get[String]("capi.key")
    val acquisitionDataApiUrl = context.initialConfiguration.get[String]("acquisitionDataApiUrl")
    actorSystem.actorOf(RankIndexActor.props(rankIndexTopicName, capiKey, acquisitionDataApiUrl))
  }

  override lazy val router: Router = new Routes(
    httpErrorHandler,
    new Application(controllerComponents, acquisitionTopicName, rankIndexTopicName, rankIndexActor)(actorSystem, materializer),
    assets
  )
}
