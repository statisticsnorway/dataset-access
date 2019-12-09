package no.ssb.datasetaccess.access;

import no.ssb.datasetaccess.Application;
import no.ssb.datasetaccess.IntegrationTestExtension;
import no.ssb.datasetaccess.TestClient;
import no.ssb.datasetaccess.role.RoleRepository;
import no.ssb.datasetaccess.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.inject.Inject;
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

    @Test
    void thatGetAccessOnNonExistingUserReturns403() {
        client.get("/access/does_not_exist?privilege=READ&namespace=a&valuation=SENSITIVE&state=RAW").expect403Forbidden();
    }
}
