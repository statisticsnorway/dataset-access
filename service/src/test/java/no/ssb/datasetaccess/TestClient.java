package no.ssb.datasetaccess;

import no.ssb.helidon.media.protobuf.ProtobufJsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

public final class TestClient {

    private static final Logger LOG = LoggerFactory.getLogger(TestClient.class);

    private final String host;
    private final int port;
    private final HttpClient client;

    private TestClient(String host, int port) {
        this.host = host;
        this.port = port;
        this.client = HttpClient
                .newBuilder()
                .build();
    }

    public static TestClient newClient(String host, int port) {
        return new TestClient(host, port);
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    URI toUri(String path) {
        try {
            return new URL(String.format("http://%s:%d%s", host, port, path)).toURI();
        } catch (URISyntaxException | MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    static String captureStackTrace(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }

    public ResponseHelper<String> options(String uri, String... headersKeyAndValue) {
        return options(uri, HttpResponse.BodyHandlers.ofString(), headersKeyAndValue);
    }

    public <R> ResponseHelper<R> options(String uri, HttpResponse.BodyHandler<R> bodyHandler, String... headersKeyAndValue) {
        try {
            HttpRequest request = HttpRequest.newBuilder(toUri(uri))
                    .headers(headersKeyAndValue)
                    .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                    .build();
            return new ResponseHelper<>(client.send(request, bodyHandler));
        } catch (Exception e) {
            LOG.error("Error: {}", captureStackTrace(e));
            throw new RuntimeException(e);
        }
    }

    public ResponseHelper<String> head(String uri) {
        return head(uri, HttpResponse.BodyHandlers.ofString());
    }

    public <R> ResponseHelper<R> head(String uri, HttpResponse.BodyHandler<R> bodyHandler) {
        try {
            HttpRequest request = HttpRequest.newBuilder(toUri(uri))
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .build();
            return new ResponseHelper<>(client.send(request, bodyHandler));
        } catch (Exception e) {
            LOG.error("Error: {}", captureStackTrace(e));
            throw new RuntimeException(e);
        }
    }

    public ResponseHelper<String> put(String uri) {
        return put(uri, HttpRequest.BodyPublishers.noBody(), HttpResponse.BodyHandlers.ofString());
    }

    public <T> ResponseHelper<String> put(String uri, T pojo) {
        return put(uri, HttpRequest.BodyPublishers.ofString(ProtobufJsonUtils.toString(pojo), StandardCharsets.UTF_8), HttpResponse.BodyHandlers.ofString());
    }

    public ResponseHelper<String> put(String uri, String body) {
        return put(uri, HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8), HttpResponse.BodyHandlers.ofString());
    }

    public <R> ResponseHelper<R> put(String uri, HttpRequest.BodyPublisher bodyPublisher, HttpResponse.BodyHandler<R> bodyHandler) {
        try {
            HttpRequest request = HttpRequest.newBuilder(toUri(uri))
                    .PUT(bodyPublisher)
                    .build();
            return new ResponseHelper<>(client.send(request, bodyHandler));
        } catch (Exception e) {
            LOG.error("Error: {}", captureStackTrace(e));
            throw new RuntimeException(e);
        }
    }

    public ResponseHelper<String> post(String uri) {
        return post(uri, HttpRequest.BodyPublishers.noBody(), HttpResponse.BodyHandlers.ofString());
    }

    public ResponseHelper<String> post(String uri, String body) {
        return post(uri, HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8), HttpResponse.BodyHandlers.ofString());
    }

    public ResponseHelper<String> postJson(String uri, String body) {
        return postJson(uri, HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8), HttpResponse.BodyHandlers.ofString());
    }

    public <R> ResponseHelper<R> post(String uri, HttpRequest.BodyPublisher bodyPublisher, HttpResponse.BodyHandler<R> bodyHandler) {
        try {
            HttpRequest request = HttpRequest.newBuilder(toUri(uri))
                    .POST(bodyPublisher)
                    .build();
            return new ResponseHelper<>(client.send(request, bodyHandler));
        } catch (Exception e) {
            LOG.error("Error: {}", captureStackTrace(e));
            throw new RuntimeException(e);
        }
    }

    public <R> ResponseHelper<R> postJson(String uri, HttpRequest.BodyPublisher bodyPublisher, HttpResponse.BodyHandler<R> bodyHandler) {
        try {
            HttpRequest request = HttpRequest.newBuilder(toUri(uri))
                    .POST(bodyPublisher)
                    .header("Content-Type", "application/json")
                    .build();
            return new ResponseHelper<>(client.send(request, bodyHandler));
        } catch (Exception e) {
            LOG.error("Error: {}", captureStackTrace(e));
            throw new RuntimeException(e);
        }
    }

    public <R> ResponseHelper<R> postForm(String uri, HttpRequest.BodyPublisher bodyPublisher, HttpResponse.BodyHandler<R> bodyHandler) {
        try {
            HttpRequest request = HttpRequest.newBuilder(toUri(uri))
                    .POST(bodyPublisher)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .build();
            return new ResponseHelper<>(client.send(request, bodyHandler));
        } catch (Exception e) {
            LOG.error("Error: {}", captureStackTrace(e));
            throw new RuntimeException(e);
        }
    }

    public ResponseHelper<String> get(String uri) {
        return get(uri, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    static class ProtobufSubscriber<R> implements HttpResponse.BodySubscriber<R> {
        final Class<R> clazz;
        final HttpResponse.BodySubscriber<String> stringBodySubscriber;

        ProtobufSubscriber(Class<R> clazz, HttpResponse.BodySubscriber<String> stringBodySubscriber) {
            this.clazz = clazz;
            this.stringBodySubscriber = stringBodySubscriber;
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            stringBodySubscriber.onSubscribe(subscription);
        }

        @Override
        public void onNext(List<ByteBuffer> item) {
            stringBodySubscriber.onNext(item);
        }

        @Override
        public void onError(Throwable throwable) {
            stringBodySubscriber.onError(throwable);
        }

        @Override
        public void onComplete() {
            stringBodySubscriber.onComplete();
        }

        @Override
        public CompletionStage<R> getBody() {
            return stringBodySubscriber.getBody()
                    .thenApply(body -> body.isBlank() ? null : ProtobufJsonUtils.toPojo(body, clazz));
        }
    }

    public <R> ResponseHelper<R> get(String uri, Class<R> clazz) {
        return get(uri, responseInfo -> new ProtobufSubscriber(clazz, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8).apply(responseInfo)));
    }

    public <R> ResponseHelper<R> get(String uri, HttpResponse.BodyHandler<R> bodyHandler) {
        try {
            HttpRequest request = HttpRequest.newBuilder(toUri(uri))
                    .GET()
                    .build();
            return new ResponseHelper<>(client.send(request, bodyHandler));
        } catch (Exception e) {
            LOG.error("Error: {}", captureStackTrace(e));
            throw new RuntimeException(e);
        }
    }

    public ResponseHelper<String> delete(String uri) {
        return delete(uri, HttpResponse.BodyHandlers.ofString());
    }

    public <R> ResponseHelper<R> delete(String uri, HttpResponse.BodyHandler<R> bodyHandler) {
        try {
            HttpRequest request = HttpRequest.newBuilder(toUri(uri))
                    .DELETE()
                    .build();
            return new ResponseHelper<>(client.send(request, bodyHandler));
        } catch (Exception e) {
            LOG.error("Error: {}", captureStackTrace(e));
            throw new RuntimeException(e);
        }
    }
}
