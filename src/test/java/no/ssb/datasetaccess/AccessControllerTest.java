package no.ssb.datasetaccess;

import io.micronaut.context.ApplicationContext;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.client.RxHttpClient;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.runtime.server.EmbeddedServer;
import io.reactivex.Flowable;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class AccessControllerTest {

    /*
      {
       "typ": "JWT",
       "alg": "HS256"
      }
      .
      {
       "sub": "USER_1",
       "name": "Stan Statistics",
       "admin": true,
       "jti": "645b5a28-392b-4b87-a3a6-41eebd7a0ed8",
       "iat": 1574328348,
       "exp": 1574331948
      }
      .
      secret
     */
    private static final String USER_WITH_ACCESS = "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJVU0VSXzEiLCJuYW1lIjoiSm9obiBEb2UiLCJhZG1pbiI6dHJ1ZSwianRpIjoiY2VhNmFjMmItNjFlNy00MzA2LWFjNDUtODdhZjVkODcyMjg2IiwiaWF0IjoxNTc0NDI3ODQ1LCJleHAiOjE1NzQ0MzE0NjN9.brr0OqtGH9m7tJyrcBtEm1zlrniOecpQso1xi5vHuAo";

    /*
      {
       "typ": "JWT",
       "alg": "HS256"
      }
      .
      {
       "sub": "USER_2",
       "name": "Dan Data",
       "admin": true,
       "jti": "f2a09bb2-005d-4d84-9a84-1dbc544cc7e1",
       "iat": 1574671395,
       "exp": 1574674995
      }
      .
      secret
     */
    private static final String USER_WITHOUT_ACCESS = "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJVU0VSXzIiLCJuYW1lIjoiRGFuIERhdGEiLCJhZG1pbiI6dHJ1ZSwianRpIjoiZjJhMDliYjItMDA1ZC00ZDg0LTlhODQtMWRiYzU0NGNjN2UxIiwiaWF0IjoxNTc0NjcxMzk1LCJleHAiOjE1NzQ2NzUwMTl9.vfi4_JkUCSph1f90myCmirqrRvuTJxTMbNBLSs_X-is";

    @Test
    void shouldReturnOkWhenUserHasAccess() {

        try (PostgreSQLContainer postgres = createPostgresContainer()) {

            postgres.start();

            try (
                    ApplicationContext applicationContext = runApplicationContext(postgres);
                    RxHttpClient httpClient = applicationContext.createBean(
                            RxHttpClient.class,
                            applicationContext.getBean(EmbeddedServer.class).start().getURL()
                    )
            ) {

                //############################### User with access should get 200 OK ###############################
                final MutableHttpRequest<Object> userWithAccessRequest = HttpRequest
                        .GET("/access/DATASET_1")
                        .header("Authorization", USER_WITH_ACCESS);

                Flowable<HttpResponse<String>> call = httpClient.exchange(userWithAccessRequest, String.class);

                final HttpResponse<String> response = call.blockingFirst();
                assertThat(response.getStatus().getCode()).isEqualTo(HttpStatus.OK.getCode());
                assertThat(response.getStatus().getReason()).isEqualTo(HttpStatus.OK.getReason());

                //############################### Missing token should result in 400 Bad request #######################
                Flowable<HttpResponse<String>> missingTokenCall = httpClient.exchange(HttpRequest.GET("/access/123"), String.class);

                final Throwable thrown = catchThrowable(missingTokenCall::blockingFirst);

                assertThat(thrown).isInstanceOf(HttpClientResponseException.class);
                assertThat(((HttpClientResponseException) thrown).getStatus().getCode()).isEqualTo(HttpStatus.BAD_REQUEST.getCode());
                assertThat(((HttpClientResponseException) thrown).getStatus().getReason()).isEqualTo(HttpStatus.BAD_REQUEST.getReason());

                //############################### User without access should get 403 Forbidden #########################
                MutableHttpRequest<Object> userWithoutAccessRequest = HttpRequest
                        .GET("/access/123")
                        .header("Authorization", USER_WITHOUT_ACCESS);

                Flowable<HttpResponse<String>> userWithoutAccessCall = httpClient.exchange(userWithoutAccessRequest, String.class);

                final Throwable forbiddenResponse = catchThrowable(userWithoutAccessCall::blockingFirst);

                assertThat(forbiddenResponse).isInstanceOf(HttpClientResponseException.class);
                assertThat(((HttpClientResponseException) forbiddenResponse).getStatus().getCode()).isEqualTo(HttpStatus.FORBIDDEN.getCode());
                assertThat(((HttpClientResponseException) forbiddenResponse).getStatus().getReason()).isEqualTo(HttpStatus.FORBIDDEN.getReason());

                //############################### Missing path parameter should result in 405 Method not allowed #######
                MutableHttpRequest<Object> missingPathParameterRequest = HttpRequest
                        .GET("/access")
                        .header("Authorization", USER_WITHOUT_ACCESS);

                Flowable<HttpResponse<String>> missingPathParameterCall = httpClient.exchange(missingPathParameterRequest, String.class);

                final Throwable methodNotAllowedResponse = catchThrowable(missingPathParameterCall::blockingFirst);

                assertThat(methodNotAllowedResponse).isInstanceOf(HttpClientResponseException.class);
                assertThat(((HttpClientResponseException) methodNotAllowedResponse).getStatus().getCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED.getCode());
                assertThat(((HttpClientResponseException) methodNotAllowedResponse).getStatus().getReason()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED.getReason());
            }
        }
    }

    @Test
    void shouldBeAbleToGrantAccess() {

        try (PostgreSQLContainer postgres = createPostgresContainer()) {

            postgres.start();

            try (
                    ApplicationContext applicationContext = runApplicationContext(postgres);
                    RxHttpClient httpClient = applicationContext.createBean(
                            RxHttpClient.class,
                            applicationContext.getBean(EmbeddedServer.class).start().getURL()
                    )
            ) {

                //############################### Grant USER_2 access to DATASET_1 ###############################
                MutableHttpRequest<Object> request = HttpRequest.POST(
                        "/access",
                        "{\"dataset_id\":\"DATASET_1\",\"user_id\":\"USER_2\"}"
                );

                Flowable<HttpResponse<String>> grantAccessCall = httpClient.exchange(request, String.class);

                final HttpResponse<String> grantAccessResponse = grantAccessCall.blockingFirst();
                assertThat(grantAccessResponse.getStatus().getCode()).isEqualTo(HttpStatus.OK.getCode());
                assertThat(grantAccessResponse.getStatus().getReason()).isEqualTo(HttpStatus.OK.getReason());

                //############################### Verify that USER_2 has access to DATASET_1 ###########################
                final MutableHttpRequest<Object> userWithAccessRequest = HttpRequest
                        .GET("/access/DATASET_1")
                        .header("Authorization", USER_WITHOUT_ACCESS);

                Flowable<HttpResponse<String>> getAccessCall = httpClient.exchange(userWithAccessRequest, String.class);

                final HttpResponse<String> getAccessResponse = getAccessCall.blockingFirst();
                assertThat(getAccessResponse.getStatus().getCode()).isEqualTo(HttpStatus.OK.getCode());
                assertThat(getAccessResponse.getStatus().getReason()).isEqualTo(HttpStatus.OK.getReason());
            }
        }
    }

    private static PostgreSQLContainer createPostgresContainer() {
        return new PostgreSQLContainer<>()
                .withInitScript("test-db.sql")
                .withUsername("rdc")
                .withPassword("rdc")
                .withDatabaseName("rdc");
    }

    private static ApplicationContext runApplicationContext(final PostgreSQLContainer postgresContainer) {
        return ApplicationContext.run(Map.of(
                "postgres.reactive.client.port", postgresContainer.getMappedPort(5432),
                "postgres.reactive.client.host", postgresContainer.getContainerIpAddress(),
                "postgres.reactive.client.database", postgresContainer.getDatabaseName()
        ));
    }
}
