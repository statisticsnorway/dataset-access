package no.ssb.datasetaccess.access;

import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.stub.StreamObserver;
import io.helidon.common.http.Http;
import io.helidon.metrics.RegistryFactory;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;
import no.ssb.dapla.auth.dataset.protobuf.AccessCheckRequest;
import no.ssb.dapla.auth.dataset.protobuf.AccessCheckResponse;
import no.ssb.dapla.auth.dataset.protobuf.AuthServiceGrpc;
import no.ssb.dapla.auth.dataset.protobuf.Role;
import no.ssb.datasetaccess.role.RoleRepository;
import no.ssb.datasetaccess.user.UserRepository;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class AccessService extends AuthServiceGrpc.AuthServiceImplBase implements Service {

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
        Role.Privilege privilege = Role.Privilege.valueOf(req.queryParams().first("privilege").orElseThrow());
        String namespace = req.queryParams().first("namespace").orElseThrow();
        if (!namespace.startsWith("/")) {
            namespace = "/" + namespace;
        }
        Role.Valuation valuation = Role.Valuation.valueOf(req.queryParams().first("valuation").orElseThrow());
        Role.DatasetState state = Role.DatasetState.valueOf(req.queryParams().first("state").orElseThrow());
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

    public CompletableFuture<Boolean> hasAccess(String userId, Role.Privilege privilege, String namespace, Role.Valuation valuation, Role.DatasetState state) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        userRepository.getUser(userId).thenAccept(user -> {
            if (user == null) {
                future.complete(false);
                return;
            }
            roleRepository.getRoles(user.getRolesList()).thenAccept(roles -> {
                for (Role role : roles) {
                    if (role == null) {
                        continue;
                    }
                    if (!role.getPrivilegesList().contains(privilege)) {
                        continue;
                    }
                    NavigableSet<String> namespacePrefixes = new TreeSet<>(role.getNamespacePrefixesList());
                    String floor = namespacePrefixes.floor(namespace);
                    if (floor == null || !namespace.startsWith(floor)) {
                        continue;
                    }
                    InternalValuation maxInternalValuation = InternalValuation.valueOf(role.getMaxValuation().name());
                    InternalValuation internalValuation = InternalValuation.valueOf(valuation.name());
                    if (!maxInternalValuation.grantsAccessTo(internalValuation)) {
                        continue;
                    }
                    if (!role.getStatesList().contains(state)) {
                        continue;
                    }
                    future.complete(true);
                    return;
                }
                future.complete(false);
            }).exceptionally(t -> {
                future.completeExceptionally(t);
                return null;
            });
        });
        return future;
    }

    @Override
    public void hasAccess(AccessCheckRequest request, StreamObserver<AccessCheckResponse> responseObserver) {
        String userId = request.getUserId();
        Role.Privilege privilege = Role.Privilege.valueOf(request.getPrivilege());
        String namespace = request.getNamespace();
        Role.Valuation valuation = Role.Valuation.valueOf(request.getValuation());
        Role.DatasetState state = Role.DatasetState.valueOf(request.getState());
        hasAccess(userId, privilege, namespace, valuation, state)
                .orTimeout(10, TimeUnit.SECONDS)
                .thenAccept(access -> {
                    responseObserver.onNext(AccessCheckResponse.newBuilder().setAllowed(access).build());
                    responseObserver.onCompleted();
                })
                .exceptionally(throwable -> {
                    responseObserver.onError(new StatusException(Status.fromThrowable(throwable)));
                    return null;
                });
    }
}
