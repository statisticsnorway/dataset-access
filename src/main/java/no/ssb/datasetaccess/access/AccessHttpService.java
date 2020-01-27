package no.ssb.datasetaccess.access;

import io.helidon.common.http.Http;
import io.helidon.metrics.RegistryFactory;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;
import io.opentracing.Span;
import no.ssb.dapla.auth.dataset.protobuf.Role;
import no.ssb.helidon.application.Tracing;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

import static no.ssb.helidon.application.Tracing.logError;

public class AccessHttpService implements Service {

    private static final Logger LOG = LoggerFactory.getLogger(AccessHttpService.class);

    private final AccessService accessService;

    private final Timer accessTimer = RegistryFactory.getInstance().getRegistry(MetricRegistry.Type.APPLICATION).timer("accessTimer");
    private final Counter accessGrantedCount = RegistryFactory.getInstance().getRegistry(MetricRegistry.Type.APPLICATION).counter("accessGrantedCount");
    private final Counter accessDeniedCount = RegistryFactory.getInstance().getRegistry(MetricRegistry.Type.APPLICATION).counter("accessDeniedCount");

    public AccessHttpService(AccessService accessService) {
        this.accessService = accessService;
    }

    @Override
    public void update(Routing.Rules rules) {
        rules.get("/{userId}", this::httpHasAccess);
    }

    private void httpHasAccess(ServerRequest req, ServerResponse res) {
        Span span = Tracing.spanFromHttp(req, "doGet");
        try {
            String userId = req.path().param("userId");
            span.setTag("userId", userId);
            Role.Privilege privilege = Role.Privilege.valueOf(req.queryParams().first("privilege").orElseThrow());
            span.setTag("privilege", privilege.name());
            String namespace = req.queryParams().first("namespace").orElseThrow();
            if (!namespace.startsWith("/")) {
                namespace = "/" + namespace;
            }
            span.setTag("namespace", namespace);
            Role.Valuation valuation = Role.Valuation.valueOf(req.queryParams().first("valuation").orElseThrow());
            span.setTag("valuation", valuation.name());
            Role.DatasetState state = Role.DatasetState.valueOf(req.queryParams().first("state").orElseThrow());
            span.setTag("state", state.name());
            Timer.Context timerContext = accessTimer.time();
            accessService.hasAccess(span, userId, privilege, namespace, valuation, state)
                    .orTimeout(10, TimeUnit.SECONDS)
                    .thenAccept(access -> {
                        if (access) {
                            accessGrantedCount.inc();
                            res.status(Http.Status.OK_200).send();
                        } else {
                            accessDeniedCount.inc();
                            res.status(Http.Status.FORBIDDEN_403).send();
                        }
                        span.finish();
                    }).thenRun(timerContext::stop)
                    .exceptionally(t -> {
                        try {
                            res.status(Http.Status.INTERNAL_SERVER_ERROR_500).send(t.getMessage());
                            logError(span, t);
                            LOG.error("hasAccess() user='{}'", userId, t);
                            return null;
                        } finally {
                            timerContext.stop();
                            span.finish();
                        }
                    });
        } catch (RuntimeException | Error e) {
            try {
                logError(span, e);
                LOG.error("top-level error", e);
                throw e;
            } finally {
                span.finish();
            }
        }
    }
}
