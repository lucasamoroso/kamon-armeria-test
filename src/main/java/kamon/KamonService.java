package kamon;


import com.linecorp.armeria.common.HttpRequest;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.server.HttpService;
import com.linecorp.armeria.server.ServiceRequestContext;
import com.linecorp.armeria.server.SimpleDecoratingHttpService;
import io.netty.util.AttributeKey;
import kamon.context.Context;
import kamon.instrumentation.http.HttpServerInstrumentation;

public class KamonService extends SimpleDecoratingHttpService {

  private HttpServerInstrumentation httpServerInstrumentation;

  private static final AttributeKey<Context> TRACE_CONTEXT_KEY =
          AttributeKey.valueOf(Context.class, "TRACE_CONTEXT");

  public KamonService(HttpService delegate, HttpServerInstrumentation httpServerInstrumentation) {
    super(delegate);
    this.httpServerInstrumentation = httpServerInstrumentation;
  }


  @Override
  public HttpResponse serve(ServiceRequestContext ctx, HttpRequest req) throws Exception {

    HttpServerInstrumentation.RequestHandler requestHandler = httpServerInstrumentation.createHandler(KamonArmeriaMessageConverter.toRequest(req, "", httpServerInstrumentation.port()));
    ctx.setAttr(TRACE_CONTEXT_KEY, requestHandler.context());

    ctx.log()
            .whenComplete()
            .thenAccept(log -> {
              Context kamonCtx = ctx.attr(TRACE_CONTEXT_KEY);
              requestHandler.buildResponse(KamonArmeriaMessageConverter.toResponse(log), kamonCtx);
              requestHandler.responseSent();
              //TODO close scope
            });
    return unwrap().serve(ctx, req);
  }


}


