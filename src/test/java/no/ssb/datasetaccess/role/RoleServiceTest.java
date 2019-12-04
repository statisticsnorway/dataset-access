package no.ssb.datasetaccess.role;

import no.ssb.datasetaccess.dataset.DatasetState;
import no.ssb.datasetaccess.dataset.Valuation;
import no.ssb.datasetaccess.testing.TestClient;
import no.ssb.datasetaccess.testing.TestServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RoleServiceTest {

    static TestServer server;
    static TestClient client;

    @BeforeAll
    static void setupApplication() {
        server = new TestServer();
        client = server.client();
    }

    @AfterAll
    static void stopApplication() {
        server.shutdown();
    }

    @BeforeEach
    void clearRoleRepository() throws InterruptedException, ExecutionException, TimeoutException {
        server.getApplication().getRoleRepository().deleteAllRoles().get(3, TimeUnit.SECONDS);
    }

    Role createRole(String roleId, Set<Privilege> privileges, Set<String> namespacePrefixes, Valuation maxValuation, Set<DatasetState> states) {
        Role role = new Role(roleId, privileges, new TreeSet<>(namespacePrefixes), maxValuation, states);
        try {
            server.getApplication().getRoleRepository().createRole(role).get(3, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new RuntimeException(e);
        }
        return role;
    }

    Role readRole(String roleId) {
        return server.getApplication().getRoleRepository().getRole(roleId).join();
    }

    @Test
    void thatGetRoleWorks() throws IOException, InterruptedException {
        Role expectedRole = createRole("writer", Set.of(Privilege.CREATE, Privilege.UPDATE), Set.of("/ns/test"), Valuation.INTERNAL, Set.of(DatasetState.RAW, DatasetState.INPUT));
        String body = client.get("/role/writer").expect200Ok().body();
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
