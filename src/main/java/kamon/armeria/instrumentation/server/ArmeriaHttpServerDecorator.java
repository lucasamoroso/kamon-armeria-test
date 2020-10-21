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

package kamon.armeria.instrumentation.server;

import com.linecorp.armeria.common.HttpRequest;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.common.HttpStatus;
import com.linecorp.armeria.server.HttpService;
import com.linecorp.armeria.server.ServiceRequestContext;
import com.linecorp.armeria.server.SimpleDecoratingHttpService;
import com.typesafe.config.Config;
import io.netty.util.AttributeKey;
import kamon.Kamon;
import kamon.armeria.instrumentation.converters.KamonArmeriaMessageConverter;
import kamon.context.Storage;
import kamon.instrumentation.http.HttpServerInstrumentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ArmeriaHttpServerDecorator extends SimpleDecoratingHttpService {
  private static final String FALLBACK_SERVICE_NAME = "com.linecorp.armeria.server.FallbackService";
  private static final AttributeKey<Storage.Scope> TRACE_SCOPE_KEY = AttributeKey.valueOf(Storage.Scope.class, "TRACE_SCOPE");

  private final HttpServerInstrumentation httpServerInstrumentation;
  private final String serverHost;

  public ArmeriaHttpServerDecorator(HttpService delegate, Config httpServerConfig, String serverHost, Integer serverPort) {
    super(delegate);
    this.serverHost = serverHost;
    this.httpServerInstrumentation = HttpServerInstrumentation.from(httpServerConfig, "armeria-http-server", serverHost, serverPort);
  }

  @Override
  public HttpResponse serve(ServiceRequestContext ctx, HttpRequest req) throws Exception {

    final Storage.Scope scope = Kamon.storeContext(Kamon.currentContext());
    ctx.setAttr(TRACE_SCOPE_KEY, scope);

    final HttpServerInstrumentation.RequestHandler requestHandler =
            httpServerInstrumentation.createHandler(KamonArmeriaMessageConverter.toRequest(req, serverHost, httpServerInstrumentation.port()));

    Logger logger = LoggerFactory.getLogger("serverDecorator");
    logger.info("generated context " + Kamon.currentContext().hashCode());
    logger.info("generated req span " + requestHandler.span().id());

    ctx.log()
            .whenComplete()
            .thenAccept(log -> {
              try (Storage.Scope requestCtxScope = ctx.attr(TRACE_SCOPE_KEY)) {
                logger.info("processing context " + Kamon.currentContext().hashCode());
                logger.info("processing req span " + requestHandler.span().id());
                if (HttpStatus.NOT_FOUND.equals(log.responseHeaders().status()) && FALLBACK_SERVICE_NAME.equals(log.serviceName())) {
                  requestHandler.span().name(httpServerInstrumentation.settings().unhandledOperationName());
                }
                requestHandler.buildResponse(KamonArmeriaMessageConverter.toResponse(log), requestCtxScope.context());
                requestHandler.responseSent();
              }
            });

    try (Storage.Scope ignored = Kamon.storeContext(scope.context())) {
      logger.info("unwraping context " + Kamon.currentContext().hashCode());
      logger.info("unwraping req span " + requestHandler.span().id());
      return unwrap().serve(ctx, req);
    }
  }
}


