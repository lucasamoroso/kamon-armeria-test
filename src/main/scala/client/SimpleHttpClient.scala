package client

import com.linecorp.armeria.client.{Clients, WebClient}

object SimpleHttpClient {

  val a = Clients.builder("http://localhost:8080/data/stream/in").build(classOf[WebClient])
  val b = Clients.builder("http://localhost:8080/data/stream/assasdasdasda").build(classOf[WebClient])
  val c = Clients.builder("http://localhost:8080/data/stream/as").build(classOf[WebClient])

}
