package kamon

import com.linecorp.armeria.common.HttpRequest
import com.linecorp.armeria.common.logging.RequestLog
import kamon.instrumentation.http.HttpMessage

import scala.collection.JavaConverters.iterableAsScalaIterableConverter

object KamonArmeriaMessageConverter {
  def toRequest(request: HttpRequest, serverHost: String, serverPort: Int): HttpMessage.Request = new HttpMessage.Request {

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

  def toResponse(log: RequestLog): HttpMessage.ResponseBuilder[RequestLog] = new HttpMessage.ResponseBuilder[RequestLog] {
    override def build(): RequestLog =
      log

    override def statusCode: Int =
      log.responseHeaders().status().code()

    override def write(header: String, value: String): Unit =
      log.responseHeaders().toBuilder.add(header, value).build()
  }
}
