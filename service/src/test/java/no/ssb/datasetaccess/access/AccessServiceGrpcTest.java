package no.ssb.datasetaccess.access;

import io.grpc.Channel;
import no.ssb.dapla.auth.dataset.protobuf.AccessCheckRequest;
import no.ssb.dapla.auth.dataset.protobuf.AccessCheckResponse;
import no.ssb.dapla.auth.dataset.protobuf.AuthServiceGrpc;
import no.ssb.datasetaccess.Application;
import no.ssb.datasetaccess.IntegrationTestExtension;
import no.ssb.datasetaccess.dataset.DatasetState;
import no.ssb.datasetaccess.dataset.Valuation;
import no.ssb.datasetaccess.role.Privilege;
import no.ssb.datasetaccess.role.Role;
import no.ssb.datasetaccess.role.RoleRepository;
import no.ssb.datasetaccess.user.User;
import no.ssb.datasetaccess.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.inject.Inject;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(IntegrationTestExtension.class)
class AccessServiceGrpcTest {

    @Inject
    Application application;

    @Inject
    Channel channel;

    void createUser(String userId, Set<String> roles) {
        try {
            User user = new User(userId, roles);
            application.get(UserRepository.class).createUser(user).get(3, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    void createRole(String roleId, Set<Privilege> privileges, Set<String> namespacePrefixes, Valuation maxValuation, Set<DatasetState> states) {
        Role role = new Role(roleId, privileges, new TreeSet<>(namespacePrefixes), maxValuation, states);
        try {
            application.get(RoleRepository.class).createRole(role).get(3, TimeUnit.SECONDS);
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
                .setValuation(Valuation.OPEN.name())
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
                .setValuation(Valuation.OPEN.name())
                .setState(DatasetState.INPUT.name())
                .build());
        assertFalse(response.getAllowed());
    }
}
