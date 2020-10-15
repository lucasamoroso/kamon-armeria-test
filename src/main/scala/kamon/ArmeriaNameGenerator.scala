package kamon

import java.util.Locale

import kamon.instrumentation.http.HttpMessage.Request
/**
 * A GET request to https://localhost:8080/kamon-io/Kamon will generate the following operationName
 * kamon-io.Kamon.get
 */
class ArmeriaNameGenerator extends BaseKamonArmeriaOperationNameGenerator {

  override protected def name(request: Request, normalisedPath: String): String =
    s"$normalisedPath${request.method.toLowerCase(Locale.ENGLISH)}"

  override protected def key(request: Request): String =
    s"${request.method}${request.path}"
}

object KamonArmeriaOperationNameGenerator {
  def apply() = new ArmeriaNameGenerator()
}


