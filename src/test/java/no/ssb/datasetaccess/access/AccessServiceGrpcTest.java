package no.ssb.datasetaccess.access;

import io.grpc.Channel;
import no.ssb.dapla.auth.dataset.protobuf.AccessCheckRequest;
import no.ssb.dapla.auth.dataset.protobuf.AccessCheckResponse;
import no.ssb.dapla.auth.dataset.protobuf.AuthServiceGrpc;
import no.ssb.dapla.auth.dataset.protobuf.Role;
import no.ssb.dapla.auth.dataset.protobuf.Role.DatasetState;
import no.ssb.dapla.auth.dataset.protobuf.Role.Privilege;
import no.ssb.dapla.auth.dataset.protobuf.Role.Valuation;
import no.ssb.dapla.auth.dataset.protobuf.User;
import no.ssb.datasetaccess.UserAccessApplication;
import no.ssb.datasetaccess.role.RoleRepository;
import no.ssb.datasetaccess.user.UserRepository;
import no.ssb.testing.helidon.IntegrationTestExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.inject.Inject;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(IntegrationTestExtension.class)
class AccessServiceGrpcTest {

    @Inject
    UserAccessApplication application;

    @Inject
    Channel channel;

    void createUser(String userId, Set<String> roles) {
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
    public void thatHasAccessRepliesWithSuccessWhenMatchingRoleAndUserExists() {
        createUser("john", Set.of("reader"));
        createRole("reader", Set.of(Privilege.READ), Set.of("/a"), Valuation.INTERNAL, Set.of(DatasetState.RAW, DatasetState.INPUT));

        AuthServiceGrpc.AuthServiceBlockingStub client = AuthServiceGrpc.newBlockingStub(channel);

        AccessCheckResponse response = client.hasAccess(AccessCheckRequest.newBuilder()
                .setUserId("john")
                .setPrivilege(Privilege.READ.name())
                .setNamespace("/a/b/c")
                .setValuation(InternalValuation.OPEN.name())
                .setState(DatasetState.INPUT.name())
                .build());
        assertTrue(response.getAllowed());
    }

    @Test
    public void thatHasAccessRepliesWithNotAllowedForNonExistentUser() {
        AuthServiceGrpc.AuthServiceBlockingStub client = AuthServiceGrpc.newBlockingStub(channel);

        AccessCheckResponse response = client.hasAccess(AccessCheckRequest.newBuilder()
                .setUserId("non_existent_user")
                .setPrivilege(Privilege.READ.name())
                .setNamespace("/no/such/dataset")
                .setValuation(InternalValuation.OPEN.name())
                .setState(DatasetState.INPUT.name())
                .build());
        assertFalse(response.getAllowed());
    }
}
