package no.ssb.datasetaccess.user;

import no.ssb.datasetaccess.JacksonUtils;
import no.ssb.datasetaccess.testing.IntegrationTestExtension;
import no.ssb.datasetaccess.testing.ResponseHelper;
import no.ssb.datasetaccess.testing.TestClient;
import no.ssb.datasetaccess.testing.TestServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.inject.Inject;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@ExtendWith(IntegrationTestExtension.class)
class UserServiceTest {

    @Inject
    TestServer server;

    @Inject
    TestClient client;

    @BeforeEach
    void clearUserRepository() throws InterruptedException, ExecutionException, TimeoutException {
        server.getApplication().getUserRepository().deleteAllUsers().get(3, TimeUnit.SECONDS);
    }

    User createUser(String userId, Set<String> roles) {
        try {
            User user = new User(userId, roles);
            server.getApplication().getUserRepository().createUser(user).get(3, TimeUnit.SECONDS);
            return user;
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    User getUser(String userId) {
        return server.getApplication().getUserRepository().getUser(userId).join();
    }

    @Test
    void thatGetUserWorks() {
        User expected = createUser("john", Set.of("reader"));
        User actual = JacksonUtils.toPojo(client.get("/user/john").expect200Ok().body(), User.class);
        assertEquals(expected, actual);
    }

    @Test
    void thatGetOnNonExistingUserReturns404() {
        client.get("/user/not-a-user").expect404NotFound();
    }

    @Test
    void thatPutWorks() {
        User expected = new User("john", Set.of("reader"));
        ResponseHelper<String> helper = client.put("/user/john", expected).expect201Created();
        assertEquals("/user/john", helper.response().headers().firstValue("Location").orElseThrow());
        User user = getUser("john");
        assertEquals(expected, user);
    }

    @Test
    void thatUpsertWorks() {
        User upsert_user = new User("upsert_john", Set.of("reader"));
        client.put("/user/upsert_john", upsert_user).expect201Created();
        User user1 = getUser("upsert_john");
        assertEquals(Set.of("reader"), user1.getRoles());
        client.put("/user/upsert_john", new User("upsert_john", Set.of("reader", "writer"))).expect201Created();
        User user2 = getUser("upsert_john");
        assertEquals(Set.of("reader", "writer"), user2.getRoles());
    }

    @Test
    void thatDeleteUserWorks() {
        createUser("user_to_be_deleted", Set.of("some_role", "any_role"));
        client.delete("/user/user_to_be_deleted").expect200Ok();
        assertNull(getUser("user_to_be_deleted"));
    }
}
