import com.lightbend.sbt.javaagent.JavaAgent.JavaAgentKeys.javaAgents
import sbt.Keys.version

val kamonV = "2.1.4"

lazy val `kamon-armeria-test` = (project in file("."))
  .enablePlugins(JavaAgent)
  .settings(
    name := "kamon-armeria-test",
    version := "0.1",
    scalaVersion := "2.12.12",
    libraryDependencies ++= Seq(

      "com.linecorp.armeria" % "armeria" % "1.1.0",
      "com.linecorp.armeria" % "armeria-grpc" % "1.1.0",
      "com.linecorp.armeria" % "armeria-dropwizard2" % "1.0.0",
      "com.linecorp.armeria" % "armeria-spring-boot2-starter" % "1.0.0",
      "io.dropwizard" % "dropwizard-core" % "2.0.13",

      "ch.qos.logback" % "logback-classic" % "1.2.3",

      "io.kamon" %% "kamon-bundle" % "2.0.6",
      "io.kamon" %% "kamon-prometheus" % kamonV,
      "io.kamon" %% "kamon-zipkin" % kamonV,


      "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalapb.compiler.Version.scalapbVersion,
      "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf",
      "com.thesamet.scalapb" %% "scalapb-json4s" % "0.10.1",

      "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.11.3",

      "io.monix" %% "monix-reactive" % "3.2.2+43-36c1ada8"
    ),
    javacOptions ++= Seq("-parameters"),
    javaAgents += "io.kamon" % "kanela-agent" % "1.0.5" % "compile;runtime;test",

    PB.targets in Compile := Seq(scalapb.gen() -> (sourceManaged in Compile).value)

  )

