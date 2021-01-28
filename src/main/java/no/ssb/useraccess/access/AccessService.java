package no.ssb.useraccess.access;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.protobuf.LazyStringArrayList;
import io.helidon.common.reactive.Single;
import io.opentracing.Span;
import no.ssb.dapla.auth.dataset.protobuf.DatasetState;
import no.ssb.dapla.auth.dataset.protobuf.DatasetStateSet;
import no.ssb.dapla.auth.dataset.protobuf.Group;
import no.ssb.dapla.auth.dataset.protobuf.PathSet;
import no.ssb.dapla.auth.dataset.protobuf.Privilege;
import no.ssb.dapla.auth.dataset.protobuf.PrivilegeSet;
import no.ssb.dapla.auth.dataset.protobuf.Role;
import no.ssb.dapla.auth.dataset.protobuf.User;
import no.ssb.dapla.auth.dataset.protobuf.Valuation;
import no.ssb.useraccess.autocreate.AutoCreateService;
import no.ssb.useraccess.group.GroupRepository;
import no.ssb.useraccess.role.RoleRepository;
import no.ssb.useraccess.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import static java.util.Optional.ofNullable;

public class AccessService {

    private static final Logger LOG = LoggerFactory.getLogger(AccessService.class);

    final UserRepository userRepository;
    final GroupRepository groupRepository;
    final RoleRepository roleRepository;
    final ObjectMapper objectMapper = new ObjectMapper();
    final AutoCreateService autoCreateService;

    public AccessService(UserRepository userRepository, GroupRepository groupRepository, RoleRepository roleRepository, AutoCreateService autoCreateService) {
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
        this.roleRepository = roleRepository;
        this.autoCreateService = autoCreateService;
    }

    Single<Boolean> hasAccess(Span span, String userId, Privilege privilege, String path, Valuation valuation, DatasetState state) {
        span.log("calling userRepository.getUser()");
        return userRepository.getUser(userId).switchIfEmpty(Single.defer(() -> autoCreateService.createNewUser(userId, span))).flatMapSingle(user -> {
            return groupRepository.getGroups(user.getGroupsList()).collectList().flatMapSingle(groups -> {
                Set<String> roleIds = new LinkedHashSet<>(user.getRolesList());
                for (Group group : groups) {
                    roleIds.addAll(group.getRolesList());
                }
                return roleRepository.getRoles(new ArrayList<>(roleIds)).collectList().map(roles -> {
                    for (Role role : roles) {
                        span.log(Map.of("event", "checking role", "roleId", role.getRoleId()));
                        if (!matchRole(role, privilege, path, valuation, state)) {
                            continue;
                        }
                        span.log(Map.of("event", "access granted", "roleId", role.getRoleId()));
                        return true;
                    }
                    span.log("access denied");
                    return false;
                });
            });
        }).switchIfEmpty(Single.just(false));
    }

    static boolean matchRole(Role role, Privilege privilege, String path, Valuation valuation, DatasetState state) {
        if (privilege != null && !matchPrivileges(ofNullable(role.getPrivileges()), privilege::equals)) {
            return false;
        }

        if (!matchPaths(ofNullable(role.getPaths()), path::startsWith)) {
            return false;
        }
        InternalValuation maxInternalValuation = InternalValuation.valueOf(role.getMaxValuation().name());
        InternalValuation internalValuation = InternalValuation.valueOf(valuation.name());
        if (!maxInternalValuation.grantsAccessTo(internalValuation)) {
            return false;
        }
        if (!matchStates(ofNullable(role.getStates()), state::equals)) {
            return false;
        }
        return true; // all criteria matched
    }

    static boolean matchPrivileges(Optional<PrivilegeSet> criterionNode, Function<Privilege, Boolean> matcher) {
        List<Privilege> excludes = criterionNode.map(PrivilegeSet::getExcludesList).orElse(Collections.emptyList());
        for (Privilege exclude : excludes) {
            if (matcher.apply(exclude)) {
                return false; // exclude matches
            }
        }
        List<Privilege> includes = criterionNode.map(PrivilegeSet::getIncludesList).orElse(Collections.emptyList());
        if (includes.isEmpty()) {
            return true; // empty include set always matches
        }
        for (Privilege include : includes) {
            if (matcher.apply(include)) {
                return true; // include matches
            }
        }
        return false; // non-empty include set, but no matches
    }

