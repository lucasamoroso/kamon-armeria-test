package example.proxy;

import com.linecorp.armeria.common.*;
import com.linecorp.armeria.server.AbstractHttpService;
import com.linecorp.armeria.server.ServiceRequestContext;
import io.netty.channel.EventLoop;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class AnimationService extends AbstractHttpService {

    private static final List<String> frames = Arrays.asList(
            "<pre>" +
            "╔════╤╤╤╤════╗\n" +
            "║    │││ \\   ║\n" +
            "║    │││  O  ║\n" +
            "║    OOO     ║" +
            "</pre>",

            "<pre>" +
            "╔════╤╤╤╤════╗\n" +
            "║    ││││    ║\n" +
            "║    ││││    ║\n" +
            "║    OOOO    ║" +
            "</pre>",

            "<pre>" +
            "╔════╤╤╤╤════╗\n" +
            "║   / │││    ║\n" +
            "║  O  │││    ║\n" +
            "║     OOO    ║" +
            "</pre>",

            "<pre>" +
            "╔════╤╤╤╤════╗\n" +
            "║    ││││    ║\n" +
            "║    ││││    ║\n" +
            "║    OOOO    ║" +
            "</pre>"
    );

    private final int frameIntervalMillis;

    public AnimationService(int frameIntervalMillis) {
        if (frameIntervalMillis < 0) {
            throw new IllegalArgumentException("frameIntervalMillis: " + frameIntervalMillis +
                                               " (expected: >= 0)");
        }
        this.frameIntervalMillis = frameIntervalMillis;
    }

    @Override
    protected HttpResponse doGet(ServiceRequestContext ctx, HttpRequest req) throws Exception {
        // Create a response for streaming. If you don't need to stream, use HttpResponse.of(...) instead.
        final HttpResponseWriter res = HttpResponse.streaming();
        res.write(ResponseHeaders.of(HttpStatus.OK,
                                     HttpHeaderNames.CONTENT_TYPE, MediaType.PLAIN_TEXT_UTF_8));
        res.whenConsumed().thenRun(() -> streamData(ctx.eventLoop(), res, 0));
        return res;
    }

    private void streamData(EventLoop executor, HttpResponseWriter writer, int frameIndex) {
        final int index = frameIndex % frames.size();
        writer.write(HttpData.ofUtf8(frames.get(index)));
        writer.whenConsumed().thenRun(() -> executor.schedule(() -> streamData(executor, writer, index + 1),
                                                              frameIntervalMillis, TimeUnit.MILLISECONDS));
    }
}
