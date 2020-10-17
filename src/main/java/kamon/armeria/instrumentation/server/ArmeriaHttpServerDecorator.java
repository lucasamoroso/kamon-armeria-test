package kamon.armeria.instrumentation.server;


import com.linecorp.armeria.common.HttpRequest;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.common.HttpStatus;
import com.linecorp.armeria.server.HttpService;
import com.linecorp.armeria.server.ServiceRequestContext;
import com.linecorp.armeria.server.SimpleDecoratingHttpService;
import io.netty.util.AttributeKey;
import kamon.Kamon;
import kamon.armeria.instrumentation.converters.KamonArmeriaMessageConverter;
import kamon.context.Storage;
import kamon.instrumentation.http.HttpServerInstrumentation;

public class ArmeriaHttpServerDecorator extends SimpleDecoratingHttpService {

  private static final String FALLBACK_SERVICE_NAME = "com.linecorp.armeria.server.FallbackService";
  private static final AttributeKey<Storage.Scope> TRACE_SCOPE_KEY = AttributeKey.valueOf(Storage.Scope.class, "TRACE_SCOPE");

  private final HttpServerInstrumentation httpServerInstrumentation;

  public ArmeriaHttpServerDecorator(HttpService delegate, HttpServerInstrumentation httpServerInstrumentation) {
    super(delegate);
    this.httpServerInstrumentation = httpServerInstrumentation;
  }

  @Override
  public HttpResponse serve(ServiceRequestContext ctx, HttpRequest req) throws Exception {
    final HttpServerInstrumentation.RequestHandler requestHandler = httpServerInstrumentation.createHandler(KamonArmeriaMessageConverter.toRequest(req, "interface", 0));

    final Storage.Scope scope = Kamon.storeContext(requestHandler.context());
    ctx.setAttr(TRACE_SCOPE_KEY, scope);

    ctx.log()
            .whenComplete()
            .thenAccept(log -> {
              try (Storage.Scope requestCtxScope = log.context().attr(TRACE_SCOPE_KEY)) {
                if (HttpStatus.NOT_FOUND.equals(log.responseHeaders().status()) && FALLBACK_SERVICE_NAME.equals(log.serviceName())) {
                  requestHandler.span().name(httpServerInstrumentation.settings().unhandledOperationName());
                }
                requestHandler.buildResponse(KamonArmeriaMessageConverter.toResponse(log), requestCtxScope.context());
                requestHandler.responseSent();
              }
            });

    try (Storage.Scope ignored = Kamon.storeContext(scope.context())) {
      return unwrap().serve(ctx, req);
    }

  }


}


