package no.ssb.datasetaccess.access;

import io.helidon.common.http.Http;
import io.helidon.metrics.RegistryFactory;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;
import no.ssb.datasetaccess.dataset.DatasetState;
import no.ssb.datasetaccess.dataset.Valuation;
import no.ssb.datasetaccess.role.Privilege;
import no.ssb.datasetaccess.role.RoleRepository;
import no.ssb.datasetaccess.user.UserRepository;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class AccessService implements Service {

    private static final Logger LOG = LoggerFactory.getLogger(AccessService.class);

    final UserRepository userRepository;
    final RoleRepository roleRepository;

    private final Timer accessTimer = RegistryFactory.getInstance().getRegistry(MetricRegistry.Type.APPLICATION).timer("accessTimer");
    private final Counter accessGrantedCount = RegistryFactory.getInstance().getRegistry(MetricRegistry.Type.APPLICATION).counter("accessGrantedCount");
    private final Counter accessDeniedCount = RegistryFactory.getInstance().getRegistry(MetricRegistry.Type.APPLICATION).counter("accessDeniedCount");

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
        Timer.Context timerContext = accessTimer.time();
        hasAccess(userId, privilege, namespace, valuation, state)
                .orTimeout(10, TimeUnit.SECONDS)
                .thenAccept(access -> {
                    if (access) {
                        accessGrantedCount.inc();
                        res.status(Http.Status.OK_200).send();
                    } else {
                        accessDeniedCount.inc();
                        res.status(Http.Status.FORBIDDEN_403).send();
                    }
                }).thenRun(() -> timerContext.stop())
                .exceptionally(t -> {
                    try {
                        res.status(Http.Status.INTERNAL_SERVER_ERROR_500).send(t.getMessage());
                        LOG.error("Could not complete access request for user {}", userId, t);
                        return null;
                    } finally {
                        timerContext.stop();
                    }
                });
    }

    public CompletableFuture<Boolean> hasAccess(String userId, Privilege privilege, String namespace, Valuation valuation, DatasetState state) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        userRepository.getUser(userId).thenAccept(user -> {
            if (user == null) {
                future.complete(false);
                return;
            }
            List<CompletableFuture<Void>> roleCompletableList = new ArrayList<>();
            for (String roleId : user.getRoles()) {
                roleCompletableList.add(roleRepository.getRole(roleId).thenAccept(role -> {
                    if (role == null) {
                        return;
                    }
                    if (!role.getPrivileges().contains(privilege)) {
                        return;
                    }
                    String floor = role.getNamespacePrefixes().floor(namespace);
                    if (floor == null || !namespace.startsWith(floor)) {
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
