package no.ssb.datasetaccess.access;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.helidon.common.reactive.Multi;
import io.helidon.common.reactive.Single;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(IntegrationTestExtension.class)
class AccessServiceHttpTest {
    private static final Logger LOG = LoggerFactory.getLogger(AccessHttpService.class);


    @Inject
    UserAccessApplication application;

    @Inject
    TestClient client;

    @BeforeEach
    void clearRepositories() {
        Multi.concat(
                application.get(UserRepository.class).deleteAllUsers(),
                application.get(GroupRepository.class).deleteAllGroups(),
                application.get(RoleRepository.class).deleteAllRoles())
                .collectList()
                .await(3, TimeUnit.SECONDS);
    }

    void createUser(String userId, Iterable<String> roles) {
        createUser(userId, roles, null);
    }

    void createUser(String userId, Iterable<String> roles, Iterable<String> groups) {
        User user = User.newBuilder().setUserId(userId)
                .addAllRoles(roles != null ? roles : Collections.emptyList())
                .addAllGroups(groups != null ? groups : Collections.emptyList())
                .build();
        application.get(UserRepository.class).createOrUpdateUser(user).await(3, TimeUnit.SECONDS);
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
        application.get(RoleRepository.class).createOrUpdateRole(role).await(3, TimeUnit.SECONDS);
    }

    Group createGroup(String groupId, String description, Iterable<String> roles) {
        Group group = Group.newBuilder().setGroupId(groupId).setDescription(description).addAllRoles(roles).build();
        application.get(GroupRepository.class).createOrUpdateGroup(group).await(3, TimeUnit.SECONDS);
        return group;
    }

    Group getGroup(String groupId) {
        return application.get(GroupRepository.class).getGroup(groupId).await();
    }

    @Test
    void thatGetAccessOnExistingUserWorks() {
        createUser("odin@ssb.no", List.of("user.odin@ssb.no"), List.of("felles"));
        createRole("user.odin@ssb.no",
                null, null,
                List.of("/user/odin"), null,
                Valuation.SENSITIVE,
                null, null);
        createRole("felles",
                null, null,
                List.of("/felles/", "/kilde/"), null,
                Valuation.SENSITIVE,
                null, null);
        createGroup("felles","This is the felles group", List.of("felles"));
        client.get("/access/odin@ssb.no?privilege=READ&path=/felles/&valuation=SENSITIVE&state=RAW").expect200Ok();
        client.get("/access/odin@ssb.no?privilege=READ&path=/user/odin&valuation=SENSITIVE&state=RAW").expect200Ok();
    }

    @Test
    void thatGetAccessOnExistingUserWithWrongPathIsDenied() {
        createUser("odin@ssb.no", List.of("user.odin@ssb.no"), null);
        createRole("user.odin@ssb.no",
                null, null,
                List.of("/user/odin"), null,
                Valuation.SENSITIVE,
                null, null);
        client.get("/access/odin@ssb.no?privilege=READ&path=/denied/access&valuation=SENSITIVE&state=RAW").expect403Forbidden();
    }

    @Test
    void thatGetAccessAutoCreatingNewUsersWorks() {
        createRole("felles",
                null, null,
                List.of("/felles/", "/kilde/"), null,
                Valuation.SENSITIVE,
                null, null);
        createGroup("felles","This is the felles group", List.of("felles"));

        //  to the 2 autocreate paths: "/felles/" and "/user/username/"
        client.get("/access/thor@ssb.no?privilege=CREATE&path=/user/thor/&valuation=SENSITIVE&state=RAW").expect200Ok();
        client.get("/access/tyr@ssb.no?privilege=CREATE&path=/felles/&valuation=SENSITIVE&state=RAW").expect200Ok();
    }

