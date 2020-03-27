name := "OdinsonWebapp"
organization := "org.clulab"

libraryDependencies ++= Seq(
  "org.clulab" %% "processors-main" % "7.4.4",
  "org.clulab" %% "processors-corenlp" % "7.4.4",
  "org.clulab" %% "processors-modelsmain" % "7.4.4",
  "org.clulab" %% "processors-modelscorenlp" % "7.4.4",
  "ai.lum" %% "odinson-core" % "0.2.3-SNAPSHOT",
  "ai.lum" %% "common" % "0.0.10",
)

lazy val core = project in file(".")

lazy val webapp = project
  .enablePlugins(PlayScala)
  .aggregate(core)
  .dependsOn(core)
