package actors

import java.util.UUID
import scala.collection.JavaConverters._

import akka.actor._
import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.{IRecordProcessor, IRecordProcessorFactory}
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.{KinesisClientLibConfiguration, Worker}
import com.amazonaws.services.kinesis.clientlibrary.types.{InitializationInput, ProcessRecordsInput, ShutdownInput}
import com.amazonaws.services.kinesis.metrics.impl.NullMetricsFactory
import com.gu.thrift.serializer.ThriftDeserializer
import com.typesafe.scalalogging.StrictLogging
import ophan.thrift.event.Acquisition
import services.Aws

object KinesisActor {
  def props(acquisitionPublisher: ActorRef, streamName: String, stage: String) =
    Props(new KinesisActor(acquisitionPublisher, streamName, stage))
}

class KinesisActor(acquisitionPublisher: ActorRef, streamName: String, stage: String) extends Actor with StrictLogging {

  private val worker = {
    val workerId = UUID.randomUUID().toString

    val config = new KinesisClientLibConfiguration(
      s"contributions-stream-$stage",
      streamName,
      Aws.credentialsProvider,
      Aws.credentialsProvider,
      null,
      workerId
    )
      .withRegionName(Aws.region.getName)
      .withInitialLeaseTableReadCapacity(1)
      .withInitialLeaseTableWriteCapacity(1)

    val processorFactory = new IRecordProcessorFactory {
      override def createProcessor(): IRecordProcessor = new EventProcessor()
    }

    new Worker.Builder()
      .recordProcessorFactory(processorFactory)
      .config(config)
      .metricsFactory(new NullMetricsFactory())
      .build
  }

  private class EventProcessor extends IRecordProcessor {
    override def initialize(initializationInput: InitializationInput): Unit = {
      println(s"Kinesis processor ready")
      logger.info(s"Kinesis processor ready")
    }

    override def processRecords(records: ProcessRecordsInput): Unit = {
      logger.info(s"Got ${records.getRecords.asScala.length} records")

      records.getRecords.asScala.map { record =>

        ThriftDeserializer.deserialize(record.getData.array)(Acquisition).fold(
          (e: Throwable) => {
            println(s"Error deserializing record: ${e.getMessage}")
            None
          },
          (acquisition: Acquisition) => {
            println(acquisition)
            acquisitionPublisher ! acquisition
          }
        )
      }
    }

    override def shutdown(shutdownInput: ShutdownInput): Unit = {
      logger.info("Kinesis processor shutdown")
    }
  }

  def receive = {
    case _ => //doesn't receive messages
  }

  context.dispatcher.execute(worker)
}
