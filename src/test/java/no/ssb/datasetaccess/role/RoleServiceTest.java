package no.ssb.datasetaccess.role;

import no.ssb.datasetaccess.dataset.DatasetState;
import no.ssb.datasetaccess.dataset.Valuation;
import no.ssb.datasetaccess.testing.IntegrationTestExtension;
import no.ssb.datasetaccess.testing.ResponseHelper;
import no.ssb.datasetaccess.testing.TestClient;
import no.ssb.datasetaccess.testing.TestServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.inject.Inject;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@ExtendWith(IntegrationTestExtension.class)
class RoleServiceTest {

    @Inject
    TestServer server;

    @Inject
    TestClient client;

    @BeforeEach
    void clearRoleRepository() throws InterruptedException, ExecutionException, TimeoutException {
        server.get(RoleRepository.class).deleteAllRoles().get(3, TimeUnit.SECONDS);
    }

    Role createRole(String roleId, Set<Privilege> privileges, Set<String> namespacePrefixes, Valuation maxValuation, Set<DatasetState> states) {
        Role role = new Role(roleId, privileges, new TreeSet<>(namespacePrefixes), maxValuation, states);
        try {
            server.get(RoleRepository.class).createRole(role).get(3, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new RuntimeException(e);
        }
        return role;
    }

    Role readRole(String roleId) {
        return server.get(RoleRepository.class).getRole(roleId).join();
    }

    @Test
    void thatGetRoleWorks() {
        Role expectedRole = createRole("writer", Set.of(Privilege.CREATE, Privilege.UPDATE), Set.of("/ns/test"), Valuation.INTERNAL, Set.of(DatasetState.RAW, DatasetState.INPUT));
        String body = client.get("/role/writer").expect200Ok().body();
        System.out.printf("%s%n", body);
        Role role = Role.fromJsonString(body);
        assertEquals(expectedRole, role);
    }

    @Test
    void thatGetNonExistentRoleRespondsWith404NotFound() {
        client.get("/role/non-existent").expect404NotFound();
    }

    @Test
    void thatPutRoleWorks() {
        Role expectedRole = new Role("reader", Set.of(Privilege.READ), new TreeSet<>(Set.of("/ns/1")), Valuation.SHIELDED, Set.of(DatasetState.RAW, DatasetState.INPUT, DatasetState.PROCESSED));
        ResponseHelper<String> helper = client.put("/role/reader", expectedRole).expect201Created();
        assertEquals("/role/reader", helper.response().headers().firstValue("Location").orElseThrow());
        Role role = readRole("reader");
        assertEquals(expectedRole, role);
    }

    @Test
    void thatCreateUpsertPutRoleWorks() {
        Role upsert_role = new Role("upsert_role", Set.of(Privilege.CREATE, Privilege.READ), new TreeSet<>(Set.of("/a/b/c")), Valuation.INTERNAL, Set.of(DatasetState.RAW, DatasetState.INPUT));
        client.put("/role/upsert_role", upsert_role).expect201Created();
        Role role1 = readRole("upsert_role");
        assertEquals(Set.of(Privilege.CREATE, Privilege.READ), role1.privileges);
        client.put("/role/upsert_role", new Role("upsert_role", Set.of(Privilege.UPDATE, Privilege.DELETE), new TreeSet<>(Set.of("/d/e")), Valuation.SHIELDED, Set.of(DatasetState.PROCESSED))).expect201Created();
        Role role2 = readRole("upsert_role");
        assertEquals(Set.of(Privilege.UPDATE, Privilege.DELETE), role2.privileges);
    }

    @Test
    void thatDeleteRoleWorks() {
        createRole("role_to_be_deleted", Set.of(Privilege.CREATE), Set.of("/universe"), Valuation.SENSITIVE, Set.of(DatasetState.INPUT));
        client.delete("/role/role_to_be_deleted").expect200Ok();
        assertNull(readRole("role_to_be_deleted"));
    }
}
