package no.ssb.datasetaccess;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.client.exceptions.HttpClientResponseException;

public class HttpClientTestUtils {

    public static <T> HttpResponse<T> toHttpResponse(Throwable t) {
        if (!(t instanceof HttpClientResponseException)) {
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            }
            if (t instanceof Error) {
                throw (Error) t;
            }
            throw new RuntimeException(t);
        }
        HttpClientResponseException e = (HttpClientResponseException) t;
        return (HttpResponse<T>) e.getResponse();
    }
}
