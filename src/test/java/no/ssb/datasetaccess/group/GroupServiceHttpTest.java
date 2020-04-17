package no.ssb.datasetaccess.group;

import no.ssb.dapla.auth.dataset.protobuf.*;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(IntegrationTestExtension.class)
class GroupServiceHttpTest {

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

    @Test
    void thatGetGroupWorks() {
        Group expected = createGroup("Group1", "This is the first group", List.of("reader"));
        Group actual = ProtobufJsonUtils.toPojo(client.get("/group/Group1").expect200Ok().body(), Group.class);
        assertEquals(expected, actual);
    }

    @Test
    void thatGetAllGroupsWorks() {
        String groupJson = client.get("/group").expect200Ok().body();
        assertEquals("{\"groups\": []}", groupJson);

        Group group1 = createGroup("group1", "This is the first group", List.of("reader"));
        Group group2 = createGroup("group2", "This is the second group", List.of("writer"));
        Group group3 = createGroup("group3", "This is the third group", List.of("reader"));
        groupJson = client.get("/group").expect200Ok().body();
        assertNotNull(groupJson);
        assertTrue(groupJson.contains("group1"));
        assertTrue(groupJson.contains(ProtobufJsonUtils.toString(group2)));
        assertTrue(groupJson.contains(ProtobufJsonUtils.toString(group3)));
    }

    @Test
    void thatGetOnNonExistingGroupReturns404() {
        client.get("/group/not-a-group").expect404NotFound();
    }

    @Test
    void thatPutGroupWorks() {
        Group expected = Group.newBuilder().setGroupId("Group1").addRoles("reader").build();
        ResponseHelper<String> helper = client.put("/group/Group1", expected).expect201Created();
        assertEquals("/group/Group1", helper.response().headers().firstValue("Location").orElseThrow());
        Group group = getGroup("Group1");
        assertEquals(expected, group);
    }

    @Test
    void thatUpsertGroupWorks() {
        Group upsert_group = Group.newBuilder().setGroupId("upsert_group").addRoles("reader").build();
        client.put("/group/upsert_group", upsert_group).expect201Created();
        Group group1 = getGroup("upsert_group");
        assertEquals(List.of("reader"), group1.getRolesList());
        client.put("/group/upsert_group", Group.newBuilder().setGroupId("upsert_group").addAllRoles(List.of("reader", "writer")).build()).expect201Created();
        Group group2 = getGroup("upsert_group");
        assertEquals(List.of("reader", "writer"), group2.getRolesList());
    }

    @Test
    void thatDeleteGroupWorks() {
        createGroup("group_to_be_deleted", "Group that will get deleted", List.of("some_role", "any_role"));
        client.delete("/group/group_to_be_deleted").expect200Ok();
        assertNull(getGroup("group_to_be_deleted"));
    }
}
