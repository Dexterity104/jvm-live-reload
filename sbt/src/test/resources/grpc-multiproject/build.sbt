val GrpcVersion = "1.72.0"

resolvers += Resolver.mavenLocal

ThisBuild / scalaVersion := "2.13.16"

val isSbt2 = settingKey[Boolean]("isSbt2")
ThisBuild / isSbt2 := (sbtBinaryVersion.value match {
  case "2" => true
  case _   => false
})

val proxyPort = settingKey[Int]("proxyPort")
ThisBuild / proxyPort := sys.props.get("testkit.proxyPort").map(_.toInt).getOrElse(if (isSbt2.value) 9001 else 9000)

val port = settingKey[Int]("port")
ThisBuild / port := sys.props.get("testkit.port").map(_.toInt).getOrElse(if (isSbt2.value) 8081 else 8080)

lazy val `project-a` = (project in file("project-a"))
  .enablePlugins(LiveReloadPlugin)
  .enablePlugins(BuildInfoPlugin)
  .settings(
    libraryDependencies ++= Seq(
      "io.grpc" % "grpc-netty-shaded" % GrpcVersion,
      "io.grpc" % "grpc-stub" % GrpcVersion,
      "io.grpc" % "grpc-services" % GrpcVersion
    ),
    liveServerType := me.seroperson.reload.live.sbt.GrpcServerType,
    liveDevSettings := Seq(
      DevSettingsKeys.LiveReloadProxyGrpcPort -> proxyPort.value.toString,
      DevSettingsKeys.LiveReloadGrpcPort -> port.value.toString,
      DevSettingsKeys.LiveReloadIsDebug -> "true"
    ),
    buildInfoKeys := Seq[BuildInfoKey](port),
    buildInfoPackage := "me.seroperson"
  )
  .dependsOn(`project-b`)

lazy val `project-b` = (project in file("project-b"))

lazy val root = (project in file("."))
