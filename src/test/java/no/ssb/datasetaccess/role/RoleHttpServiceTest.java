package no.ssb.datasetaccess.role;

import no.ssb.dapla.auth.dataset.protobuf.Role;
import no.ssb.dapla.auth.dataset.protobuf.Role.DatasetState;
import no.ssb.dapla.auth.dataset.protobuf.Role.Privilege;
import no.ssb.dapla.auth.dataset.protobuf.Role.Valuation;
import no.ssb.datasetaccess.Application;
import no.ssb.datasetaccess.IntegrationTestExtension;
import no.ssb.datasetaccess.ResponseHelper;
import no.ssb.datasetaccess.TestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@ExtendWith(IntegrationTestExtension.class)
class RoleHttpServiceTest {

    @Inject
    Application application;

    @Inject
    TestClient client;

    @BeforeEach
    void clearRoleRepository() throws InterruptedException, ExecutionException, TimeoutException {
        application.get(RoleRepository.class).deleteAllRoles().get(3, TimeUnit.SECONDS);
    }

    Role createRole(String roleId, Iterable<Privilege> privileges, Iterable<String> namespacePrefixes, Valuation maxValuation, Iterable<DatasetState> states) {
        Role role = Role.newBuilder()
                .setRoleId(roleId)
                .addAllPrivileges(privileges)
                .addAllNamespacePrefixes(namespacePrefixes)
                .setMaxValuation(maxValuation)
                .addAllStates(states)
                .build();
        try {
            application.get(RoleRepository.class).createOrUpdateRole(role).get(3, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new RuntimeException(e);
        }
        return role;
    }

    Role readRole(String roleId) {
        return application.get(RoleRepository.class).getRole(roleId).join();
    }

    @Test
    void thatGetRoleWorks() {
        Role expectedRole = createRole("writer", List.of(Privilege.CREATE, Privilege.UPDATE), List.of("/ns/test"), Valuation.INTERNAL, List.of(DatasetState.RAW, DatasetState.INPUT));
        Role role = client.get("/role/writer", Role.class).expect200Ok().body();
        assertEquals(expectedRole, role);
    }

    @Test
    void thatGetNonExistentRoleRespondsWith404NotFound() {
        client.get("/role/non-existent").expect404NotFound();
    }

    @Test
    void thatPutRoleWorks() {
        Role expectedRole = Role.newBuilder()
                .setRoleId("reader")
                .addAllPrivileges(List.of(Privilege.READ))
                .addAllNamespacePrefixes(List.of("/ns/1"))
                .setMaxValuation(Valuation.SHIELDED)
                .addAllStates(List.of(DatasetState.RAW, DatasetState.INPUT, DatasetState.PROCESSED))
                .build();
        ResponseHelper<String> helper = client.put("/role/reader", expectedRole).expect201Created();
        assertEquals("/role/reader", helper.response().headers().firstValue("Location").orElseThrow());
        Role role = readRole("reader");
        assertEquals(expectedRole, role);
    }

    @Test
    void thatCreateUpsertPutRoleWorks() {
        Role upsert_role = Role.newBuilder()
                .setRoleId("upsert_role")
                .addAllPrivileges(List.of(Privilege.CREATE, Privilege.READ))
                .addAllNamespacePrefixes(List.of("/a/b/c"))
                .setMaxValuation(Valuation.INTERNAL)
                .addAllStates(List.of(DatasetState.RAW, DatasetState.INPUT))
                .build();
        client.put("/role/upsert_role", upsert_role).expect201Created();
        Role role1 = readRole("upsert_role");
        assertEquals(List.of(Privilege.CREATE, Privilege.READ), role1.getPrivilegesList());
        Role another_upsert_role = Role.newBuilder()
                .setRoleId("upsert_role")
                .addAllPrivileges(List.of(Privilege.UPDATE, Privilege.DELETE))
                .addAllNamespacePrefixes(List.of("//d/e"))
                .setMaxValuation(Valuation.SHIELDED)
                .addAllStates(List.of(DatasetState.PROCESSED))
                .build();
        client.put("/role/upsert_role", another_upsert_role).expect201Created();
        Role role2 = readRole("upsert_role");
        assertEquals(List.of(Privilege.UPDATE, Privilege.DELETE), role2.getPrivilegesList());
    }

    @Test
    void thatDeleteRoleWorks() {
        createRole("role_to_be_deleted", List.of(Privilege.CREATE), List.of("/universe"), Valuation.SENSITIVE, List.of(DatasetState.INPUT));
        client.delete("/role/role_to_be_deleted").expect200Ok();
        assertNull(readRole("role_to_be_deleted"));
    }
}
