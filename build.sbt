import sbt._

name := "OdinsonWebapp"
organization in ThisBuild := "org.clulab"

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
  .settings(
    assemblyMergeStrategy in assembly := {
      case PathList("META-INF", "MANIFEST.MF") => MergeStrategy.discard
      case PathList("META-INF", "*.SF")        => MergeStrategy.discard
      case PathList("META-INF", "*.DSA")       => MergeStrategy.discard
      case PathList("META-INF", "*.RSA")       => MergeStrategy.discard
      case PathList("META-INF", "*.DEF")       => MergeStrategy.discard
      case PathList("*.SF")                    => MergeStrategy.discard
      case PathList("*.DSA")                   => MergeStrategy.discard
      case PathList("*.RSA")                   => MergeStrategy.discard
      case PathList("*.DEF")                   => MergeStrategy.discard
      case PathList("META-INF", "services", "org.apache.lucene.codecs.PostingsFormat" ) => MergeStrategy.filterDistinctLines
      case PathList("META-INF", "services", "com.fasterxml.jackson.databind.Module" ) => MergeStrategy.filterDistinctLines
      case PathList("META-INF", "services", "javax.xml.transform.TransformerFactory" ) => MergeStrategy.first // or last or both?
      case PathList("reference.conf")   => MergeStrategy.concat
      case PathList("application.conf") => MergeStrategy.concat // combine config files
      case PathList("logback.xml")      => MergeStrategy.first
      case _                            => MergeStrategy.last
    },
    test in assembly := {},
    Compile / unmanagedResourceDirectories += baseDirectory.value / "webapp" / "conf"
  )
