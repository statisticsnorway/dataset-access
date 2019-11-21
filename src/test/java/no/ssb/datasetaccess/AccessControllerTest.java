package no.ssb.datasetaccess;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.client.RxHttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.test.annotation.MicronautTest;
import io.reactivex.Flowable;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@MicronautTest
class AccessControllerTest {

    @Inject
    private EmbeddedServer server;

    @Inject
    @Client("/")
    private RxHttpClient httpClient;

    @Test
    void shouldReturnXWhenNoTokenIsPresent() {

        Flowable<HttpResponse<String>> call = httpClient.exchange(HttpRequest.GET("/access"), String.class);

        try {
            HttpResponse<String> response = call.blockingFirst();
            fail("Expected BAD_REQUEST");
        } catch (HttpClientResponseException e) {
            assertThat(e.getStatus().getCode()).isEqualTo(HttpStatus.BAD_REQUEST.getCode());
        }
    }

    @Test
    void shouldReturnYWhenInvalidToken() {

        MutableHttpRequest<Object> request = HttpRequest
                .GET("/access")
                .header("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c");

        Flowable<HttpResponse<String>> call = httpClient.exchange(request, String.class);

        try {
            HttpResponse<String> response = call.blockingFirst();
            fail("Expected UNAUTHORIZED");
        } catch (HttpClientResponseException e) {
            assertThat(e.getStatus().getCode()).isEqualTo(HttpStatus.UNAUTHORIZED.getCode());
        }
    }

    @Test
    void shouldDecodeToken() {


    }
}
