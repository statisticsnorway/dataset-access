package no.ssb.datasetaccess.user;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.RxHttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.annotation.MicronautTest;
import no.ssb.datasetaccess.HttpClientTestUtils;
import no.ssb.datasetaccess.dataset.DatasetState;
import no.ssb.datasetaccess.dataset.Valuation;
import no.ssb.datasetaccess.role.Privilege;
import no.ssb.datasetaccess.role.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@MicronautTest
class UserControllerTest {

    @Inject
    @Client("/")
    private RxHttpClient httpClient;

    @Inject
    private UserRepository repository;

    @BeforeEach
    void clearUserRepository() {
        repository.deleteAllUsers().blockingAwait(3, TimeUnit.SECONDS);
    }

    void createUser(User user) {
        repository.createUser(user).blockingAwait(3, TimeUnit.SECONDS);
    }

    User getUser(String userId) {
        return repository.getUser(userId).blockingGet();
    }

    @Test
    void thatGetUserWorks() {
        final User expected = new User("john", Set.of(new Role("reader", Set.of(Privilege.READ), new TreeSet<>(Set.of("/a/b/c")), Valuation.INTERNAL, Set.of(DatasetState.PROCESSED))));
        createUser(expected);
        HttpResponse<User> response = httpClient.exchange(HttpRequest.GET("/user/john"), User.class).blockingFirst();
        assertThat((CharSequence) response.getStatus()).isEqualTo(HttpStatus.OK);
        final User actual = response.getBody().orElseThrow();
        assertThat(actual).isEqualToComparingFieldByField(expected);
    }

    @Test
    void thatGetOnNonExistingUserReturns404() {
        HttpResponse<User> response = httpClient.exchange(HttpRequest.GET("/user/john"), User.class)
                .onErrorReturn(HttpClientTestUtils::toHttpResponse)
                .blockingFirst();
        assertThat((CharSequence) response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void thatPutWorks() {
        final User userToCreate = new User("john", Set.of(new Role("reader", Set.of(Privilege.READ), new TreeSet<>(Set.of("/a/b/c")), Valuation.INTERNAL, Set.of(DatasetState.PROCESSED))));
        HttpResponse<String> response = httpClient.exchange(HttpRequest.PUT("/user/john", userToCreate), String.class).blockingFirst();
        assertThat((CharSequence) response.getStatus()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getHeaders().get("Location")).isEqualTo("/user/john");

        final User createdUser = getUser(userToCreate.getUserId());
        assertThat(createdUser).isEqualToComparingFieldByField(userToCreate);
    }

    @Test
    void thatUpsertWorks() {
        final User initialUser = new User("john", Set.of(new Role("reader", Set.of(Privilege.READ), new TreeSet<>(Set.of("/a/b/c")), Valuation.INTERNAL, Set.of(DatasetState.RAW))));
        createUser(initialUser);

        final User upsertUser = new User("john", Set.of(new Role("reader", Set.of(Privilege.READ, Privilege.DELETE), new TreeSet<>(Set.of("/a/b/c", "x/y/z")), Valuation.OPEN, Set.of(DatasetState.RAW, DatasetState.INPUT))));
        HttpResponse<String> response = httpClient.exchange(HttpRequest.PUT("/user/john", upsertUser), String.class).blockingFirst();
        assertThat((CharSequence) response.getStatus()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getHeaders().get("Location")).isEqualTo("/user/john");

        final User actual = getUser(upsertUser.getUserId());
        assertThat(actual).isEqualToComparingFieldByField(upsertUser);
    }

    @Test
    void thatDeleteUserWorks() {
        final User userToDelete = new User("john", Set.of(new Role("reader", Set.of(Privilege.READ), new TreeSet<>(Set.of("/a/b/c")), Valuation.OPEN, Set.of(DatasetState.OTHER))));
        createUser(userToDelete);
        HttpResponse<String> response = httpClient.exchange(HttpRequest.DELETE("/user/john"), String.class).blockingFirst();
        assertThat((CharSequence) response.getStatus()).isEqualTo(HttpStatus.OK);
        assertThat(getUser(userToDelete.getUserId())).isNull();
    }
}
