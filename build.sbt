ThisBuild / scalaVersion     := "3.2.2"
ThisBuild / organization     := "io.github.kitlangton"
ThisBuild / organizationName := "kitlangton"

val zioSchemaVersion = "0.4.9"
val zioVersion       = "2.0.10"
val zioOpenAiVersion = "0.2.0"

lazy val root = (project in file("."))
  .settings(
    name := "automaton"
  )
  .aggregate(core, examples)

lazy val core = (project in file("modules/core"))
  .settings(
    name := "automaton",
    libraryDependencies ++= Seq(
      "dev.zio"     %% "zio"             % zioVersion,
      "dev.zio"     %% "zio-test"        % zioVersion % Test,
      "dev.zio" %% "zio-test-sbt" % zioVersion % Test,
      "dev.zio"     %% "zio-openai"      % zioOpenAiVersion,
      "dev.zio"     %% "zio-schema"      % zioSchemaVersion,
      "dev.zio"     %% "zio-schema-json" % zioSchemaVersion,
      "com.lihaoyi" %% "pprint"          % "0.8.1"
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )

lazy val examples = (project in file("examples"))
  .settings(
    name           := "automaton-examples",
    publish / skip := true,
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.client3" %% "zio"      % "3.8.13",
      "com.softwaremill.sttp.client3" %% "zio-json" % "3.8.13"
    )
  )
  .dependsOn(core)
