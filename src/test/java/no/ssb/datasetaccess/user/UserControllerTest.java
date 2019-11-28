package no.ssb.datasetaccess.user;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.RxHttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.annotation.MicronautTest;
import no.ssb.datasetaccess.role.Privilege;
import no.ssb.datasetaccess.role.Role;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;

@MicronautTest
class UserControllerTest {

    @Inject
    @Client("/")
    private RxHttpClient httpClient;

    @Test
    void thatGetUserWorks() {
        HttpResponse<User> response = httpClient.exchange(HttpRequest.GET("/user/john"), User.class).blockingFirst();
        assertThat((CharSequence) response.getStatus()).isEqualTo(HttpStatus.OK);

        final User actual = response.getBody().orElseThrow();
        final User expected = new User("john", Set.of(new Role("reader", Set.of(Privilege.READ))), new TreeSet<>(Set.of("/a/b/c")));

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void thatPutWorks() {
        final User user = new User("john", Set.of(new Role("reader", Set.of(Privilege.READ))), new TreeSet<>(Set.of("/a/b/c")));
        HttpResponse<String> response = httpClient.exchange(HttpRequest.PUT("/user/john", user), String.class).blockingFirst();
        assertThat((CharSequence) response.getStatus()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getHeaders().get("Location")).isEqualTo("/user/john");
    }

    @Test
    void thatDeleteUserWorks() {
        HttpResponse<String> response = httpClient.exchange(HttpRequest.DELETE("/user/john"), String.class).blockingFirst();
        assertThat((CharSequence) response.getStatus()).isEqualTo(HttpStatus.OK);
    }
}
