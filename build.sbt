name         in ThisBuild := "doodlebot"
version      in ThisBuild := "0.0.1"
organization in ThisBuild := "underscoreio"
scalaVersion in ThisBuild := "2.12.8"

scalacOptions in ThisBuild ++= Seq("-feature", "-deprecation", "-unchecked", "-Ywarn-unused-import", "-Ypartial-unification")

val http4sVersion = "0.18.23"

lazy val server = project.
  settings(
    sharedSettings,
    scalacOptions in (Compile, console) := Seq("-feature", "-Xfatal-warnings", "-deprecation", "-unchecked"),

    licenses += ("Apache-2.0", url("http://apache.org/licenses/LICENSE-2.0")),

    resolvers += Resolver.sonatypeRepo("snapshots"),

    libraryDependencies ++= Seq(
      "io.circe" %% "circe-core" % "0.11.1",
      "io.circe" %% "circe-generic" % "0.11.1",
      "io.circe" %% "circe-parser" % "0.11.1",
      "org.scalatest" %% "scalatest" % "3.0.6" % "test",
      "org.scalacheck" %% "scalacheck" % "1.14.0" % "test",

      "org.http4s" %% "http4s-dsl" % http4sVersion,
      "org.http4s" %% "http4s-blaze-server" % http4sVersion,
      "org.http4s" %% "http4s-blaze-client" % http4sVersion,
      "org.http4s" %% "http4s-circe" % http4sVersion

    ),

    resourceGenerators in Compile += Def.task {
      val code = (ui / Compile / fastOptJS).value.data
      val sourceMap = code.getParentFile / (code.getName + ".map")
      val dependencies = (ui / Compile / packageJSDependencies).value
      Seq(code, sourceMap, dependencies)
    }.taskValue,

    initialCommands in console := """
      |doodlebot.DoodleBot.server
    """.trim.stripMargin,

    cleanupCommands in console := """
      |doodlebot.DoodleBot.server.close()
    """.trim.stripMargin
  )


lazy val ui = project.
  enablePlugins(ScalaJSPlugin).
  settings(
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % "0.9.6",
      "com.lihaoyi" %%% "scalatags" % "0.6.8",
      "be.doeraene" %%% "scalajs-jquery" % "0.9.4"
    ),
    jsDependencies ++= Seq(
      "org.webjars" % "jquery" % "2.1.3" / "2.1.3/jquery.js",
      ProvidedJS / "virtual-dom.js"
    ),
    scalaJSUseMainModuleInitializer := true,
    skip in packageJSDependencies := false,
    sharedSettings
  )

lazy val sharedSettings = Seq(
  addCompilerPlugin("org.spire-math" % "kind-projector" % "0.9.9" cross CrossVersion.binary)
)
