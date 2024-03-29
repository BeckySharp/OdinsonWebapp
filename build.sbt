import sbt._
organization in ThisBuild := "org.clulab"
name := "OdinsonWebapp"

scalaVersion in ThisBuild := "2.12.4"

resolvers ++= Seq(
  "Artifactory" at "http://artifactory.cs.arizona.edu:8081/artifactory/sbt-release", // processors-models
  "Local Ivy Repository" at s"file://${System.getProperty("user.home")}/.ivy2/local/default"
)

val procVer = "8.2.3"
val odinsonVer = "0.5.0"
val akkaVersion = "2.6.8"
val akkaHttpVersion = "10.2.4"

libraryDependencies ++= Seq(
  "org.clulab" %% "processors-main" % procVer,
  "ai.lum" %% "odinson-core" % odinsonVer,
  "ai.lum" %% "common" % "0.0.10",
  "com.lihaoyi" %% "ujson" % "0.7.1",
  "com.lihaoyi" %% "upickle" % "0.7.1",
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-slf4j"  % akkaVersion,
  guice
)

lazy val core = (project in file(".")).disablePlugins(PlayScala, JavaAppPackaging, SbtNativePackager)

lazy val webapp = project
  .enablePlugins(PlayScala, JavaAppPackaging, SbtNativePackager)
  .aggregate(core)
  .dependsOn(core)