    //  Should create a new user, and then deny the request
    @Test
    void thatGetAccessAutoCreatingNewUsersWhereRequestPathIsOutsideDefaultAccessWorks() {
        client.get("/access/odin@ssb.no?privilege=CREATE&path=outside/default/access&valuation=SENSITIVE&state=RAW").expect403Forbidden();

        //  check that user and role were created (not empty)
        assertFalse(application.get(UserRepository.class).getUser("odin@ssb.no").await().getUserId().isEmpty());
        assertFalse(application.get(RoleRepository.class).getRole("user.odin").await().getRoleId().isEmpty());
    }

    @Test
    void thatGetAccessAutoCreatingNewUsersWithMalformedUserIdIsDenied() {
        client.get("/access/does_not_exist?privilege=READ&path=a&valuation=SENSITIVE&state=RAW").expect403Forbidden();
    }

    @Test
    void thatGetAccessAutoCreatingNewUsersWithWrongDomainIsDenied() {
        client.get("/access/does_not_exist@does_not_exist.net?privilege=READ&path=a&valuation=SENSITIVE&state=RAW").expect403Forbidden();
    }

    @Test
    void thatAutoCreateCreatesCorrectEntries() {

        //  autoCreate new user
        client.get("/access/thor@ssb.no?privilege=CREATE&path=/user/thor/&valuation=SENSITIVE&state=RAW").expect200Ok();

        //  retrieve autocreated user and role from db
        Single<User> autoCreatedUser = application.get(UserRepository.class).getUser("thor@ssb.no");
        Single<Role> autoCreatedRole = application.get(RoleRepository.class).getRole("user.thor");

        try {
            //  check user
            assertEquals("thor@ssb.no", autoCreatedUser.get().getUserId());
            assertEquals(List.of("felles"), autoCreatedUser.get().getGroupsList());
            assertEquals(1, autoCreatedUser.get().getGroupsCount());
            assertEquals(List.of("user.thor"), autoCreatedUser.get().getRolesList());
            assertEquals(1, autoCreatedUser.get().getRolesCount());

            //  check roles
            assertEquals("user.thor", autoCreatedRole.get().getRoleId());
            assertEquals("Home folder for user thor@ssb.no", autoCreatedRole.get().getDescription());
            assertEquals(PrivilegeSet.newBuilder()
                            .addAllIncludes(List.of(Privilege.READ, Privilege.CREATE, Privilege.UPDATE))
                            .addAllExcludes(List.of(Privilege.DELETE)).build(),
                    autoCreatedRole.get().getPrivileges());
            assertEquals(PathSet.newBuilder()
                            .addAllIncludes(List.of("/user/thor/"))
                            .addAllExcludes(List.of("/ns/test/thor/")).build(),
                    autoCreatedRole.get().getPaths());
            assertEquals(Valuation.SENSITIVE, autoCreatedRole.get().getMaxValuation());
            assertEquals(DatasetStateSet.newBuilder()
                            .addAllIncludes(List.of(DatasetState.RAW, DatasetState.INPUT, DatasetState.PROCESSED, DatasetState.OUTPUT, DatasetState.PRODUCT, DatasetState.OTHER))
                            .addAllExcludes(List.of(DatasetState.TEMP)).build(),
                    autoCreatedRole.get().getStates());
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    @Test
    void thatAutoCreateDoesNotCreateDuplicateEntriesOfUsersGroupsOrRoles() {
        createRole("felles",
                null, null,
                List.of("/felles/", "/kilde/"), null,
                Valuation.SENSITIVE,
                null, null);
        createGroup("felles","This is the felles group", List.of("felles"));

        //  create and check new user multiple times
        client.get("/access/thor@ssb.no?privilege=CREATE&path=/user/thor/&valuation=SENSITIVE&state=RAW").expect200Ok();
        client.get("/access/thor@ssb.no?privilege=CREATE&path=/felles/&valuation=SENSITIVE&state=RAW").expect200Ok();
        client.get("/access/odin@ssb.no?privilege=CREATE&path=/user/odin/&valuation=SENSITIVE&state=RAW").expect200Ok();

        //  count specific user in db
        AtomicInteger userCount = new AtomicInteger();
        application.get(UserRepository.class).getUserList("thor@ssb.no").forEach(user -> {
            userCount.set(userCount.get() + 1);
        }).await();
        assertEquals(1, userCount.get());

        //  count "felles" group
        AtomicInteger groupCount = new AtomicInteger();
        application.get(GroupRepository.class).getAllGroups().forEach(group -> {
            if (group.getGroupId().equals("felles")) {
                groupCount.set(groupCount.get() + 1);
            }
        }).await();
        assertEquals(1, groupCount.get());

        //  count specific user's role in db
        AtomicInteger roleCount = new AtomicInteger();
        application.get(RoleRepository.class).getRoleList("user.thor").forEach(role -> {
            roleCount.set(roleCount.get() + 1);
        }).await();
        assertEquals(1, roleCount.get());
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
        createUser("john_can_update", List.of("updater"), null);
        createRole("updater", List.of(Privilege.UPDATE), null, Valuation.INTERNAL, List.of(DatasetState.RAW, DatasetState.INPUT));
        client.get("/access/john_can_update?privilege=UPDATE&path=/ns/test&valuation=INTERNAL&state=RAW").expect200Ok();
    }

    @Test
    void thatGetAccessWhenUserDoesntHaveTheAppropriatePathReturns403() {
        createUser("john_with_missing_ns", List.of("creator"));
        createRole("creator", List.of(Privilege.CREATE), List.of("/ns/test/a", "/test"), Valuation.OPEN, List.of(DatasetState.OTHER));
        client.get("/access/john_with_missing_ns?privilege=CREATE&path=/ns/test&valuation=OPEN&state=OTHER").expect403Forbidden();
    }

    @Test
    void thatGetAccessWhenUserIsExcludedFromPathReturns403() {
        createUser("john_with_excluded_ns", List.of("creator"));
        createRole("creator", List.of(Privilege.CREATE), null,
                List.of("/ns/test/", "/test"), List.of("/ns/test/exclude"),
                Valuation.OPEN, List.of(DatasetState.OTHER), null);
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
    void thatGetAccessWhenUserHasExcludedPrivilegeReturns403() {
        createUser("john_with_excluded_priv", List.of("creator"));
        createRole("creator", null, List.of(Privilege.CREATE),
                List.of("/ns/test", "/test"), null,
                Valuation.OPEN, List.of(DatasetState.OTHER), null);
        client.get("/access/john_with_excluded_priv?privilege=READ&path=/ns/test&valuation=OPEN&state=OTHER").expect200Ok();
        client.get("/access/john_with_excluded_priv?privilege=CREATE&path=/ns/test&valuation=OPEN&state=OTHER").expect403Forbidden();
    }

    @Test
    void thatGetAccessWhenUserHasExcludedDatasetStateReturns403() {
        createUser("john_with_excluded_priv", List.of("creator"));
        createRole("creator", List.of(Privilege.CREATE), null,
                List.of("/ns/test", "/test"), null,
                Valuation.OPEN, null, List.of(DatasetState.RAW));
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
        client.get("/access?path=/ns/test&valuation=internal&state=raw").expect200Ok();
    }

    @Test
    void thatCatalogAccessReturnsResult() throws JsonProcessingException {
        createUser("john_can_update", null, List.of("group1"));
        createRole("updater", List.of(Privilege.UPDATE), List.of("/ns/test"), Valuation.INTERNAL, List.of(DatasetState.RAW, DatasetState.INPUT));
        createGroup("group1", "This is the first group", List.of("updater"));
        String response = client.get("/access?path=/ns/test&valuation=internal&state=raw").expect200Ok().body();
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode expected = mapper.createArrayNode();
        expected.addObject()
                .put("user", "john_can_update")
                .put("role", "updater")
                .put("group", "group1")
                .putArray("privileges").add("UPDATE");
        JsonNode actual = mapper.readTree(response);
        assertEquals(expected, actual);
    }

    @Test
    void thatCatalogAccessReturnsEmptyResult() {
        createUser("john_can_update_internal", null, List.of("group1"));
        createRole("updater", List.of(Privilege.UPDATE), List.of("/ns/test"), Valuation.INTERNAL, List.of(DatasetState.RAW, DatasetState.INPUT));
        createGroup("group1", "This is the first group", List.of("updater"));
        String processedResponse = client.get("/access?path=/ns/test&valuation=internal&state=processed").expect200Ok().body();
        assertEquals("[]", processedResponse);
    }

    @Test
    void thatCatalogAccessReturnsMultipleRowsResult() throws JsonProcessingException {
        createUser("john_can_update_shielded", null, List.of("group1"));
        createUser("june_can_update_shielded", null, List.of("group1"));
        createRole("updater", List.of(Privilege.UPDATE), List.of("/ns/test"), Valuation.SHIELDED, List.of(DatasetState.RAW, DatasetState.INPUT));
        createGroup("group1", "This is the first group", List.of("updater"));

        createUser("june_can_read_internal", null, List.of("group2"));
        createRole("readinternal", List.of(Privilege.READ), List.of("/ns/test"), Valuation.INTERNAL, List.of(DatasetState.RAW, DatasetState.INPUT));
        createGroup("group2", "This is the second group", List.of("readinternal"));

        ObjectMapper mapper = new ObjectMapper();

        String shieldedresponseJson = client.get("/access?path=/ns/test&valuation=shielded&state=raw").expect200Ok().body();
        JsonNode shieldedresponse = mapper.readTree(shieldedresponseJson);
        assertTrue(shieldedresponse.isArray());
        assertEquals(2, shieldedresponse.size());

        String internalresponseJson = client.get("/access?path=/ns/test&valuation=internal&state=raw").expect200Ok().body();
        JsonNode internalaccesses = mapper.readTree(internalresponseJson);
        assertTrue(internalaccesses.isArray());
        assertEquals(3, internalaccesses.size());
    }

    @Test
    void thatExcludePathPreventsCatalogAccess() {
        createUser("john_can_update_shielded", null, List.of("group1"));
        createRole("updater", List.of(Privilege.UPDATE), null,
                List.of("/ns/test"), List.of("/ns/test/exclude"), Valuation.SHIELDED,
                List.of(DatasetState.RAW, DatasetState.INPUT), null);
        createGroup("group1", "This is the first group", List.of("updater"));
        String okresponse = client.get("/access?path=/ns/test/somedir&valuation=internal&state=input").expect200Ok().body();
        assertTrue(okresponse.contains("\"user\":\"john_can_update_shielded\""));
        String emptyresponse = client.get("/access?path=/ns/test/exclude/somedir&valuation=internal&state=input").expect200Ok().body();
        assertEquals("[]", emptyresponse);

    }

    @Test
    void thatExcludePrivilegePreventsCatalogAccess() {
        createUser("john_can_update_shielded", null, List.of("group1"));
        createRole("updater", null, List.of(Privilege.DELETE),
                List.of("/ns/test"), List.of("/ns/test/exclude"), Valuation.SHIELDED,
                List.of(DatasetState.RAW, DatasetState.INPUT), null);
        createGroup("group1", "This is the first group", List.of("updater"));
        String okresponse = client.get("/access?path=/ns/test/somedir&valuation=internal&state=input").expect200Ok().body();
        assertTrue(okresponse.contains("\"user\":\"john_can_update_shielded\""));
        String emptyresponse = client.get("/access?path=/ns/test/exclude/somedir&valuation=internal&state=input").expect200Ok().body();
        assertEquals("[]", emptyresponse);
    }

    @Test
    void thatCatalogAccessDirectThroughRoleWorks() {
        createUser("john_can_update_shielded", List.of("updater"));
        createRole("updater", null, List.of(Privilege.DELETE),
                List.of("/ns/test"), List.of("/ns/test/exclude"), Valuation.SHIELDED,
                List.of(DatasetState.RAW, DatasetState.INPUT), null);
        String response = client.get("/access?path=/ns/test/somedir&valuation=internal&state=input").expect200Ok().body();
        assertTrue(response.contains("\"user\":\"john_can_update_shielded\""));
    }
}
