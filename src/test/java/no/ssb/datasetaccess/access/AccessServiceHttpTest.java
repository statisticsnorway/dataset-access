package no.ssb.datasetaccess.access;


import no.ssb.dapla.auth.dataset.protobuf.*;
import no.ssb.datasetaccess.UserAccessApplication;
import no.ssb.datasetaccess.group.GroupRepository;
import no.ssb.datasetaccess.role.RoleRepository;
import no.ssb.datasetaccess.user.UserRepository;
import no.ssb.testing.helidon.IntegrationTestExtension;
import no.ssb.testing.helidon.TestClient;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(IntegrationTestExtension.class)
class AccessServiceHttpTest {
    private static final Logger LOG = LoggerFactory.getLogger(AccessHttpService.class);


    @Inject
    UserAccessApplication application;

    @Inject
    TestClient client;

    @BeforeEach
    void clearRepositories() throws InterruptedException, ExecutionException, TimeoutException {
        CompletableFuture.allOf(
                application.get(UserRepository.class).deleteAllUsers(),
                application.get(RoleRepository.class).deleteAllRoles()
        ).get(3, TimeUnit.SECONDS);
    }

    void createUser(String userId, Iterable<String> roles) {
        createUser(userId, roles, null);
    }

    void createUser(String userId, Iterable<String> roles, Iterable<String> groups) {
        try {
            User user = User.newBuilder().setUserId(userId)
                    .addAllRoles(roles != null ? roles : List.of(""))
                    .addAllGroups(groups != null ? groups : List.of(""))
                    .build();
            application.get(UserRepository.class).createOrUpdateUser(user).get(3, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }
    void createRole(String roleId, Iterable<Privilege> privilegeIncludes, Iterable<String> pathIncludes, Valuation maxValuation, Iterable<DatasetState> stateIncludes) {
        createRole(roleId, privilegeIncludes, null, pathIncludes, null, maxValuation, stateIncludes, null);
    }

    void createRole(String roleId, Iterable<Privilege> privilegeIncludes, Iterable<Privilege> privilegeExcludes,
                    Iterable<String> pathIncludes, Iterable<String> pathExcludes,
                    Valuation maxValuation, Iterable<DatasetState> stateIncludes,
                    Iterable<DatasetState> stateExcludes) {
        PrivilegeSet.Builder privileges = PrivilegeSet.newBuilder();
        if (privilegeIncludes != null) privileges.addAllIncludes(privilegeIncludes);
        if (privilegeExcludes != null) privileges.addAllExcludes(privilegeExcludes);

        PathSet.Builder paths = PathSet.newBuilder();
        if (pathIncludes != null) paths.addAllIncludes(pathIncludes);
        if (pathExcludes != null) paths.addAllExcludes(pathExcludes);

        DatasetStateSet.Builder states = DatasetStateSet.newBuilder();
        if (stateIncludes != null) states.addAllIncludes(stateIncludes);
        if (stateExcludes != null) states.addAllExcludes(stateExcludes);

        Role role = Role.newBuilder()
                .setRoleId(roleId)
                .setPrivileges(privileges.build())
                .setPaths(paths.build())
                .setStates(states.build())
                .setMaxValuation(maxValuation)
                .build();
        try {
            application.get(RoleRepository.class).createOrUpdateRole(role).get(3, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new RuntimeException(e);
        }
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
    void thatGetAccessWorks() {
        createUser("john_can_update", List.of("updater"));
        createRole("updater", List.of(Privilege.UPDATE), List.of("/ns/test"), Valuation.INTERNAL, List.of(DatasetState.RAW, DatasetState.INPUT));
        client.get("/access/john_can_update?privilege=UPDATE&path=/ns/test&valuation=INTERNAL&state=RAW").expect200Ok();
    }

    @Test
    void thatGetAccessThroughGroupWorks() {
        createUser("john_can_update", null, List.of("group1"));
        createRole("updater", List.of(Privilege.UPDATE), List.of("/ns/test"), Valuation.INTERNAL, List.of(DatasetState.RAW, DatasetState.INPUT));
        createGroup("group1", "This is the first group", List.of("updater"));
        client.get("/access/john_can_update?privilege=UPDATE&path=/ns/test&valuation=INTERNAL&state=RAW").expect200Ok();
    }

    @Test
    void thatGetAccessWhenRoleHasNoIncludedOrExcludedPathsWorks() {
        createUser("john_can_update", null, List.of("group1"));
        createRole("updater", List.of(Privilege.UPDATE), null, Valuation.INTERNAL, List.of(DatasetState.RAW, DatasetState.INPUT));
        client.get("/access/john_can_update?privilege=UPDATE&path=/ns/test&valuation=INTERNAL&state=RAW").expect200Ok();
    }

    @Test
    void GetAccessWhenUserDoesntHaveTheAppropriatePathReturns403() {
        createUser("john_with_missing_ns", List.of("creator"));
        createRole("creator", List.of(Privilege.CREATE), List.of("/ns/test/a", "/test"), Valuation.OPEN, List.of(DatasetState.OTHER));
        client.get("/access/john_with_missing_ns?privilege=CREATE&path=/ns/test&valuation=OPEN&state=OTHER").expect403Forbidden();
    }

    @Test
    void GetAccessWhenUserIsExcludedFromPathReturns403() {
        createUser("john_with_excluded_ns", List.of("creator"));
        createRole("creator", List.of(Privilege.CREATE), null,
                List.of("/ns/test/", "/test"), List.of("/ns/test/exclude")
                , Valuation.OPEN, List.of(DatasetState.OTHER), null);
        client.get("/access/john_with_excluded_ns?privilege=CREATE&path=/ns/test/somedir&valuation=OPEN&state=OTHER").expect200Ok();
        client.get("/access/john_with_excluded_ns?privilege=CREATE&path=/ns/test/exclude/somedir&valuation=OPEN&state=OTHER").expect403Forbidden();
    }

    @Test
    void thatGetAccessWhenUserDoesntHaveTheAppropriatePrivilegeReturns403() {
        createUser("john_cant_delete", List.of("updater"));
        createRole("updater", List.of(Privilege.UPDATE), List.of("/ns/test"), Valuation.SHIELDED, List.of(DatasetState.PROCESSED));
        client.get("/access/john_cant_delete?privilege=DELETE&path=/ns/test&valuation=SHIELDED&state=PROCESSED").expect403Forbidden();
    }

    @Test
    void GetAccessWhenUserHasExcludedPrivilegeReturns403() {
        createUser("john_with_excluded_priv", List.of("creator"));
        createRole("creator", null, List.of(Privilege.CREATE),
                List.of("/ns/test", "/test"), null
                , Valuation.OPEN, List.of(DatasetState.OTHER), null);
        client.get("/access/john_with_excluded_priv?privilege=READ&path=/ns/test&valuation=OPEN&state=OTHER").expect200Ok();
        client.get("/access/john_with_excluded_priv?privilege=CREATE&path=/ns/test&valuation=OPEN&state=OTHER").expect403Forbidden();
    }

    @Test
    void GetAccessWhenUserHasExcludedDatasetStateReturns403() {
        createUser("john_with_excluded_priv", List.of("creator"));
        createRole("creator", List.of(Privilege.CREATE), null,
                List.of("/ns/test", "/test"), null
                , Valuation.OPEN, null, List.of(DatasetState.RAW));
        client.get("/access/john_with_excluded_priv?privilege=CREATE&path=/ns/test&valuation=OPEN&state=INPUT").expect200Ok();
        client.get("/access/john_with_excluded_priv?privilege=CREATE&path=/ns/test&valuation=OPEN&state=RAW").expect403Forbidden();
    }

    @Test
    void thatGetAccessOnUserWithoutAppropriateRolesReturns403() {
        createUser("john_without_roles", List.of("reader"));
        client.get("/access/john_without_roles?privilege=DELETE&path=test&valuation=SENSITIVE&state=RAW").expect403Forbidden();
    }

    @Test
    void thatGetAccessOnNonExistingUserReturns403() {
        client.get("/access/does_not_exist?privilege=READ&path=a&valuation=SENSITIVE&state=RAW").expect403Forbidden();
    }

    @Test
    void thatGetAccessForUserWithMoreThanOneRoleWorks() {
        createUser("john_two_roles", List.of("updater", "reader"));
        createRole("updater", List.of(Privilege.UPDATE), List.of("/ns/test"), Valuation.INTERNAL, List.of(DatasetState.RAW, DatasetState.INPUT));
        createRole("reader", List.of(Privilege.READ), List.of("/ns/test"), Valuation.INTERNAL, List.of(DatasetState.RAW, DatasetState.INPUT));
        client.get("/access/john_two_roles?privilege=UPDATE&path=/ns/test&valuation=INTERNAL&state=RAW").expect200Ok();
        client.get("/access/john_two_roles?privilege=READ&path=/ns/test&valuation=INTERNAL&state=RAW").expect200Ok();
        client.get("/access/john_two_roles?privilege=DELETE&path=/ns/test&valuation=INTERNAL&state=RAW").expect403Forbidden();
    }

    @Test
    void thatCatalogAccessWorks() {
        client.get("/access/catalog?path=/ns/test/internal/raw").expect200Ok();
    }

    @Test
    void thatCatalogAccessReturnsResult() {
        createUser("john_can_update", null, List.of("group1"));
        createRole("updater", List.of(Privilege.UPDATE), List.of("/ns/test"), Valuation.INTERNAL, List.of(DatasetState.RAW, DatasetState.INPUT));
        createGroup("group1", "This is the first group", List.of("updater"));
        String respons = client.get("/access/catalog?path=/ns/test&valuation=internal&state=raw").expect200Ok().body();
        assertTrue(respons.startsWith("{\"catalogAccess\": [{"));
        assertTrue(respons.contains("\"user\":\"john_can_update\""));
        assertTrue(respons.contains("\"privileges\":\"UPDATE \""));
        assertTrue(respons.contains("\"role\":\"updater\""));
        assertTrue(respons.contains("\"group\":\"group1\""));
        assertTrue(respons.contains("\"path\":\"/ns/test\""));
        assertTrue(respons.endsWith("\"}]}"));
    }

    @Test
    void thatCatalogAccessReturnsEmptyResult() {
        createUser("john_can_update_internal", null, List.of("group1"));
        createRole("updater", List.of(Privilege.UPDATE), List.of("/ns/test"), Valuation.INTERNAL, List.of(DatasetState.RAW, DatasetState.INPUT));
        createGroup("group1", "This is the first group", List.of("updater"));
        String processedRespons = client.get("/access/catalog?path=/ns/test&valuation=internal&state=processed").expect200Ok().body();
        assertEquals("{\"catalogAccess\": []}", processedRespons);
    }

    @Test
    void thatCatalogAccessReturnsMultipleRowsResult() {
        createUser("john_can_update_shielded", null, List.of("group1"));
        createUser("june_can_update_shielded", null, List.of("group1"));
        createRole("updater", List.of(Privilege.UPDATE), List.of("/ns/test"), Valuation.SHIELDED, List.of(DatasetState.RAW, DatasetState.INPUT));
        createGroup("group1", "This is the first group", List.of("updater"));

        createUser("june_can_read_internal", null, List.of("group2"));
        createRole("readinternal", List.of(Privilege.READ), List.of("/ns/test"), Valuation.INTERNAL, List.of(DatasetState.RAW, DatasetState.INPUT));
        createGroup("group2", "This is the second group", List.of("readinternal"));

        String shieldedresponse = client.get("/access/catalog?path=/ns/test&valuation=shielded&state=raw").expect200Ok().body();
        JSONArray shieldedaccesses = new JSONObject(shieldedresponse).getJSONArray("catalogAccess");
        assertEquals(2, shieldedaccesses.length());

        String internalresponse = client.get("/access/catalog?path=/ns/test&valuation=internal&state=raw").expect200Ok().body();
        JSONArray internalaccesses = new JSONObject(internalresponse).getJSONArray("catalogAccess");
        assertEquals(3, internalaccesses.length());
    }

    @Test
    void thatExcludePathPreventsCatalogAccess() {
        createUser("john_can_update_shielded", null, List.of("group1"));
        createRole("updater", List.of(Privilege.UPDATE), null,
                List.of("/ns/test"), List.of("/ns/test/exclude"), Valuation.SHIELDED,
                List.of(DatasetState.RAW, DatasetState.INPUT), null);
        createGroup("group1", "This is the first group", List.of("updater"));
        String okrespons = client.get("/access/catalog?path=/ns/test/somedir&valuation=internal&state=input").expect200Ok().body();
        assertTrue(okrespons.contains("\"user\":\"john_can_update_shielded\""));
        String emptyrespons = client.get("/access/catalog?path=/ns/test/exclude/somedir&valuation=internal&state=input").expect200Ok().body();
        assertEquals("{\"catalogAccess\": []}", emptyrespons);

    }

    @Test
    void thatExcludePrivilegePreventsCatalogAccess() {
        createUser("john_can_update_shielded", null, List.of("group1"));
        createRole("updater", null, List.of(Privilege.DELETE),
                List.of("/ns/test"), List.of("/ns/test/exclude"), Valuation.SHIELDED,
                List.of(DatasetState.RAW, DatasetState.INPUT), null);
        createGroup("group1", "This is the first group", List.of("updater"));
        String okrespons = client.get("/access/catalog?path=/ns/test/somedir&valuation=internal&state=input").expect200Ok().body();
        assertTrue(okrespons.contains("\"user\":\"john_can_update_shielded\""));
        String emptyrespons = client.get("/access/catalog?path=/ns/test/exclude/somedir&valuation=internal&state=input").expect200Ok().body();
        assertEquals("{\"catalogAccess\": []}", emptyrespons);
    }

    @Test
    void thatCatalogAccessDirectThroughRoleWorks() {
        createUser("john_can_update_shielded", List.of("updater"));
        createRole("updater", null, List.of(Privilege.DELETE),
                List.of("/ns/test"), List.of("/ns/test/exclude"), Valuation.SHIELDED,
                List.of(DatasetState.RAW, DatasetState.INPUT), null);
        String respons = client.get("/access/catalog?path=/ns/test/somedir&valuation=internal&state=input").expect200Ok().body();
        assertTrue(respons.contains("\"user\":\"john_can_update_shielded\""));
    }
}
