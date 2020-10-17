package kamon.armeria.instrumentation.converters

import com.linecorp.armeria.common.HttpRequest
import com.linecorp.armeria.common.logging.RequestLog
import kamon.context.HttpPropagation.HeaderWriter
import kamon.instrumentation.http.HttpMessage

import scala.collection.JavaConverters.iterableAsScalaIterableConverter
import scala.collection.immutable.Map
import scala.collection.{JavaConverters, mutable}

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

  def getRequestBuilder(request: HttpRequest): HttpMessage.RequestBuilder[HttpRequest] = new HttpMessage.RequestBuilder[HttpRequest]() {
    private val _headers = mutable.Map[String, String]()

    override def read(header: String): Option[String] = Option(request.headers().get(header))

    override def readAll: Map[String, String] = {
      JavaConverters
        .asScalaIteratorConverter(request.headers().iterator())
        .asScala
        .map(entry => (entry.getKey.toString, entry.getValue))
        .toMap

    }

    override def url: String = request.uri().toString

    override def path: String = request.uri().getPath

    override def method: String = request.method().name()

    override def host: String = request.uri().getHost

    override def port: Int = request.uri().getPort

    override def write(header: String, value: String): Unit = {
      _headers += (header -> value)
    }

    override def build: HttpRequest = {
      val newHeadersMap = request.headers.toBuilder
      _headers.foreach { case (key, value) => newHeadersMap.add(key, value) }
      request.withHeaders(newHeadersMap)
    }
  }

  def toKamonResponse(reqLog: RequestLog): HttpMessage.Response = new HttpMessage.Response() {
    override def statusCode: Int = reqLog.responseHeaders().status().code()
  }

  trait HeaderHandler extends HeaderWriter {
    private val _headers = mutable.Map[String, String]()

    override def write(header: String, value: String): Unit = {
      _headers += (header -> value)
    }

    def headers: mutable.Map[String, String] = _headers
  }
}
