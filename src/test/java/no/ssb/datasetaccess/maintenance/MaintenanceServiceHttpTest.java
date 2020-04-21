package no.ssb.datasetaccess.maintenance;

import no.ssb.dapla.auth.dataset.protobuf.DatasetState;
import no.ssb.dapla.auth.dataset.protobuf.DatasetStateSet;
import no.ssb.dapla.auth.dataset.protobuf.Group;
import no.ssb.dapla.auth.dataset.protobuf.PathSet;
import no.ssb.dapla.auth.dataset.protobuf.Privilege;
import no.ssb.dapla.auth.dataset.protobuf.PrivilegeSet;
import no.ssb.dapla.auth.dataset.protobuf.Role;
import no.ssb.dapla.auth.dataset.protobuf.User;
import no.ssb.dapla.auth.dataset.protobuf.Valuation;
import no.ssb.datasetaccess.UserAccessApplication;
import no.ssb.datasetaccess.group.GroupRepository;
import no.ssb.datasetaccess.role.RoleRepository;
import no.ssb.datasetaccess.user.UserRepository;
import no.ssb.testing.helidon.IntegrationTestExtension;
import no.ssb.testing.helidon.TestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@ExtendWith(IntegrationTestExtension.class)
class MaintenanceServiceHttpTest {

    @Inject
    UserAccessApplication application;

    @Inject
    TestClient client;

    @BeforeEach
    void clearGroupRepository() throws InterruptedException, ExecutionException, TimeoutException {
        application.get(GroupRepository.class).deleteAllGroups().get(3, TimeUnit.SECONDS);
    }

    Group createGroup(String groupId, String description, Iterable<String> roles) {
        try {
            Group group = Group.newBuilder().setGroupId(groupId).setDescription(description).addAllRoles(roles).build();
            application.get(GroupRepository.class).createOrUpdateGroup(group).get(3, TimeUnit.SECONDS);
            return group;
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    Group getGroup(String groupId) {
        return application.get(GroupRepository.class).getGroup(groupId).join();
    }

    Role buildRole(String roleId, Iterable<Privilege> privilegeIncludes, Iterable<String> pathIncludes, Valuation maxValuation, Iterable<DatasetState> stateIncludes) {
        return Role.newBuilder()
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
    }

    Role createRole(String roleId, Iterable<Privilege> privilegeIncludes, Iterable<String> pathIncludes, Valuation maxValuation, Iterable<DatasetState> stateIncludes) {
        Role role = buildRole(roleId, privilegeIncludes, pathIncludes, maxValuation, stateIncludes);
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

    User createUser(String userId, Iterable<String> groups, Iterable<String> roles) {
        try {
            User user = User.newBuilder().setUserId(userId).addAllRoles(roles).build();
            application.get(UserRepository.class).createOrUpdateUser(user).get(3, TimeUnit.SECONDS);
            return user;
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    User getUser(String userId) {
        return application.get(UserRepository.class).getUser(userId).join();
    }

    @Test
    void thatDeleteAllWorks() {
        createRole("reader", List.of(Privilege.READ), List.of("/universe"), Valuation.SENSITIVE, List.of(DatasetState.INPUT));
        createRole("writer", List.of(Privilege.CREATE, Privilege.UPDATE, Privilege.DELETE), List.of("/universe"), Valuation.SENSITIVE, List.of(DatasetState.INPUT));

        createGroup("readers", "This is the reader group", List.of("reader"));
        createGroup("writers", "This is the writer group", List.of("writer"));

        createUser("john", List.of("readers"), List.of());
        createUser("jane", List.of("writers"), List.of());

        assertNotNull(readRole("reader"));
        assertNotNull(readRole("writer"));
        assertNotNull(getGroup("readers"));
        assertNotNull(getGroup("writers"));
        assertNotNull(getUser("john"));
        assertNotNull(getUser("jane"));

        client.delete("/maintenance/all");

        assertNull(readRole("reader"));
        assertNull(readRole("writer"));
        assertNull(getGroup("readers"));
        assertNull(getGroup("writers"));
        assertNull(getUser("john"));
        assertNull(getUser("jane"));
    }
}
