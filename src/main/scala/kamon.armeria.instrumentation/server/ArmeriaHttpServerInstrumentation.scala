package kamon.armeria.instrumentation.server

import com.linecorp.armeria.server.{ServerBuilder, ServerPort}
import kamon.Kamon
import kamon.armeria.instrumentation.converters.JavaConverters
import kamon.armeria.instrumentation.server.InternalState.ServerBuilderInternalState
import kanela.agent.api.instrumentation.InstrumentationBuilder
import kanela.agent.api.instrumentation.bridge.FieldBridge
import kanela.agent.libs.net.bytebuddy.asm.Advice

import scala.collection.JavaConverters.collectionAsScalaIterableConverter

class ArmeriaHttpServerInstrumentation extends InstrumentationBuilder {
  onType("com.linecorp.armeria.server.ServerBuilder")
    .advise(method("build"), classOf[ArmeriaServerBuilderAdvisor])
    .bridge(classOf[ServerBuilderInternalState])
}

class ArmeriaServerBuilderAdvisor

object ArmeriaServerBuilderAdvisor extends JavaConverters {
  lazy val httpServerConfig = Kamon.config().getConfig("kamon.instrumentation.armeria.http-server")

  @Advice.OnMethodEnter
  def addKamonDecorator(@Advice.This builder: ServerBuilder): Unit = {
    builder.asInstanceOf[ServerBuilderInternalState].getServerPorts.asScala.filter(_.hasHttp).foreach(serverPort => {
      builder.decorator(delegate => new ArmeriaHttpServerDecorator(delegate, httpServerConfig,
        serverPort.localAddress().getHostName, serverPort.localAddress().getPort))
    })
  }
}

object InternalState {

  trait ServerBuilderInternalState {
    @FieldBridge(value = "ports")
    def getServerPorts: java.util.List[ServerPort]
  }

}







