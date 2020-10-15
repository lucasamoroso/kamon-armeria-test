package kamon.instrumentation

import com.linecorp.armeria.server.ServerBuilder
import kamon.KamonService
import kanela.agent.api.instrumentation.InstrumentationBuilder
import kanela.agent.libs.net.bytebuddy.asm.Advice

class ArmeriaInstrumentation extends InstrumentationBuilder {
  onType("com.linecorp.armeria.server.ServerBuilder")
    .advise(isConstructor, classOf[ArmeriaServerBuilderAdvisor])
}

class ArmeriaServerBuilderAdvisor

object ArmeriaServerBuilderAdvisor {
  @Advice.OnMethodExit(suppress = classOf[Throwable])
  def addKamonDecorator(@Advice.This builder: ServerBuilder): Unit = {
    builder.decorator(decorate => new KamonService(decorate))
  }
}