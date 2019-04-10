package actors

import actors.RankIndexActor._
import RawAcquisitionData._
import CapiData.capiDataDecoder
import actors.WebSocketActor.RankIndexMessage
import akka.actor.{Actor, Props}
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.Publish
import com.typesafe.scalalogging.StrictLogging
import com.softwaremill.sttp.circe._
import com.softwaremill.sttp._
import com.softwaremill.sttp.akkahttp.AkkaHttpBackend
import io.circe.Decoder
import models.RankIndexData

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

case class CapiData(headline: String)

object CapiData {
  implicit val capiDataDecoder: Decoder[CapiData] = _.downField("response")
    .downField("content")
    .downField("fields")
    .get[String]("headline")
    .map(headline => CapiData(headline))
}

case class RawAcquisitionData(url: String, annualisedValueInGBP: Double, acquisitions: Int, pageviews: Long, headline: Option[String])

object RawAcquisitionData {
  def toRankIndexData(rawAcquisitionData: RawAcquisitionData) = RankIndexData(
    url = rawAcquisitionData.url,
    annualisedValueInGBP = rawAcquisitionData.annualisedValueInGBP,
    acquisitions = rawAcquisitionData.acquisitions,
    pageviews = rawAcquisitionData.pageviews,
    headline = rawAcquisitionData.headline,
    annualisedValueInGBPPer1000 = rawAcquisitionData.annualisedValueInGBP / (rawAcquisitionData.pageviews / 1000),
    acquisitionsPer1000 = rawAcquisitionData.acquisitions / (rawAcquisitionData.pageviews / 1000)
  )

  import io.circe.generic.auto._

  implicit val acquisitionDataDecoder = Decoder[RawAcquisitionData]
}

object RankIndexActor {
  def props(topicName: String, capiKey: String) = Props(new RankIndexActor(topicName, capiKey))

  val acquisitionDataApiUrl = Uri {
    new java.net.URI("https://what-if-streaming-was-a-reality.ophan.co.uk/contributions")
  }
  val capiHostUrl = (path: String, capiKey: String) => Uri {
    new java.net.URI(s"https://content.guardianapis.com/$path?show-fields=headline&api-key=$capiKey")
  }

  sealed trait Message
  case object RefreshRankIndexData extends Message
  case class GetHeadlines(acquisitionData: List[RawAcquisitionData]) extends Message
  case class SetRankIndexData(acquisitionData: List[RawAcquisitionData]) extends Message
  case object RequestRankIndexData
}

class RankIndexActor(topicName: String, capiKey: String) extends Actor with StrictLogging {

  private implicit val sttpHandler = AkkaHttpBackend.usingActorSystem(context.system)

  private var rankIndexData: List[RankIndexData] = Nil

  private val mediator = DistributedPubSub(context.system).mediator

  context.system.scheduler.schedule(1.second, 5.minutes, self, RefreshRankIndexData)(context.dispatcher)

  def receive = {
    case RefreshRankIndexData =>
      sttp
        .get(acquisitionDataApiUrl)
        .response(asJson[List[RawAcquisitionData]])
        .send
        .foreach { response =>
          for {
            body <- response.body
            acquisitionData <- body
          } yield {
            self ! GetHeadlines(acquisitionData)
          }
        }

    case GetHeadlines(newAcquisitionData) =>
      Future.sequence {
        newAcquisitionData
          .filter(item => item.url.contains("theguardian.com"))
          .map { item =>
            val path = new java.net.URI(item.url).getPath

            sttp
              .get(capiHostUrl(path, capiKey))
              .response(asJson[CapiData])
              .send
              .map { response =>
                for {
                  body <- response.body
                  capiData <- body
                } yield capiData.headline
              }
              .map {
                case Left(_) => item.copy(headline = Some(path))  //Use the path - this happens for e.g. tag pages
                case Right(headline) => item.copy(headline = Some(headline))
              }
          }
      } foreach { acquisitionData: List[RawAcquisitionData] =>
        self ! SetRankIndexData(acquisitionData)
      }

    case SetRankIndexData(latest) =>
      rankIndexData = latest.map(toRankIndexData)
      mediator ! Publish(topicName, RankIndexMessage(rankIndexData))

    case RequestRankIndexData =>
      sender() ! RankIndexMessage(rankIndexData)
  }
}
