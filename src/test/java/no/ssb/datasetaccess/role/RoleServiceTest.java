package no.ssb.datasetaccess.role;

import io.helidon.common.http.Http;
import no.ssb.datasetaccess.Application;
import no.ssb.datasetaccess.dataset.DatasetState;
import no.ssb.datasetaccess.dataset.Valuation;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RoleServiceTest {

    static Application application;

    static HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    @BeforeAll
    static void setupApplication() {
        application = new Application();
    }

    @AfterAll
    static void stopApplication() {
        application.getWebServer().shutdown();
    }

    @BeforeEach
    void clearRoleRepository() throws InterruptedException, ExecutionException, TimeoutException {
        application.getRoleRepository().deleteAllRoles().get(3, TimeUnit.SECONDS);
    }

    URI toUri(String path) {
        try {
            return new URI("http", "test", "localhost", application.getWebServer().port(), path, "", "");
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    Role createRole(String roleId, Set<Privilege> privileges, Set<String> namespacePrefixes, Valuation maxValuation, Set<DatasetState> states) {
        Role role = new Role(roleId, privileges, new TreeSet<>(namespacePrefixes), maxValuation, states);
        try {
            application.getRoleRepository().createRole(role).get(3, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new RuntimeException(e);
        }
        return role;
    }

    Role readRole(String roleId) {
        return application.getRoleRepository().getRole(roleId).join();
    }

    @Test
    void thatGetRoleWorks() throws IOException, InterruptedException {
        Role expectedRole = createRole("writer", Set.of(Privilege.CREATE, Privilege.UPDATE), Set.of("/ns/test"), Valuation.INTERNAL, Set.of(DatasetState.RAW, DatasetState.INPUT));
        HttpRequest request = HttpRequest.newBuilder().GET().uri(toUri("/role/writer")).build();
        HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(Http.Status.OK_200.code(), response.statusCode());
        String body = response.body();
        System.out.printf("%s%n", body);
        Role role = Role.fromJsonString(body);
        assertEquals(expectedRole, role);
    }

    @Test
    void thatGetNonExistentRoleRespondsWith404NotFound() {
        //HttpResponse<Role> response = httpClient.exchange(HttpRequest.GET("/role/non-existent"), Role.class)
        //        .onErrorReturn(HttpClientTestUtils::toHttpResponse).blockingFirst();
        //assertThat((CharSequence) response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void thatPutRoleWorks() {
        //Role expectedRole = new Role("reader", Set.of(Privilege.READ), new TreeSet<>(Set.of("/ns/1")), Valuation.SHIELDED, Set.of(DatasetState.RAW, DatasetState.INPUT, DatasetState.PROCESSED));
        //HttpResponse<String> response = httpClient.exchange(HttpRequest.PUT("/role/reader", expectedRole), String.class).blockingFirst();
        //assertThat((CharSequence) response.getStatus()).isEqualTo(HttpStatus.CREATED);
        //assertThat(response.getHeaders().getFirst("Location").orElseThrow()).isEqualTo("/role/reader");
        //Role role = readRole("reader");
        //assertThat(role).isEqualTo(expectedRole);
    }

    @Test
    void thatCreateUpsertPutRoleWorks() {
        //HttpResponse<String> upsert1 = httpClient.exchange(HttpRequest.PUT("/role/upsert_role", new Role("upsert_role", Set.of(Privilege.CREATE, Privilege.READ), new TreeSet<>(Set.of("/a/b/c")), Valuation.INTERNAL, Set.of(DatasetState.RAW, DatasetState.INPUT))), String.class).blockingFirst();
        //assertThat((CharSequence) upsert1.getStatus()).isEqualTo(HttpStatus.CREATED);
        //Role role1 = readRole("upsert_role");
        //assertThat(role1.privileges).isEqualTo(Set.of(Privilege.CREATE, Privilege.READ));
        //HttpResponse<String> upsert2 = httpClient.exchange(HttpRequest.PUT("/role/upsert_role", new Role("upsert_role", Set.of(Privilege.UPDATE, Privilege.DELETE), new TreeSet<>(Set.of("/d/e")), Valuation.SHIELDED, Set.of(DatasetState.PROCESSED))), String.class).blockingFirst();
        //assertThat((CharSequence) upsert2.getStatus()).isEqualTo(HttpStatus.CREATED);
        //Role role2 = readRole("upsert_role");
        //assertThat(role2.privileges).isEqualTo(Set.of(Privilege.UPDATE, Privilege.DELETE));
    }

    @Test
    void thatDeleteRoleWorks() {
        //createRole("role_to_be_deleted", Set.of(Privilege.CREATE), Set.of("/universe"), Valuation.SENSITIVE, Set.of(DatasetState.INPUT));
        //HttpResponse<String> response = httpClient.exchange(HttpRequest.DELETE("/role/role_to_be_deleted"), String.class).blockingFirst();
        //assertThat((CharSequence) response.getStatus()).isEqualTo(HttpStatus.OK);
        //assertThat(readRole("role_to_be_deleted")).isNull();
    }
}
