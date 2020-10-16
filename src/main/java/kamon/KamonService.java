package kamon;


import com.linecorp.armeria.common.HttpRequest;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.server.HttpService;
import com.linecorp.armeria.server.ServiceRequestContext;
import com.linecorp.armeria.server.SimpleDecoratingHttpService;
import kamon.context.Storage;
import kamon.instrumentation.http.HttpServerInstrumentation;
import kamon.utils.JEither;

public class KamonService extends SimpleDecoratingHttpService {

  private HttpServerInstrumentation httpServerInstrumentation;

  public KamonService(HttpService delegate, HttpServerInstrumentation httpServerInstrumentation) {
    super(delegate);
    this.httpServerInstrumentation = httpServerInstrumentation;
  }


  @Override
  public HttpResponse serve(ServiceRequestContext ctx, HttpRequest req) throws Exception {

    HttpServerInstrumentation.RequestHandler requestHandler = httpServerInstrumentation.createHandler(KamonArmeriaMessageConverter.toRequest(req, "", httpServerInstrumentation.port()));
    Storage.Scope scope = Kamon.storeContext(requestHandler.context());

    ctx.log()
            .whenComplete()
            .thenAccept(log -> {
              requestHandler.buildResponse(KamonArmeriaMessageConverter.toResponse(log), scope.context());
              requestHandler.responseSent();
              scope.close();
            });

    JEither<Exception, HttpResponse> result = Kamon.runWithContext(scope.context(), () -> {
      try {
        HttpResponse httpResponse = unwrap().serve(ctx, req);
        return JEither.right(httpResponse);
      } catch (Exception ex) {
        return JEither.left(ex);
      }
    });

    if (result.left() != null) {
      throw result.left();
    }

    return result.right();
  }


}