    static boolean matchPaths(Optional<PathSet> criterionNode, Function<String, Boolean> matcher) {
        List<String> excludes = criterionNode.map(PathSet::getExcludesList).orElse(LazyStringArrayList.EMPTY);
        for (String exclude : excludes) {
            if (matcher.apply(exclude)) {
                return false; // exclude matches
            }
        }
        List<String> includes = criterionNode.map(PathSet::getIncludesList).orElse(LazyStringArrayList.EMPTY);
        if (includes.isEmpty()) {
            return true; // empty include set always matches
        }
        for (String include : includes) {
            if (matcher.apply(include)) {
                return true; // include matches
            }
        }
        return false; // non-empty include set, but no matches
    }

    static boolean matchStates(Optional<DatasetStateSet> criterionNode, Function<DatasetState, Boolean> matcher) {
        List<DatasetState> excludes = criterionNode.map(DatasetStateSet::getExcludesList).orElse(Collections.emptyList());
        for (DatasetState exclude : excludes) {
            if (matcher.apply(exclude)) {
                return false; // exclude matches
            }
        }
        List<DatasetState> includes = criterionNode.map(DatasetStateSet::getIncludesList).orElse(Collections.emptyList());
        if (includes.isEmpty()) {
            return true; // empty include set always matches
        }
        for (DatasetState include : includes) {
            if (matcher.apply(include)) {
                return true; // include matches
            }
        }
        return false; // non-empty include set, but no matches
    }

    Single<JsonNode> listMatchingUsersRolesAndGroupsByPath(Span span, String path, String valuation, String state) {
        ArrayNode result = objectMapper.createArrayNode();
        span.log("userRepository.getUserList()");
        return userRepository.getUserList(null).collectList().flatMapSingle(users -> {
            span.log("calling roleRepository.getRoleList()");
            return roleRepository.getRoleList(null).collectList().flatMapSingle(roles -> {
                span.log("calling groupRepository.getGroupList()");
                return groupRepository.getAllGroups().collectList().flatMapSingle(groups -> {
                    roles.stream()
                            .filter(role -> matchRole(role, null, path, Valuation.valueOf(valuation.toUpperCase()), DatasetState.valueOf(state.toUpperCase())))
                            .forEach(role -> {
                                groups.stream()
                                        .filter(group -> group.getRolesList().contains(role.getRoleId()))
                                        .forEach(group -> users.stream()
                                                .filter(user -> user.getGroupsList().contains(group.getGroupId()))
                                                .forEach(user -> addMatchToResult(result, role, user, group.getGroupId())
                                                ));
                                users.stream()
                                        .filter(user -> user.getRolesList().contains(role.getRoleId()))
                                        .forEach(user -> addMatchToResult(result, role, user, ""));
                            });
                    span.log("return catalogAccess");
                    return Single.just(result);
                });
            });
        });
    }

    private static void addMatchToResult(ArrayNode result, Role role, User user, String groupId) {
        ObjectNode match = result.addObject();
        match.put("user", user.getUserId())
                .put("role", role.getRoleId())
                .put("group", groupId);
        ArrayNode privileges = match.putArray("privileges");
        for (Privilege privilege : resolvePrivileges(role.getPrivileges())) {
            privileges.add(privilege.name());
        }
    }

    private static List<Privilege> resolvePrivileges(PrivilegeSet privileges) {
        List<Privilege> privs = new ArrayList<>(privileges.getIncludesList());
        if (privs.size() == 0) {
            privs.addAll(Arrays.asList(Privilege.values()));
        }
        privs.remove(Privilege.UNRECOGNIZED);
        privileges.getExcludesList().forEach(privs::remove);
        return privs;
    }

}
