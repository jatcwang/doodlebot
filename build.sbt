name         in ThisBuild := "doodlebot"
version      in ThisBuild := "0.0.1"
organization in ThisBuild := "underscoreio"
scalaVersion in ThisBuild := "2.12.8"

scalacOptions in ThisBuild ++= Seq("-feature", "-deprecation", "-unchecked", "-Ywarn-unused-import", "-Ypartial-unification")


lazy val server = project.
  settings(
    scalacOptions in (Compile, console) := Seq("-feature", "-Xfatal-warnings", "-deprecation", "-unchecked"),

    licenses += ("Apache-2.0", url("http://apache.org/licenses/LICENSE-2.0")),

    resolvers += Resolver.sonatypeRepo("snapshots"),

    libraryDependencies ++= Seq(
      "com.github.finagle" %% "finch-core" % "0.28.0",
      "com.github.finagle" %% "finch-circe" % "0.28.0",
      "io.circe" %% "circe-core" % "0.11.1",
      "io.circe" %% "circe-generic" % "0.11.1",
      "io.circe" %% "circe-parser" % "0.11.1",
      "org.scalatest" %% "scalatest" % "3.0.6" % "test",
      "org.scalacheck" %% "scalacheck" % "1.14.0" % "test"
    ),

    /* resourceGenerators in Compile += Def.task { */
    /*   val code = (fastOptJS in Compile in ui).value.data */
    /*   val sourceMap = code.getParentFile / (code.getName + ".map") */
    /*   val launcher = (scalaJSUseMainModuleInitializer in Compile in ui).value.data */
    /*   val dependencies = (packageJSDependencies in Compile in ui).value */
    /*   Seq(code, sourceMap, launcher, dependencies) */
    /* }.taskValue, */

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
      "com.lihaoyi" %%% "scalatags" % "0.6.7",
      "be.doeraene" %%% "scalajs-jquery" % "0.9.4"
    ),
    jsDependencies ++= Seq(
      "org.webjars" % "jquery" % "2.1.3" / "2.1.3/jquery.js",
      ProvidedJS / "virtual-dom.js"
    ),
    scalaJSUseMainModuleInitializer := true,
    skip in packageJSDependencies := false
  )
