import java.net.InetSocketAddress
import java.security.{KeyStore, SecureRandom}
import java.util.concurrent.Executors

import client.SimpleHttpClient
import com.linecorp.armeria.common.{HttpMethod, HttpResponse, HttpStatus, RequestHeaders}
import com.linecorp.armeria.server.Server
import com.linecorp.armeria.server.annotation.{Get, Param}
import com.linecorp.armeria.server.healthcheck.HealthCheckService
import javax.net.ssl.{KeyManagerFactory, SSLContext}
import kamon.Kamon

import scala.concurrent.{ExecutionContext, Future}

object Main extends App {

  Kamon.loadModules()

  val sb = Server
    .builder()
    .service("/health-check", HealthCheckService.of())
    .annotatedService().build(TestRoutesSupport())


//  val password = "kamon".toCharArray
//  val ks = KeyStore.getInstance("PKCS12")
//  ks.load(getClass.getClassLoader.getResourceAsStream("https/server.p12"), password)
//
//  val kmf = KeyManagerFactory.getInstance("SunX509")
//  kmf.init(ks, password)
//
//  val context = SSLContext.getInstance("TLS")
//  context.init(kmf.getKeyManagers, null, new SecureRandom)
//
//  sb.https(InetSocketAddress.createUnresolved("localhost", 8082))
//    .tls(kmf)
    sb
    .http(InetSocketAddress.createUnresolved("localhost", 8081))

  val server = sb.build()
  server
    .start()
    .join()

  server

}

final class TestRoutesSupport() {
  implicit val ec = ExecutionContext.fromExecutor(Executors.newCachedThreadPool())
  @Get("/dummy")
  def getDummy(): HttpResponse = {
    Future{
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
