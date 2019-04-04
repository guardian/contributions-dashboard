name := "contributions-stream-ws"

version := "0.1"

scalaVersion := "2.12.8"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

val circeVersion = "0.10.0"

libraryDependencies ++= Seq(
  "com.fasterxml.jackson.dataformat" % "jackson-dataformat-cbor" % "2.9.8",
  "com.amazonaws" % "amazon-kinesis-client" % "1.9.3",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.7.2",
  "com.gu" %% "thrift-serializer" % "3.0.0",
  "com.gu" %% "ophan-event-model" % "0.0.6",
  "com.typesafe.akka" %% "akka-cluster-tools" % "2.5.21",
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
  "com.gu" %% "fezziwig" % "0.10"
)
