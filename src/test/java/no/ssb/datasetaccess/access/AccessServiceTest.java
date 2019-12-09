package no.ssb.datasetaccess.access;

import no.ssb.datasetaccess.Application;
import no.ssb.datasetaccess.IntegrationTestExtension;
import no.ssb.datasetaccess.TestClient;
import no.ssb.datasetaccess.dataset.DatasetState;
import no.ssb.datasetaccess.dataset.Valuation;
import no.ssb.datasetaccess.role.Privilege;
import no.ssb.datasetaccess.role.Role;
import no.ssb.datasetaccess.role.RoleRepository;
import no.ssb.datasetaccess.user.User;
import no.ssb.datasetaccess.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.inject.Inject;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@ExtendWith(IntegrationTestExtension.class)
class AccessServiceTest {

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

    User createUser(String userId, Set<String> roles) {
        try {
            User user = new User(userId, roles);
            application.get(UserRepository.class).createUser(user).get(3, TimeUnit.SECONDS);
            return user;
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    User getUser(String userId) {
        return application.get(UserRepository.class).getUser(userId).join();
    }

    Role createRole(String roleId, Set<Privilege> privileges, Set<String> namespacePrefixes, Valuation maxValuation, Set<DatasetState> states) {
        Role role = new Role(roleId, privileges, new TreeSet<>(namespacePrefixes), maxValuation, states);
        try {
            application.get(RoleRepository.class).createRole(role).get(3, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new RuntimeException(e);
        }
        return role;
    }

    Role getRole(String roleId) {
        return application.get(RoleRepository.class).getRole(roleId).join();
    }

    @Test
    void thatGetAccessOnUserWithoutAppropriateRolesReturns403() {
        createUser("john", Set.of("reader"));
        client.get("/access/john?privilege=DELETE&namespace=test&valuation=SENSITIVE&state=RAW").expect403Forbidden();
    }

    @Test
    void thatGetAccessOnNonExistingUserReturns403() {
        client.get("/access/does_not_exist?privilege=READ&namespace=a&valuation=SENSITIVE&state=RAW").expect403Forbidden();
    }
}
