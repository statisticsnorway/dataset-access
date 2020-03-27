package no.ssb.datasetaccess.access;

import io.opentracing.Span;
import no.ssb.dapla.auth.dataset.protobuf.DatasetState;
import no.ssb.dapla.auth.dataset.protobuf.Privilege;
import no.ssb.dapla.auth.dataset.protobuf.Role;
import no.ssb.dapla.auth.dataset.protobuf.Valuation;
import no.ssb.datasetaccess.role.RoleRepository;
import no.ssb.datasetaccess.user.UserRepository;

import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;

public class AccessService {

    final UserRepository userRepository;
    final RoleRepository roleRepository;

    public AccessService(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    CompletableFuture<Boolean> hasAccess(Span span, String userId, Privilege privilege, String namespace, Valuation valuation, DatasetState state) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        span.log("calling userRepository.getUser()");
        userRepository.getUser(userId).thenAccept(user -> {
            if (user == null) {
                span.log("user not found");
                future.complete(false);
                return;
            }
            roleRepository.getRoles(user.getRolesList()).thenAccept(roles -> {
                for (Role role : roles) {
                    span.log(Map.of("event", "checking role", "roleId", role.getRoleId()));
                    if (role == null) {
                        continue;
                    }
                    if (!role.getPrivileges().getIncludesList().contains(privilege)) {
                        continue;
                    }
                    NavigableSet<String> namespacePrefixes = new TreeSet<>(role.getPaths().getIncludesList());
                    String floor = namespacePrefixes.floor(namespace);
                    if (floor == null || !namespace.startsWith(floor)) {
                        continue;
                    }
                    InternalValuation maxInternalValuation = InternalValuation.valueOf(role.getMaxValuation().name());
                    InternalValuation internalValuation = InternalValuation.valueOf(valuation.name());
                    if (!maxInternalValuation.grantsAccessTo(internalValuation)) {
                        continue;
                    }
                    if (!role.getStates().getIncludesList().contains(state)) {
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
        return future;
    }
}
