package kamon.armeria.instrumentation.server

import java.util.Locale

import kamon.armeria.instrumentation.BaseArmeriaHttpOperationNameGenerator
import kamon.instrumentation.http.HttpMessage.Request
/**
 * A GET request to https://localhost:8080/kamon-io/Kamon will generate the following operationName
 * kamon-io.Kamon.get
 */
class ArmeriaHttpOperationNameGenerator extends BaseArmeriaHttpOperationNameGenerator {

  override protected def name(request: Request, normalisedPath: String): String =
    s"$normalisedPath${request.method.toLowerCase(Locale.ENGLISH)}"

  override protected def key(request: Request): String =
    s"${request.method}${request.path}"
}

object ArmeriaHttpOperationNameGenerator {
  def apply() = new ArmeriaHttpOperationNameGenerator()
}


