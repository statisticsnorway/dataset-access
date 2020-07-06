package no.ssb.datasetaccess.role;

import no.ssb.dapla.auth.dataset.protobuf.DatasetState;
import no.ssb.dapla.auth.dataset.protobuf.DatasetStateSet;
import no.ssb.dapla.auth.dataset.protobuf.PathSet;
import no.ssb.dapla.auth.dataset.protobuf.Privilege;
import no.ssb.dapla.auth.dataset.protobuf.PrivilegeSet;
import no.ssb.dapla.auth.dataset.protobuf.Role;
import no.ssb.dapla.auth.dataset.protobuf.Valuation;
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
class RoleServiceHttpTest {
    private static final Logger LOG = LoggerFactory.getLogger(RoleServiceHttpTest.class);

    @Inject
    UserAccessApplication application;

    @Inject
    TestClient client;

    @BeforeEach
    void clearRoleRepository() throws InterruptedException, ExecutionException, TimeoutException {
        application.get(RoleRepository.class).deleteAllRoles().get(3, TimeUnit.SECONDS);
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
        Role role = buildRole(roleId, privilegeIncludes, pathIncludes, maxValuation,  stateIncludes);
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

    @Test
    void thatGetRoleWorks() {
        Role expectedRole = createRole("writer", List.of(Privilege.CREATE, Privilege.UPDATE),
                List.of("/ns/test"), Valuation.INTERNAL, List.of(DatasetState.RAW, DatasetState.INPUT));
        Role role = client.get("/role/writer", Role.class).expect200Ok().body();
        assertEquals(expectedRole, role);
    }

    @Test
    void thatGetAllRoleWorks() throws JSONException {
        String getResult = client.get("/role").expect200Ok().body();

        JSONObject expected = new JSONObject();
        JSONArray roles = new JSONArray();
        expected.put("roles", roles);
        JSONAssert.assertEquals(expected, new JSONObject(getResult), JSONCompareMode.LENIENT);

        Role role1 = createRole("writer1", List.of(Privilege.CREATE, Privilege.UPDATE),
                List.of("/ns/test"), Valuation.INTERNAL, List.of(DatasetState.RAW, DatasetState.INPUT));
        Role role2 = createRole("writer2", List.of(Privilege.CREATE, Privilege.UPDATE),
                List.of("/ns/test"), Valuation.INTERNAL, List.of(DatasetState.PROCESSED));
        Role role3 = createRole("reader1", List.of(Privilege.CREATE, Privilege.UPDATE),
                List.of("/ns/test2"), Valuation.INTERNAL, List.of(DatasetState.RAW, DatasetState.INPUT));
        getResult = client.get("/role").expect200Ok().body();


        roles.put(new JSONObject(ProtobufJsonUtils.toString(role1)));
        roles.put(new JSONObject(ProtobufJsonUtils.toString(role2)));
        roles.put(new JSONObject(ProtobufJsonUtils.toString(role3)));

        JSONAssert.assertEquals(expected, new JSONObject(getResult), JSONCompareMode.LENIENT);


    }

    @Test
    void thatGetNonExistentRoleRespondsWith404NotFound() {
        client.get("/role/non-existent").expect404NotFound();
    }

    @Test
    void thatPutRoleWorks() {
        Role expectedRole = buildRole("reader", List.of(Privilege.READ), List.of("/ns/1"),
                Valuation.SHIELDED, List.of(DatasetState.RAW, DatasetState.INPUT, DatasetState.PROCESSED));
        ResponseHelper<String> helper = client.put("/role/reader", expectedRole).expect201Created();
        assertEquals("/role/reader", helper.response().headers().firstValue("Location").orElseThrow());
        Role role = readRole("reader");
        assertEquals(expectedRole, role);
    }

    @Test
    void thatCreateUpsertPutRoleWorks() {
        Role upsert_role = buildRole("upsert_role", List.of(Privilege.CREATE, Privilege.READ),
                        List.of("/a/b/c"), Valuation.INTERNAL, List.of(DatasetState.RAW, DatasetState.INPUT));
        client.put("/role/upsert_role", upsert_role).expect201Created();
        Role role1 = readRole("upsert_role");
        assertEquals(List.of(Privilege.CREATE, Privilege.READ), role1.getPrivileges().getIncludesList());

        Role another_upsert_role = buildRole("upsert_role", List.of(Privilege.UPDATE, Privilege.DELETE),
                        List.of("//d/e"), Valuation.SHIELDED, List.of(DatasetState.PROCESSED));
        client.put("/role/upsert_role", another_upsert_role).expect201Created();
        Role role2 = readRole("upsert_role");
        assertEquals(List.of(Privilege.UPDATE, Privilege.DELETE), role2.getPrivileges().getIncludesList());
    }

    @Test
    void thatDeleteRoleWorks() {
        createRole("role_to_be_deleted", List.of(Privilege.CREATE), List.of("/universe"), Valuation.SENSITIVE, List.of(DatasetState.INPUT));
        client.delete("/role/role_to_be_deleted").expect200Ok();
        assertNull(readRole("role_to_be_deleted"));
    }
}
