package kamon.armeria.instrumentation.server

import com.linecorp.armeria.server.ServerBuilder
import kamon.Kamon
import kamon.instrumentation.http.HttpServerInstrumentation
import kanela.agent.api.instrumentation.InstrumentationBuilder
import kanela.agent.libs.net.bytebuddy.asm.Advice

class ArmeriaHttpServerInstrumentation extends InstrumentationBuilder {
  onType("com.linecorp.armeria.server.ServerBuilder")
    .advise(isConstructor, classOf[ArmeriaServerBuilderAdvisor])
}

class ArmeriaServerBuilderAdvisor

object ArmeriaServerBuilderAdvisor {
  lazy val httpServerConfig = Kamon.config().getConfig("kamon.instrumentation.armeria.http-server")

  @Advice.OnMethodExit
  def addKamonDecorator(@Advice.This builder: ServerBuilder): Unit = {
    //TODO que pasa si usan https? grpc?
    //obtener intercafe y puerto desde configuracion
    val serverInstrumentation = HttpServerInstrumentation.from(httpServerConfig, "armeria-http-server", "localhost", 8081)
    builder.decorator(delegate => new ArmeriaHttpServerDecorator(delegate, serverInstrumentation))
  }
}
