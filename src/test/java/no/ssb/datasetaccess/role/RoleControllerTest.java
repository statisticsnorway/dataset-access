package no.ssb.datasetaccess.role;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.RxHttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.annotation.MicronautTest;
import no.ssb.datasetaccess.HttpClientTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@MicronautTest
class RoleControllerTest {

    @Inject
    @Client("/")
    RxHttpClient httpClient;

    @Inject
    RoleRepository repository;

    @BeforeEach
    void clearRoleRepository() {
        repository.deleteAllRoles().blockingAwait(3, TimeUnit.SECONDS);
    }

    void createRole(String roleId, Privilege... privileges) {
        LinkedHashSet<Privilege> set = new LinkedHashSet<>();
        Arrays.stream(privileges).forEach(p -> set.add(p));
        repository.createRole(new Role(roleId, set)).blockingAwait(3, TimeUnit.SECONDS);
    }

    Role readRole(String roleId) {
        return repository.getRole(roleId).blockingGet();
    }

    @Test
    void thatGetRoleWorks() {
        createRole("writer", Privilege.CREATE, Privilege.UPDATE);
        HttpResponse<Role> response = httpClient.exchange(HttpRequest.GET("/role/writer"), Role.class).blockingFirst();
        Role role = response.getBody().orElseThrow();
        assertThat((CharSequence) response.getStatus()).isEqualTo(HttpStatus.OK);
        assertThat(role.name).isEqualTo("writer");
        assertThat(role.privileges).isEqualTo(Set.of(Privilege.CREATE, Privilege.UPDATE));
    }

    @Test
    void thatGetNonExistentRoleRespondsWith404NotFound() {
        HttpResponse<Role> response = httpClient.exchange(HttpRequest.GET("/role/non-existent"), Role.class)
                .onErrorReturn(HttpClientTestUtils::toHttpResponse).blockingFirst();
        assertThat((CharSequence) response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void thatPutRoleWorks() {
        HttpResponse<String> response = httpClient.exchange(HttpRequest.PUT("/role/reader", new Role("reader", Set.of(Privilege.READ))), String.class).blockingFirst();
        assertThat((CharSequence) response.getStatus()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getHeaders().getFirst("Location").orElseThrow()).isEqualTo("/role/reader");
        Role role = readRole("reader");
        assertThat(role.name).isEqualTo("reader");
        assertThat(role.privileges).isEqualTo(Set.of(Privilege.READ));
    }

    @Test
    void thatCreateUpsertPutRoleWorks() {
        HttpResponse<String> upsert1 = httpClient.exchange(HttpRequest.PUT("/role/upsert_role", new Role("upsert_role", Set.of(Privilege.CREATE, Privilege.READ))), String.class).blockingFirst();
        assertThat((CharSequence) upsert1.getStatus()).isEqualTo(HttpStatus.CREATED);
        Role role1 = readRole("upsert_role");
        assertThat(role1.privileges).isEqualTo(Set.of(Privilege.CREATE, Privilege.READ));
        HttpResponse<String> upsert2 = httpClient.exchange(HttpRequest.PUT("/role/upsert_role", new Role("upsert_role", Set.of(Privilege.UPDATE, Privilege.DELETE))), String.class).blockingFirst();
        assertThat((CharSequence) upsert2.getStatus()).isEqualTo(HttpStatus.CREATED);
        Role role2 = readRole("upsert_role");
        assertThat(role2.privileges).isEqualTo(Set.of(Privilege.UPDATE, Privilege.DELETE));
    }

    @Test
    void thatDeleteRoleWorks() {
        createRole("role_to_be_deleted", Privilege.CREATE);
        HttpResponse<String> response = httpClient.exchange(HttpRequest.DELETE("/role/role_to_be_deleted"), String.class).blockingFirst();
        assertThat((CharSequence) response.getStatus()).isEqualTo(HttpStatus.OK);
        assertThat(readRole("role_to_be_deleted")).isNull();
    }
}
