package no.ssb.datasetaccess.user;

import no.ssb.dapla.auth.dataset.protobuf.User;
import no.ssb.datasetaccess.UserAccessApplication;
import no.ssb.helidon.media.protobuf.ProtobufJsonUtils;
import no.ssb.testing.helidon.IntegrationTestExtension;
import no.ssb.testing.helidon.ResponseHelper;
import no.ssb.testing.helidon.TestClient;
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
class UserServiceHttpTest {

    @Inject
    UserAccessApplication application;

    @Inject
    TestClient client;

    @BeforeEach
    void clearUserRepository() throws InterruptedException, ExecutionException, TimeoutException {
        application.get(UserRepository.class).deleteAllUsers().get(3, TimeUnit.SECONDS);
    }

    User createUser(String userId, Iterable<String> roles) {
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
    void thatGetUserWorks() {
        User expected = createUser("john", List.of("reader"));
        User actual = ProtobufJsonUtils.toPojo(client.get("/user/john").expect200Ok().body(), User.class);
        assertEquals(expected, actual);
    }

    @Test
    void thatGetOnNonExistingUserReturns404() {
        client.get("/user/not-a-user").expect404NotFound();
    }

    @Test
    void thatPutWorks() {
        User expected = User.newBuilder().setUserId("john").addRoles("reader").build();
        ResponseHelper<String> helper = client.put("/user/john", expected).expect201Created();
        assertEquals("/user/john", helper.response().headers().firstValue("Location").orElseThrow());
        User user = getUser("john");
        assertEquals(expected, user);
    }

    @Test
    void thatUpsertWorks() {
        User upsert_user = User.newBuilder().setUserId("upsert_john").addRoles("reader").build();
        client.put("/user/upsert_john", upsert_user).expect201Created();
        User user1 = getUser("upsert_john");
        assertEquals(List.of("reader"), user1.getRolesList());
        client.put("/user/upsert_john", User.newBuilder().setUserId("upsert_john").addAllRoles(List.of("reader", "writer")).build()).expect201Created();
        User user2 = getUser("upsert_john");
        assertEquals(List.of("reader", "writer"), user2.getRolesList());
    }

    @Test
    void thatDeleteUserWorks() {
        createUser("user_to_be_deleted", List.of("some_role", "any_role"));
        client.delete("/user/user_to_be_deleted").expect200Ok();
        assertNull(getUser("user_to_be_deleted"));
    }
}
