/* =========================================================================================
 * Copyright © 2013-2020 the kamon project <http://kamon.io/>
 *
 * Licensed under the Apache License, Version 2.0 (the "License") you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 * =========================================================================================
 */

package kamon.armeria.instrumentation.client;

import com.linecorp.armeria.client.ClientRequestContext;
import com.linecorp.armeria.client.HttpClient;
import com.linecorp.armeria.client.SimpleDecoratingHttpClient;
import com.linecorp.armeria.common.HttpRequest;
import com.linecorp.armeria.common.HttpResponse;
import com.typesafe.config.Config;
import io.netty.util.AttributeKey;
import kamon.Kamon;
import kamon.armeria.instrumentation.client.timing.Timing;
import kamon.armeria.instrumentation.converters.KamonArmeriaMessageConverter;
import kamon.context.Storage;
import kamon.instrumentation.http.HttpClientInstrumentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ArmeriaHttpClientDecorator extends SimpleDecoratingHttpClient {
  private static final AttributeKey<Storage.Scope> TRACE_SCOPE_KEY = AttributeKey.valueOf(Storage.Scope.class, "TRACE_SCOPE");

  private final HttpClientInstrumentation clientInstrumentation;

  protected ArmeriaHttpClientDecorator(HttpClient delegate, Config httpClientConfig) {
    super(delegate);
    this.clientInstrumentation = HttpClientInstrumentation.from(httpClientConfig, "armeria-http-client");
  }

  @Override
  public HttpResponse execute(ClientRequestContext ctx, HttpRequest req) throws Exception {
    Logger logger = LoggerFactory.getLogger("clientDecorator");

    final Storage.Scope scope = Kamon.storeContext(Kamon.currentContext());
    ctx.setAttr(TRACE_SCOPE_KEY, scope);

    final HttpClientInstrumentation.RequestHandler<HttpRequest> requestHandler =
            clientInstrumentation.createHandler(KamonArmeriaMessageConverter.getRequestBuilder(req), Kamon.currentContext());


    logger.info("generated context " + Kamon.currentContext().hashCode());
    logger.info("generated req span " + requestHandler.span().id());


    ctx.log()
            .whenComplete()
            .thenAccept(log -> {
              try (Storage.Scope ignored = Kamon.storeContext(ctx.attr(TRACE_SCOPE_KEY).context())) {
                logger.info("processing context " + Kamon.currentContext().hashCode());
                logger.info("processing req span " + requestHandler.span().id());
                Timing.takeTimings(log, requestHandler.span());
                requestHandler.processResponse(KamonArmeriaMessageConverter.toKamonResponse(log));
              }
            });

    try (Storage.Scope ignored = Kamon.storeContext(ctx.attr(TRACE_SCOPE_KEY).context())) {
      logger.info("unwraping context " + Kamon.currentContext().hashCode());
      logger.info("unwraping req span " + requestHandler.span().id());
      return unwrap().execute(ctx, requestHandler.request());
    } catch (Exception exception) {
      logger.info("failed context " + Kamon.currentContext().hashCode());
      logger.info("failed req span " + requestHandler.span().id());
      requestHandler.span().fail(exception.getMessage(), exception).finish();
      throw exception;
    }
  }
}
