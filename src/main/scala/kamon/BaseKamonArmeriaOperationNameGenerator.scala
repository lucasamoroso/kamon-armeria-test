package kamon

import kamon.instrumentation.http.HttpMessage.Request
import kamon.instrumentation.http.HttpOperationNameGenerator

import scala.collection.concurrent.TrieMap

trait BaseKamonArmeriaOperationNameGenerator extends HttpOperationNameGenerator {

  private val localCache = TrieMap.empty[String, String]
  private val normalizePattern = """\$([^<]+)<[^>]+>""".r

  def name(request: Request): Option[String] =
    Some(
      localCache.getOrElseUpdate(key(request), {
        // Convert paths of form GET /foo/bar/$paramname<regexp>/blah to foo.bar.paramname.blah.get
        val normalisedPath = normalisePath(request.path)
        name(request, normalisedPath)
      })
    )

  protected def name(request: Request, normalisedPath: String): String

  protected def key(request: Request): String

  private def normalisePath(path: String): String = {
    val p = normalizePattern.replaceAllIn(path, "$1").replace('/', '.').dropWhile(_ == '.')
    val normalisedPath = {
      if (p.lastOption.exists(_ != '.')) s"$p."
      else p
    }
    normalisedPath
  }
}
