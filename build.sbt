import sbt._
organization in ThisBuild := "org.clulab"
name := "OdinsonWebapp"

scalaVersion in ThisBuild := "2.12.4"

resolvers ++= Seq(
  "Artifactory" at "http://artifactory.cs.arizona.edu:8081/artifactory/sbt-release", // processors-models
  "Local Ivy Repository" at s"file://${System.getProperty("user.home")}/.ivy2/local/default"
)

val procVer = "8.2.3"
val odinsonVer = "0.3.0-SNAPSHOT"

libraryDependencies ++= Seq(
  "org.clulab" %% "processors-main" % procVer,
  "org.clulab" %% "processors-corenlp" % procVer,
  "ai.lum" %% "odinson-core" % odinsonVer,
  "ai.lum" %% "odinson-extra" % odinsonVer,
  "ai.lum" %% "common" % "0.0.10",
  "com.lihaoyi" %% "ujson" % "0.7.1",
  "com.lihaoyi" %% "upickle" % "0.7.1",
  guice
)

lazy val core = (project in file(".")).disablePlugins(PlayScala, JavaAppPackaging, SbtNativePackager)

lazy val webapp = project
  .enablePlugins(PlayScala, JavaAppPackaging, SbtNativePackager)
  .aggregate(core)
  .dependsOn(core)
