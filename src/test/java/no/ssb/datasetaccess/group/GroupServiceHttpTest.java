package no.ssb.datasetaccess.group;

import no.ssb.dapla.auth.dataset.protobuf.Group;
import no.ssb.datasetaccess.UserAccessApplication;
import no.ssb.helidon.media.protobuf.ProtobufJsonUtils;
import no.ssb.testing.helidon.IntegrationTestExtension;
import no.ssb.testing.helidon.ResponseHelper;
import no.ssb.testing.helidon.TestClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@ExtendWith(IntegrationTestExtension.class)
class GroupServiceHttpTest {
    private static final Logger LOG = LoggerFactory.getLogger(GroupServiceHttpTest.class);

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
        return application.get(GroupRepository.class).getGroup(groupId).await();
    }

    @Test
    void thatGetGroupWorks() {
        Group expected = createGroup("Group1", "This is the first group", List.of("reader"));
        Group actual = ProtobufJsonUtils.toPojo(client.get("/group/Group1").expect200Ok().body(), Group.class);
        assertEquals(expected, actual);
    }

    @Test
    void thatGetAllGroupsWorks() throws JSONException {
        String getResult = client.get("/group").expect200Ok().body();

        JSONObject expected = new JSONObject();
        JSONArray groups = new JSONArray();
        expected.put("groups", groups);
        JSONAssert.assertEquals(expected, new JSONObject(getResult), JSONCompareMode.LENIENT);

        Group group1 = createGroup("group1", "This is the first group", List.of("reader"));
        Group group2 = createGroup("group2", "This is the second group", List.of("writer"));
        Group group3 = createGroup("group3", "This is the third group", List.of("reader"));
        getResult = client.get("/group").expect200Ok().body();

        groups.put(new JSONObject(ProtobufJsonUtils.toString(group1)));
        groups.put(new JSONObject(ProtobufJsonUtils.toString(group2)));
        groups.put(new JSONObject(ProtobufJsonUtils.toString(group3)));

        JSONAssert.assertEquals(expected, new JSONObject(getResult), JSONCompareMode.LENIENT);
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
