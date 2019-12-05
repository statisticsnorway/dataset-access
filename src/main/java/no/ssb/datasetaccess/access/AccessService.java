package no.ssb.datasetaccess.access;

import io.helidon.common.http.Http;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;
import no.ssb.datasetaccess.dataset.DatasetState;
import no.ssb.datasetaccess.dataset.Valuation;
import no.ssb.datasetaccess.role.Privilege;
import no.ssb.datasetaccess.role.RoleRepository;
import no.ssb.datasetaccess.user.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class AccessService implements Service {

    final UserRepository userRepository;
    final RoleRepository roleRepository;

    public AccessService(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    public void update(Routing.Rules rules) {
        rules.get("/{userId}", this::doGet);
    }

    private void doGet(ServerRequest req, ServerResponse res) {
        String userId = req.path().param("userId");
        Privilege privilege = Privilege.valueOf(req.queryParams().first("privilege").orElseThrow());
        String namespace = req.queryParams().first("namespace").orElseThrow();
        Valuation valuation = Valuation.valueOf(req.queryParams().first("valuation").orElseThrow());
        DatasetState state = DatasetState.valueOf(req.queryParams().first("state").orElseThrow());
        hasAccess(userId, privilege, namespace, valuation, state)
                .thenAccept(access -> res.status(access ? Http.Status.OK_200 : Http.Status.FORBIDDEN_403).send())
                .exceptionally(t -> {
                    res.status(Http.Status.INTERNAL_SERVER_ERROR_500).send(t.getMessage());
                    return null;
                });
    }

    public CompletableFuture<Boolean> hasAccess(String userId, Privilege privilege, String namespace, Valuation valuation, DatasetState state) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        userRepository.getUser(userId).thenAccept(user -> {
            List<CompletableFuture<Void>> roleCompletableList = new ArrayList<>();
            for (String roleId : user.getRoles()) {
                roleCompletableList.add(roleRepository.getRole(roleId).thenAccept(role -> {
                    if (!role.getPrivileges().contains(privilege)) {
                        return;
                    }
                    String floor = role.getNamespacePrefixes().floor(namespace);
                    if (!namespace.startsWith(floor)) {
                        return;
                    }
                    if (!role.getMaxValuation().grantsAccessTo(valuation)) {
                        return;
                    }
                    if (!role.getStates().contains(state)) {
                        return;
                    }
                    future.complete(true);
                }));
            }
            CompletableFuture.allOf(roleCompletableList.toArray(new CompletableFuture[0]))
                    .thenRun(() -> future.complete(false));
        });
        return future;
    }
}
