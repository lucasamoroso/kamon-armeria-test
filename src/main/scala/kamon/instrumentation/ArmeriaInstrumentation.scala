package kamon.instrumentation

import com.linecorp.armeria.server.ServerBuilder
import kamon.instrumentation.http.HttpServerInstrumentation
import kamon.{Kamon, KamonService}
import kanela.agent.api.instrumentation.InstrumentationBuilder
import kanela.agent.libs.net.bytebuddy.asm.Advice

class ArmeriaInstrumentation extends InstrumentationBuilder {
  onType("com.linecorp.armeria.server.ServerBuilder")
    .advise(isConstructor, classOf[ArmeriaServerBuilderAdvisor])
}

class ArmeriaServerBuilderAdvisor

object ArmeriaServerBuilderAdvisor {
  lazy val httpServerConfig = Kamon.config().getConfig("kamon.instrumentation.armeria.http-server")
  lazy val serverInstrumentation = HttpServerInstrumentation.from(httpServerConfig, "armeria-http-server", "interface", 1234)

  @Advice.OnMethodExit(suppress = classOf[Throwable])
  def addKamonDecorator(@Advice.This builder: ServerBuilder): Unit = {
    builder.decorator(delegate => new KamonService(delegate, serverInstrumentation))
  }
}
