package no.ssb.datasetaccess.access;

import com.google.protobuf.LazyStringArrayList;
import io.opentracing.Span;
import no.ssb.dapla.auth.dataset.protobuf.DatasetState;
import no.ssb.dapla.auth.dataset.protobuf.DatasetStateSet;
import no.ssb.dapla.auth.dataset.protobuf.Group;
import no.ssb.dapla.auth.dataset.protobuf.PathSet;
import no.ssb.dapla.auth.dataset.protobuf.Privilege;
import no.ssb.dapla.auth.dataset.protobuf.PrivilegeSet;
import no.ssb.dapla.auth.dataset.protobuf.Role;
import no.ssb.dapla.auth.dataset.protobuf.Valuation;
import no.ssb.datasetaccess.group.GroupRepository;
import no.ssb.datasetaccess.role.RoleRepository;
import no.ssb.datasetaccess.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static java.util.Optional.ofNullable;

public class AccessService {
    private static final Logger LOG = LoggerFactory.getLogger(AccessService.class);


    final UserRepository userRepository;
    final GroupRepository groupRepository;
    final RoleRepository roleRepository;

    public AccessService(UserRepository userRepository, GroupRepository groupRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
        this.roleRepository = roleRepository;
    }

    CompletableFuture<Boolean> hasAccess(Span span, String userId, Privilege privilege, String path, Valuation valuation, DatasetState state) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        span.log("calling userRepository.getUser()");
        userRepository.getUser(userId).thenAccept(user -> {
            if (user == null) {
                span.log("user not found");
                future.complete(false);
                return;
            }
            groupRepository.getGroups(user.getGroupsList()).thenAccept(groups -> {
                Set<String> roleIds = new LinkedHashSet<>(user.getRolesList());
                for (Group group : groups) {
                    roleIds.addAll(group.getRolesList());
                }
                roleRepository.getRoles(roleIds).thenAccept(roles -> {
                    for (Role role : roles) {
                        span.log(Map.of("event", "checking role", "roleId", role.getRoleId()));
                        if (!matchRole(role, privilege, path, valuation, state)) {
                            continue;
                        }
                        span.log(Map.of("event", "access granted", "roleId", role.getRoleId()));
                        future.complete(true);
                        return;
                    }
                    span.log("access denied");
                    future.complete(false);
                }).exceptionally(t -> {
                    future.completeExceptionally(t);
                    return null;
                });

            });
        });
        return future;
    }


    private boolean matchRole(Role role, Privilege privilege, String path, Valuation valuation, DatasetState state) {
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

    private boolean matchPrivileges(Optional<PrivilegeSet> criterionNode, Function<Privilege, Boolean> matcher) {
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

    private boolean matchPaths(Optional<PathSet> criterionNode, Function<String, Boolean> matcher) {
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

    private boolean matchStates(Optional<DatasetStateSet> criterionNode, Function<DatasetState, Boolean> matcher) {
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

    CompletableFuture<List<Map<String, String>>> catalogAccess(Span span, String path, String valuation, String state) {
        CompletableFuture<List<Map<String, String>>> future = new CompletableFuture<>();
        List<Map<String, String>> catalogAccess = new ArrayList<>();
        span.log("userRepository.getUserList()");
        userRepository.getUserList(null).thenAccept(users -> {
            span.log("calling roleRepostory.getRoleList()");
            roleRepository.getRoleList(null).thenAccept(roles -> {
                span.log("calling groupRepository.getGroupList()");
                groupRepository.getGroupList().thenAccept(groups -> {
                    roles.stream()
                        .filter(role -> matchRole(role, null, path, Valuation.valueOf(valuation), DatasetState.valueOf(state)))
                        .forEach(role -> {
                            groups.stream()
                                .filter(group -> group.getRolesList().contains(role.getRoleId()))
                                .forEach(group -> {
                                    users.stream()
                                        .filter(user -> user.getGroupsList().contains(group.getGroupId()))
                                        .forEach(user -> {
                                            catalogAccess.add(Map.of("path", path,"user", user.getUserId(),"role", role.getRoleId(),"group", group.getGroupId(), "privileges", privilegeString(role.getPrivileges())));
                                        });
                                });
                            users.stream()
                                .filter(user -> user.getRolesList().contains(role.getRoleId()))
                                .forEach(user -> {
                                    catalogAccess.add(Map.of("path", path,"user", user.getUserId(),"role", role.getRoleId(),"group", "", "privileges", privilegeString(role.getPrivileges())));
                                });
                        });
                    span.log("return catalogAccess");
                    future.complete(catalogAccess);

                });
            });
        }).exceptionally(t -> {
            future.completeExceptionally(t);
            return null;
        });
        return future;
    }

    private String privilegeString(PrivilegeSet privileges) {
        StringBuilder privilegesString = new StringBuilder();
        List<Privilege> privs = new ArrayList<>();
        privileges.getIncludesList().forEach(p -> privs.add(p));
        if (privs.size() == 0) {
            privs.addAll(Arrays.asList(Privilege.values()));
        }
        privs.remove(Privilege.UNRECOGNIZED);
        privileges.getExcludesList().forEach(p -> privs.remove(p));
        privs.forEach(p -> privilegesString.append(p.name()).append(" "));
        return privilegesString.toString();
    }

}
