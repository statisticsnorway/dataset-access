package no.ssb.datasetaccess.access;

import no.ssb.dapla.auth.dataset.protobuf.AccessCheckRequest;
import no.ssb.dapla.auth.dataset.protobuf.AccessCheckResponse;
import no.ssb.dapla.auth.dataset.protobuf.DatasetState;
import no.ssb.dapla.auth.dataset.protobuf.DatasetStateSet;
import no.ssb.dapla.auth.dataset.protobuf.PathSet;
import no.ssb.dapla.auth.dataset.protobuf.Privilege;
import no.ssb.dapla.auth.dataset.protobuf.PrivilegeSet;
import no.ssb.dapla.auth.dataset.protobuf.Role;
import no.ssb.dapla.auth.dataset.protobuf.User;
import no.ssb.dapla.auth.dataset.protobuf.Valuation;
import no.ssb.datasetaccess.UserAccessApplication;
import no.ssb.datasetaccess.role.RoleRepository;
import no.ssb.datasetaccess.user.UserRepository;
import no.ssb.testing.helidon.IntegrationTestExtension;
import no.ssb.testing.helidon.TestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(IntegrationTestExtension.class)
class AccessServiceTranscodingTest {

    @Inject
    UserAccessApplication application;

    @Inject
    TestClient client;

    @BeforeEach
    void clearRepositories() throws InterruptedException, ExecutionException, TimeoutException {
        CompletableFuture.allOf(
                application.get(UserRepository.class).deleteAllUsers(),
                application.get(RoleRepository.class).deleteAllRoles()
        ).get(3, TimeUnit.SECONDS);
    }

    void createUser(String userId, Iterable<String> roles) {
        try {
            User user = User.newBuilder().setUserId(userId).addAllRoles(roles).build();
            application.get(UserRepository.class).createOrUpdateUser(user).get(3, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    void createRole(String roleId, Iterable<Privilege> privilegeIncludes, Iterable<String> pathIncludes, Valuation maxValuation, Iterable<DatasetState> stateIncludes) {
        Role role = Role.newBuilder()
                .setRoleId(roleId)
                .setPrivileges(PrivilegeSet.newBuilder()
                        .addAllIncludes(privilegeIncludes)
                        .build())
                .setPaths(PathSet.newBuilder()
                        .addAllIncludes(pathIncludes)
                        .build())
                .setMaxValuation(maxValuation)
                .setStates(DatasetStateSet.newBuilder()
                        .addAllIncludes(stateIncludes)
                        .build())
                .build();
        try {
            application.get(RoleRepository.class).createOrUpdateRole(role).get(3, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void thatGetAccessWorks() {
        createUser("john_can_update", List.of("updater"));
        createRole("updater", List.of(Privilege.UPDATE), List.of("/ns/test"), Valuation.INTERNAL, List.of(DatasetState.RAW, DatasetState.INPUT));
        assertTrue(client.post("/rpc/AuthService/hasAccess", AccessCheckRequest.newBuilder().setUserId("john_can_update").setPrivilege("UPDATE").setNamespace("/ns/test").setValuation("INTERNAL").setState("RAW").build(), AccessCheckResponse.class).expect200Ok().body().getAllowed());
    }

    @Test
    void GetAccessWhenUserDoesntHaveTheAppropriateNamespaceReturns403() {
        createUser("john_with_missing_ns", List.of("creator"));
        createRole("creator", List.of(Privilege.CREATE), List.of("/ns/test/a", "/test"), Valuation.OPEN, List.of(DatasetState.OTHER));
        assertFalse(client.post("/rpc/AuthService/hasAccess", AccessCheckRequest.newBuilder().setUserId("john_with_missing_ns").setPrivilege("CREATE").setNamespace("/ns/test").setValuation("OPEN").setState("OTHER").build(), AccessCheckResponse.class).expect200Ok().body().getAllowed());
    }

    @Test
    void thatGetAccessWhenUserDoesntHaveTheAppropriatePrivilegeReturns403() {
        createUser("john_cant_delete", List.of("updater"));
        createRole("updater", List.of(Privilege.UPDATE), List.of("/ns/test"), Valuation.SHIELDED, List.of(DatasetState.PROCESSED));
        assertFalse(client.post("/rpc/AuthService/hasAccess", AccessCheckRequest.newBuilder().setUserId("john_cant_delete").setPrivilege("DELETE").setNamespace("/ns/test").setValuation("SHIELDED").setState("PROCESSED").build(), AccessCheckResponse.class).expect200Ok().body().getAllowed());
    }

    @Test
    void thatGetAccessOnUserWithoutAppropriateRolesReturns403() {
        assertFalse(client.post("/rpc/AuthService/hasAccess", AccessCheckRequest.newBuilder().setUserId("john_without_roles").setPrivilege("DELETE").setNamespace("test").setValuation("SENSITIVE").setState("RAW").build(), AccessCheckResponse.class).expect200Ok().body().getAllowed());
    }

    @Test
    void thatGetAccessOnNonExistingUserReturns403() {
        assertFalse(client.post("/rpc/AuthService/hasAccess", AccessCheckRequest.newBuilder().setUserId("does_not_exist").setPrivilege("READ").setNamespace("a").setValuation("SENSITIVE").setState("RAW").build(), AccessCheckResponse.class).expect200Ok().body().getAllowed());
    }

    @Test
    void thatGetAccessForUserWithMoreThanOneRoleWorks() {
        createUser("john_two_roles", List.of("updater", "reader"));
        createRole("updater", List.of(Privilege.UPDATE), List.of("/ns/test"), Valuation.INTERNAL, List.of(DatasetState.RAW, DatasetState.INPUT));
        createRole("reader", List.of(Privilege.READ), List.of("/ns/test"), Valuation.INTERNAL, List.of(DatasetState.RAW, DatasetState.INPUT));
        assertTrue(client.post("/rpc/AuthService/hasAccess", AccessCheckRequest.newBuilder().setUserId("john_two_roles").setPrivilege("UPDATE").setNamespace("/ns/test").setValuation("INTERNAL").setState("RAW").build(), AccessCheckResponse.class).expect200Ok().body().getAllowed());
        assertTrue(client.post("/rpc/AuthService/hasAccess", AccessCheckRequest.newBuilder().setUserId("john_two_roles").setPrivilege("READ").setNamespace("/ns/test").setValuation("INTERNAL").setState("RAW").build(), AccessCheckResponse.class).expect200Ok().body().getAllowed());
        assertFalse(client.post("/rpc/AuthService/hasAccess", AccessCheckRequest.newBuilder().setUserId("john_two_roles").setPrivilege("DELETE").setNamespace("/ns/test").setValuation("INTERNAL").setState("RAW").build(), AccessCheckResponse.class).expect200Ok().body().getAllowed());
    }
}
