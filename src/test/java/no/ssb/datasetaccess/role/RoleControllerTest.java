package no.ssb.datasetaccess.role;

import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.RxHttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@MicronautTest
class RoleControllerTest {

    @Inject
    @Client("/")
    private RxHttpClient httpClient;

    @Test
    void thatGetRoleWorks() {
        HttpResponse<Role> response = httpClient.exchange(HttpRequest.GET("/role/writer"), Role.class).blockingFirst();
        Role role = response.getBody().orElseThrow();
        System.out.printf("%s%n", role);
        assertThat((CharSequence) response.getStatus()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void thatPutRoleWorks() {
        HttpResponse<String> response = httpClient.exchange(HttpRequest.PUT("/role/reader", new Role("reader", Set.of(Privilege.READ))), String.class).blockingFirst();
        assertThat((CharSequence) response.getStatus()).isEqualTo(HttpStatus.CREATED);
        HttpHeaders headers = response.getHeaders();
        System.out.printf("HEADERS:%n%s%n", headers.asMap());
    }

    @Test
    void thatUpsertPutRoleWorks() {
        HttpResponse<String> upsert1 = httpClient.exchange(HttpRequest.PUT("/role/upsert_role", new Role("upsert_role", Set.of(Privilege.CREATE, Privilege.READ))), String.class).blockingFirst();
        assertThat((CharSequence) upsert1.getStatus()).isEqualTo(HttpStatus.CREATED);
        Role role1 = httpClient.exchange(HttpRequest.GET("/role/upsert_role"), Role.class).blockingFirst().getBody().orElseThrow();
        assertThat(role1.privileges).isEqualTo(Set.of(Privilege.CREATE, Privilege.READ));
        HttpResponse<String> upsert2 = httpClient.exchange(HttpRequest.PUT("/role/upsert_role", new Role("upsert_role", Set.of(Privilege.UPDATE, Privilege.DELETE))), String.class).blockingFirst();
        assertThat((CharSequence) upsert2.getStatus()).isEqualTo(HttpStatus.CREATED);
        Role role2 = httpClient.exchange(HttpRequest.GET("/role/upsert_role"), Role.class).blockingFirst().getBody().orElseThrow();
        assertThat(role2.privileges).isEqualTo(Set.of(Privilege.UPDATE, Privilege.DELETE));
    }

    @Test
    void thatDeleteRoleWorks() {
        HttpResponse<String> response = httpClient.exchange(HttpRequest.DELETE("/role/reader"), String.class).blockingFirst();
        assertThat((CharSequence) response.getStatus()).isEqualTo(HttpStatus.OK);
        HttpHeaders headers = response.getHeaders();
        System.out.printf("HEADERS:%n%s%n", headers.asMap());
    }
}
