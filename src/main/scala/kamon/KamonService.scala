package kamon

import java.util.concurrent.Executors

import com.linecorp.armeria.common.logging.RequestLog
import com.linecorp.armeria.common.{HttpRequest, HttpResponse}
import com.linecorp.armeria.server.{HttpService, ServiceRequestContext, SimpleDecoratingHttpService}
import converters.FutureConverters
import kamon.instrumentation.http.{HttpMessage, HttpServerInstrumentation}
import kamon.instrumentation.play.NettyPlayRequestHandlerHandleAdvice.RequestProcessingContext

import scala.collection.JavaConverters.iterableAsScalaIterableConverter
import scala.concurrent.ExecutionContext


class KamonService(httpService: HttpService) extends SimpleDecoratingHttpService(httpService) with FutureConverters {
  private implicit val ec = ExecutionContext.fromExecutor(Executors.newCachedThreadPool())

  lazy val httpServerConfig = Kamon.config().getConfig("kamon.instrumentation.armeria.http-server")
  private val localhost: String = "localhost"
  private val port: Int = 8080
  lazy val serverInstrumentation = HttpServerInstrumentation.from(httpServerConfig, "armeria-http-server", localhost, port)

  override def serve(ctx: ServiceRequestContext, req: HttpRequest): HttpResponse = {
    val serverRequestHandler = serverInstrumentation.createHandler(toRequest(req, serverInstrumentation.interface(), serverInstrumentation.port()))

    val kamonCtx = RequestProcessingContext(serverRequestHandler, Kamon.storeContext(serverRequestHandler.context))
    //TODO: on failure close?
    ctx.log()
      .whenComplete()
      .toScala
      .foreach(log => {
        serverRequestHandler.buildResponse(toResponse(log), kamonCtx.scope.context)
        kamonCtx.requestHandler.responseSent()
        kamonCtx.scope.close()
      })


    httpService.serve(ctx, req)
  }

  private def toRequest(request: HttpRequest, serverHost: String, serverPort: Int): HttpMessage.Request = new HttpMessage.Request {

    override def url: String = request.uri().toString

    override def path: String = request.path()

    override def method: String = request.method().name()

    override def host: String = serverHost

    override def port: Int = serverPort

    override def read(header: String): Option[String] =
      Option(request.headers().get(header))

    override def readAll(): Map[String, String] =
      request.headers().asScala.map(e => e.getKey.toString() -> e.getValue).toMap
  }

  private def toResponse(log: RequestLog): HttpMessage.ResponseBuilder[RequestLog] = new HttpMessage.ResponseBuilder[RequestLog] {
    override def build(): RequestLog =
      log

    override def statusCode: Int =
      log.responseHeaders().status().code()

    override def write(header: String, value: String): Unit =
      log.responseHeaders().toBuilder.add(header, value).build()
  }
}
