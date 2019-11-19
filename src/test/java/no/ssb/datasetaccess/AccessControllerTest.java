package no.ssb.datasetaccess;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.client.RxHttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.test.annotation.MicronautTest;
import io.reactivex.Flowable;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

import static org.assertj.core.api.Assertions.assertThat;

@MicronautTest
class AccessControllerTest {

    @Inject
    private EmbeddedServer server;

    @Inject
    @Client("/")
    private RxHttpClient httpClient;

    @Test
    void shouldReturnXWhenNoTokenIsPresent() {

        Flowable<HttpResponse<String>> call = httpClient.exchange(HttpRequest.GET("/auth"), String.class);

        HttpResponse<String> response = call.blockingFirst();

        assertThat(response.getBody()).contains("X"); //FIXME
        assertThat((CharSequence) response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST); //FIXME
    }

    @Test
    void shouldReturnYWhenInvalidToken() {

        MutableHttpRequest<Object> request = HttpRequest
                .GET("/auth")
                .header("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c");

        Flowable<HttpResponse<String>> call = httpClient.exchange(request, String.class);

        HttpResponse<String> response = call.blockingFirst();

        assertThat(response.getBody()).contains("Y"); //FIXME
        assertThat(response.code()).isEqualTo(400); //FIXME
    }

    @Test
    void shouldDecodeToken() {



    }
}
