import java.net.InetSocketAddress
import java.util.concurrent.Executors

import client.SimpleHttpClient
import com.linecorp.armeria.client.{ClientFactory, Clients, WebClient}
import com.linecorp.armeria.common._
import com.linecorp.armeria.server.Server
import com.linecorp.armeria.server.annotation.{Get, Param}
import com.linecorp.armeria.server.healthcheck.HealthCheckService
import kamon.Kamon

import scala.concurrent.{ExecutionContext, Future}

object Main extends App {

  Kamon.loadModules()

  val sb = Server
    .builder()
    .service("/health-check", HealthCheckService.of())
    .annotatedService().build(TestRoutesSupport())


  sb
    .http(InetSocketAddress.createUnresolved("localhost", 8000))
    .https(InetSocketAddress.createUnresolved("localhost", 8081))
    .https(InetSocketAddress.createUnresolved("localhost", 8291))
    .tlsSelfSigned()

  val server = sb.build()
  server
    .start()
    .join()

  val clientFactory = ClientFactory.builder().tlsNoVerifyHosts("localhost", "127.0.0.1").build()
  val webClientBuilder = Clients.builder(s"https://127.0.0.1:8081").build(classOf[WebClient])
  val webClient = clientFactory.newClient(webClientBuilder).asInstanceOf[WebClient]
  val request = HttpRequest.of(RequestHeaders.of(HttpMethod.GET, "/dummy"))

  //  var i = 1
  //  while (i <= 1) {
  //    webClient.execute(request)
  //    i = i + 1
  //  }

  server

}

final class TestRoutesSupport() {
  implicit val ec = ExecutionContext.fromExecutor(Executors.newCachedThreadPool())

  @Get("/dummy")
  def getDummy(): HttpResponse = {
    Future {
      val t = SimpleHttpClient.a.execute(com.linecorp.armeria.common.HttpRequest.of(RequestHeaders.of(HttpMethod.GET, "topics"))).aggregate().get()
      println(t.status())
    }
    println("Dasdasdasdasdasdasda")
    HttpResponse.of(HttpStatus.ACCEPTED)
  }

  @Get("/dummy-resources/{resource}/other-resources/{other}")
  def getResource(@Param("resource") resource: String, @Param("other") other: String): HttpResponse = {
    println(s"Received a request to retrieve resource $resource and $other")
    SimpleHttpClient.b.execute(com.linecorp.armeria.common.HttpRequest.of(RequestHeaders.of(HttpMethod.POST, "topics")))
  }

  @Get("/nf")
  def nf(): HttpResponse = {
    HttpResponse.of(HttpStatus.NOT_FOUND)
  }

  @Get("/dummy-error")
  def getDummyError(): HttpResponse = SimpleHttpClient.c.execute(com.linecorp.armeria.common.HttpRequest.of(RequestHeaders.of(HttpMethod.PUT, "topics")))
}

object TestRoutesSupport {

  def apply(): TestRoutesSupport = new TestRoutesSupport()
}
