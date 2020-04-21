name := "OdinsonWebapp"
organization := "org.clulab"

val procVer = "8.0.2"

libraryDependencies ++= Seq(
  "org.clulab"    %% "processors-main"          % procVer,
  "org.clulab"    %% "processors-corenlp"       % procVer,
  "ai.lum"        %% "odinson-core"             % "0.2.3",
  "ai.lum"        %% "common"                   % "0.0.10",
  "com.lihaoyi"   %% "ujson"                    % "0.7.1",
  "com.lihaoyi"   %% "upickle"                  % "0.7.1",
)

lazy val core = project in file(".")

lazy val webapp = project
  .enablePlugins(PlayScala)
  .aggregate(core)
  .dependsOn(core)
