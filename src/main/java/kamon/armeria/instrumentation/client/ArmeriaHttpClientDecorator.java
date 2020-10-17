package kamon.armeria.instrumentation.client;

import com.linecorp.armeria.client.ClientRequestContext;
import com.linecorp.armeria.client.HttpClient;
import com.linecorp.armeria.client.SimpleDecoratingHttpClient;
import com.linecorp.armeria.common.HttpRequest;
import com.linecorp.armeria.common.HttpResponse;
import kamon.Kamon;
import kamon.armeria.instrumentation.client.timing.Timing;
import kamon.armeria.instrumentation.converters.KamonArmeriaMessageConverter;
import kamon.context.Storage;
import kamon.instrumentation.http.HttpClientInstrumentation;
import kamon.trace.Span;

public class ArmeriaHttpClientDecorator extends SimpleDecoratingHttpClient {
  private final HttpClientInstrumentation clientInstrumentation;

  /**
   * Creates a new instance that decorates the specified {@link HttpClient}.
   *
   * @param delegate
   * @param clientInstrumentation
   */
  protected ArmeriaHttpClientDecorator(HttpClient delegate, HttpClientInstrumentation clientInstrumentation) {
    super(delegate);
    this.clientInstrumentation = clientInstrumentation;
  }

  @Override
  public HttpResponse execute(ClientRequestContext ctx, HttpRequest req) throws Exception {
    final HttpClientInstrumentation.RequestHandler<HttpRequest> requestHandler = clientInstrumentation.createHandler(KamonArmeriaMessageConverter.getRequestBuilder(req), Kamon.currentContext());
    final Span span = requestHandler.span();
    System.out.println(Kamon.currentContext().hashCode());

    ctx.log()
            .whenComplete()
            .thenAccept(log -> {
              System.out.println(Kamon.currentContext().hashCode());
              Timing.takeTimings(log, requestHandler.span());
              requestHandler.processResponse(KamonArmeriaMessageConverter.toKamonResponse(log));
            });

//    try (Storage.Scope ignored = Kamon.storeContext(Kamon.currentContext().withEntry(Span.Key(), span))) {
//      System.out.println(Kamon.currentContext().hashCode());
//      return unwrap().execute(ctx, requestHandler.request());
//    }

    try {
      Kamon.storeContext(Kamon.currentContext().withEntry(Span.Key(), span));
      System.out.println(Kamon.currentContext().hashCode());
      return unwrap().execute(ctx, requestHandler.request());
    } catch (Exception ex){
      Kamon.currentSpan().fail(ex.getMessage(), ex);
      throw ex;
    } finally {
      Kamon.currentSpan().finish();
    }

  }


}
