name := "contributions-dashboard"

version := "0.1"

scalaVersion := "2.12.8"

val circeVersion = "0.10.0"
val sttpVersion = "1.5.12"

libraryDependencies ++= Seq(
  "com.fasterxml.jackson.dataformat" % "jackson-dataformat-cbor" % "2.9.8",
  "com.amazonaws" % "amazon-kinesis-client" % "1.9.3",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.7.2",
  "com.gu" %% "thrift-serializer" % "3.0.0",
  "com.gu" %% "ophan-event-model" % "0.0.6",
  "com.typesafe.akka" %% "akka-cluster-tools" % "2.5.19",
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
  "com.gu" %% "fezziwig" % "0.10",
  "com.softwaremill.sttp" %% "core" % sttpVersion,
  "com.softwaremill.sttp" %% "circe" % sttpVersion,
  "com.softwaremill.sttp" %% "akka-http-backend" % sttpVersion
)

sources in(Compile, doc) := Seq.empty

publishArtifact in(Compile, packageDoc) := false

enablePlugins(PlayScala, RiffRaffArtifact, JDebPackaging, SystemdPlugin)

riffRaffPackageType := (packageBin in Debian).value
riffRaffManifestProjectName := "support:contributions-dashboard"
riffRaffPackageName := "contributions-dashboard"
riffRaffUploadArtifactBucket := Option("riffraff-artifact")
riffRaffUploadManifestBucket := Option("riffraff-builds")
riffRaffArtifactResources += (file("cloudformation.yaml"), "cfn/cfn.yaml")
