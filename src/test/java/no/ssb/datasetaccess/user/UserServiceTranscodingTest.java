package no.ssb.datasetaccess.user;

import no.ssb.dapla.auth.dataset.protobuf.User;
import no.ssb.dapla.auth.dataset.protobuf.UserDeleteRequest;
import no.ssb.dapla.auth.dataset.protobuf.UserGetRequest;
import no.ssb.dapla.auth.dataset.protobuf.UserGetResponse;
import no.ssb.dapla.auth.dataset.protobuf.UserPutRequest;
import no.ssb.datasetaccess.Application;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

@ExtendWith(IntegrationTestExtension.class)
class UserServiceTranscodingTest {

    @Inject
    Application application;

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
        User actual = client.post("/rpc/UserService/getUser", UserGetRequest.newBuilder().setUserId("john").build(), UserGetResponse.class).expect200Ok().body().getUser();
        assertEquals(expected, actual);
    }

    @Test
    void thatGetOnNonExistingUserReturns404() {
        assertFalse(client.post("/rpc/UserService/getUser", UserGetRequest.newBuilder().setUserId("not-a-user").build(), UserGetResponse.class).expect200Ok().body().hasUser());
    }

    @Test
    void thatPutWorks() {
        User expected = User.newBuilder().setUserId("john").addRoles("reader").build();
        client.post("/rpc/UserService/putUser", UserPutRequest.newBuilder().setUser(expected).build()).expect200Ok();
        User user = getUser("john");
        assertEquals(expected, user);
    }

    @Test
    void thatUpsertWorks() {
        User upsert_user = User.newBuilder().setUserId("upsert_john").addRoles("reader").build();
        client.post("/rpc/UserService/putUser", UserPutRequest.newBuilder().setUser(upsert_user).build()).expect200Ok();
        User user1 = getUser("upsert_john");
        assertEquals(List.of("reader"), user1.getRolesList());
        client.post("/rpc/UserService/putUser", UserPutRequest.newBuilder().setUser(User.newBuilder().setUserId("upsert_john").addAllRoles(List.of("reader", "writer")).build()).build()).expect200Ok();
        User user2 = getUser("upsert_john");
        assertEquals(List.of("reader", "writer"), user2.getRolesList());
    }

    @Test
    void thatDeleteUserWorks() {
        createUser("user_to_be_deleted", List.of("some_role", "any_role"));
        client.post("/rpc/UserService/deleteUser", UserDeleteRequest.newBuilder().setUserId("user_to_be_deleted")).expect200Ok();
        assertNull(getUser("user_to_be_deleted"));
    }
}
