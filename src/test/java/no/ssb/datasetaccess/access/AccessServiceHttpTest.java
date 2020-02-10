package no.ssb.datasetaccess.access;

import no.ssb.dapla.auth.dataset.protobuf.AccessCheckRequest;
import no.ssb.dapla.auth.dataset.protobuf.AccessCheckResponse;
import no.ssb.dapla.auth.dataset.protobuf.Role;
import no.ssb.dapla.auth.dataset.protobuf.Role.DatasetState;
import no.ssb.dapla.auth.dataset.protobuf.Role.Privilege;
import no.ssb.dapla.auth.dataset.protobuf.Role.Valuation;
import no.ssb.dapla.auth.dataset.protobuf.User;
import no.ssb.datasetaccess.Application;
import no.ssb.datasetaccess.role.RoleRepository;
import no.ssb.datasetaccess.user.UserRepository;
import no.ssb.helidon.media.protobuf.ProtobufJsonUtils;
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

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(IntegrationTestExtension.class)
class AccessServiceHttpTest {

    @Inject
    Application application;

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

    void createRole(String roleId, Iterable<Privilege> privileges, Iterable<String> namespacePrefixes, Valuation maxValuation, Iterable<DatasetState> states) {
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
    }

    @Test
    void thatGetAccessWorks() {
        createUser("john_can_update", List.of("updater"));
        createRole("updater", List.of(Privilege.UPDATE), List.of("/ns/test"), Valuation.INTERNAL, List.of(DatasetState.RAW, DatasetState.INPUT));
        client.get("/access/john_can_update?privilege=UPDATE&namespace=/ns/test&valuation=INTERNAL&state=RAW").expect200Ok();
    }

    @Test
    void GetAccessWhenUserDoesntHaveTheAppropriateNamespaceReturns403() {
        createUser("john_with_missing_ns", List.of("creator"));
        createRole("creator", List.of(Privilege.CREATE), List.of("/ns/test/a", "/test"), Valuation.OPEN, List.of(DatasetState.OTHER));
        client.get("/access/john_with_missing_ns?privilege=CREATE&namespace=/ns/test&valuation=OPEN&state=OTHER").expect403Forbidden();
    }

    @Test
    void thatGetAccessWhenUserDoesntHaveTheAppropriatePrivilegeReturns403() {
        createUser("john_cant_delete", List.of("updater"));
        createRole("updater", List.of(Privilege.UPDATE), List.of("/ns/test"), Valuation.SHIELDED, List.of(DatasetState.PROCESSED));
        client.get("/access/john_cant_delete?privilege=DELETE&namespace=/ns/test&valuation=SHIELDED&state=PROCESSED").expect403Forbidden();
    }

    @Test
    void thatGetAccessOnUserWithoutAppropriateRolesReturns403() {
        createUser("john_without_roles", List.of("reader"));
        client.get("/access/john_without_roles?privilege=DELETE&namespace=test&valuation=SENSITIVE&state=RAW").expect403Forbidden();
    }

    @Test
    void thatGetAccessOnNonExistingUserReturns403() {
        client.get("/access/does_not_exist?privilege=READ&namespace=a&valuation=SENSITIVE&state=RAW").expect403Forbidden();
    }

    @Test
    void thatGetAccessForUserWithMoreThanOneRoleWorks() {
        createUser("john_two_roles", List.of("updater", "reader"));
        createRole("updater", List.of(Privilege.UPDATE), List.of("/ns/test"), Valuation.INTERNAL, List.of(DatasetState.RAW, DatasetState.INPUT));
        createRole("reader", List.of(Privilege.READ), List.of("/ns/test"), Valuation.INTERNAL, List.of(DatasetState.RAW, DatasetState.INPUT));
        client.get("/access/john_two_roles?privilege=UPDATE&namespace=/ns/test&valuation=INTERNAL&state=RAW").expect200Ok();
        client.get("/access/john_two_roles?privilege=READ&namespace=/ns/test&valuation=INTERNAL&state=RAW").expect200Ok();
        client.get("/access/john_two_roles?privilege=DELETE&namespace=/ns/test&valuation=INTERNAL&state=RAW").expect403Forbidden();
    }

    @Test
    void thatGrpcTranscodingWorks() {
        createUser("transcoding_user", List.of("updater"));
        createRole("updater", List.of(Privilege.UPDATE, Privilege.READ), List.of("/ns/test"), Valuation.INTERNAL, List.of(DatasetState.RAW, DatasetState.INPUT));
        AccessCheckRequest request = AccessCheckRequest.newBuilder()
                .setUserId("transcoding_user")
                .setPrivilege(Privilege.READ.name())
                .setNamespace("/ns/test")
                .setValuation(Valuation.INTERNAL.name())
                .setState(DatasetState.RAW.name())
                .build();
        String jsonResponse = client.post("/rpc/AuthService/hasAccess", request).expect200Ok().body();
        System.out.printf("body: %s%n", jsonResponse);
        AccessCheckResponse response = ProtobufJsonUtils.toPojo(jsonResponse, AccessCheckResponse.class);
        assertTrue(response.getAllowed());
    }
}
