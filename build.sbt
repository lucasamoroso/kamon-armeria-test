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
      "com.linecorp.armeria" % "armeria-dropwizard2" % "1.0.0",
      "com.linecorp.armeria" % "armeria-spring-boot2-starter" % "1.0.0",
      "ch.qos.logback" % "logback-classic" % "1.2.3",
      "io.kamon" %% "kamon-bundle" % "2.0.6",
      "io.kamon" %% "kamon-prometheus" % kamonV,
      "io.kamon" %% "kamon-zipkin" % kamonV,
      "io.dropwizard" % "dropwizard-core" % "2.0.13"
    ),
    javacOptions ++= Seq("-parameters"),
    javaAgents += "io.kamon" % "kanela-agent" % "1.0.5" % "compile;runtime;test"
  )
