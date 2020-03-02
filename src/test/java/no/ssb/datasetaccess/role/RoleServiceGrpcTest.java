package no.ssb.datasetaccess.role;

import io.grpc.Channel;
import no.ssb.dapla.auth.dataset.protobuf.Role;
import no.ssb.dapla.auth.dataset.protobuf.Role.DatasetState;
import no.ssb.dapla.auth.dataset.protobuf.Role.Privilege;
import no.ssb.dapla.auth.dataset.protobuf.Role.Valuation;
import no.ssb.dapla.auth.dataset.protobuf.RoleDeleteRequest;
import no.ssb.dapla.auth.dataset.protobuf.RoleGetRequest;
import no.ssb.dapla.auth.dataset.protobuf.RoleGetResponse;
import no.ssb.dapla.auth.dataset.protobuf.RolePutRequest;
import no.ssb.dapla.auth.dataset.protobuf.RoleServiceGrpc;
import no.ssb.datasetaccess.UserAccessApplication;
import no.ssb.testing.helidon.IntegrationTestExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

@ExtendWith(IntegrationTestExtension.class)
class RoleServiceGrpcTest {

    @Inject
    UserAccessApplication application;

    @Inject
    Channel channel;

    RoleServiceGrpc.RoleServiceBlockingStub client;

    @BeforeEach
    void clearRoleRepository() throws InterruptedException, ExecutionException, TimeoutException {
        application.get(RoleRepository.class).deleteAllRoles().get(3, TimeUnit.SECONDS);
        client = RoleServiceGrpc.newBlockingStub(channel);
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
        Role role = client.getRole(RoleGetRequest.newBuilder().setRoleId("writer").build()).getRole();
        assertEquals(expectedRole, role);
    }

    @Test
    void thatGetNonExistentRoleRespondsWith404NotFound() {
        RoleGetResponse getRoleResponse = client.getRole(RoleGetRequest.newBuilder().setRoleId("non-existent").build());
        assertFalse(getRoleResponse.hasRole());
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
        client.putRole(RolePutRequest.newBuilder().setRole(expectedRole).build());
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
        client.putRole(RolePutRequest.newBuilder().setRole(upsert_role).build());
        Role role1 = readRole("upsert_role");
        assertEquals(List.of(Privilege.CREATE, Privilege.READ), role1.getPrivilegesList());
        Role another_upsert_role = Role.newBuilder()
                .setRoleId("upsert_role")
                .addAllPrivileges(List.of(Privilege.UPDATE, Privilege.DELETE))
                .addAllNamespacePrefixes(List.of("//d/e"))
                .setMaxValuation(Valuation.SHIELDED)
                .addAllStates(List.of(DatasetState.PROCESSED))
                .build();
        client.putRole(RolePutRequest.newBuilder().setRole(another_upsert_role).build());
        Role role2 = readRole("upsert_role");
        assertEquals(List.of(Privilege.UPDATE, Privilege.DELETE), role2.getPrivilegesList());
    }

    @Test
    void thatDeleteRoleWorks() {
        createRole("role_to_be_deleted", List.of(Privilege.CREATE), List.of("/universe"), Valuation.SENSITIVE, List.of(DatasetState.INPUT));

        client.deleteRole(RoleDeleteRequest.newBuilder().setRoleId("role_to_be_deleted").buildPartial());
        Role role_to_be_deleted = readRole("role_to_be_deleted");
        assertNull(role_to_be_deleted);
    }
}
