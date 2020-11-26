package no.ssb.datasetaccess.access;

import io.helidon.common.http.Http;
import io.helidon.metrics.RegistryFactory;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;
import io.opentracing.Span;
import no.ssb.dapla.auth.dataset.protobuf.DatasetState;
import no.ssb.dapla.auth.dataset.protobuf.Privilege;
import no.ssb.dapla.auth.dataset.protobuf.Valuation;
import no.ssb.helidon.application.Tracing;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static no.ssb.helidon.application.Tracing.logError;
import static no.ssb.helidon.application.Tracing.traceOutputMessage;

public class AccessHttpService implements Service {

    private static final Logger LOG = LoggerFactory.getLogger(AccessHttpService.class);

    private final ScheduledExecutorService timeoutService;
    private final AccessService accessService;

    private final Timer accessUserTimer = RegistryFactory.getInstance().getRegistry(MetricRegistry.Type.APPLICATION).timer("accessUserTimer");
    private final Timer accessListMatchingTimer = RegistryFactory.getInstance().getRegistry(MetricRegistry.Type.APPLICATION).timer("accessListMatchingTimer");
    private final Counter accessGrantedCount = RegistryFactory.getInstance().getRegistry(MetricRegistry.Type.APPLICATION).counter("accessGrantedCount");
    private final Counter accessDeniedCount = RegistryFactory.getInstance().getRegistry(MetricRegistry.Type.APPLICATION).counter("accessDeniedCount");

    public AccessHttpService(ScheduledExecutorService timeoutService, AccessService accessService) {
        this.timeoutService = timeoutService;
        this.accessService = accessService;
    }

    @Override
    public void update(Routing.Rules rules) {
        rules.get("/", this::listMatchingUsersRolesAndGroupsByPath);
        rules.get("/{userId}", this::httpHasAccess);
    }

    private void httpHasAccess(ServerRequest req, ServerResponse res) {
        Span span = Tracing.spanFromHttp(req, "httpHasAccess");
        try {
            String userId = req.path().param("userId");
            span.setTag("userId", userId);
            Privilege privilege = Privilege.valueOf(req.queryParams().first("privilege").orElseThrow());
            span.setTag("privilege", privilege.name());
            String path = req.queryParams().first("path").orElseThrow();
            if (!path.startsWith("/")) {
                path = "/" + path;
            }
            span.setTag("path", path);
            Valuation valuation = Valuation.valueOf(req.queryParams().first("valuation").orElseThrow());
            span.setTag("valuation", valuation.name());
            DatasetState state = DatasetState.valueOf(req.queryParams().first("state").orElseThrow());
            span.setTag("state", state.name());
            Timer.Context timerContext = accessUserTimer.time();
            accessService.hasAccess(span, userId, privilege, path, valuation, state)
                    .timeout(1, TimeUnit.SECONDS, timeoutService)
                    .subscribe(access -> {
                        Tracing.restoreTracingContext(req.tracer(), span);
                        if (access) {
                            accessGrantedCount.inc();
                            res.status(Http.Status.OK_200).send();
                        } else {
                            accessDeniedCount.inc();
                            res.status(Http.Status.FORBIDDEN_403).send();
                        }
                        span.finish();
                        timerContext.stop();
                    }, t -> {
                        try {
                            Tracing.restoreTracingContext(req.tracer(), span);
                            res.status(Http.Status.INTERNAL_SERVER_ERROR_500).send(t.getMessage());
                            logError(span, t);
                            LOG.error("hasAccess() user='{}'", userId, t);
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

    private void listMatchingUsersRolesAndGroupsByPath(ServerRequest req, ServerResponse res) {
        Span span = Tracing.spanFromHttp(req, "catalogAccess");
        try {
            String path = req.queryParams().first("path").orElseThrow();
            String valuation = req.queryParams().first("valuation").orElseThrow();
            String state = req.queryParams().first("state").orElseThrow();
            if (!path.startsWith("/")) {
                path = "/" + path;
            }
            span.setTag("path", path);
            span.setTag("valuation", valuation);
            span.setTag("state", state);
            Timer.Context timerContext = accessListMatchingTimer.time();
            accessService.listMatchingUsersRolesAndGroupsByPath(span, path, valuation, state)
                    .timeout(5, TimeUnit.SECONDS, timeoutService)
                    .subscribe(catalogAccessList -> {
                        Tracing.restoreTracingContext(req.tracer(), span);
                        if (catalogAccessList == null) {
                            res.status(Http.Status.NOT_FOUND_404).send();
                        } else {
                            String json = catalogAccessList.toString();
                            res.send(json);
                            traceOutputMessage(span, json);
                        }
                        span.finish();
                        timerContext.stop();
                    }, t -> {
                        try {
                            Tracing.restoreTracingContext(req.tracer(), span);
                            logError(span, t);
                            res.status(Http.Status.INTERNAL_SERVER_ERROR_500).send(t.getMessage());
                        } finally {
                            timerContext.stop();
                            span.finish();
                        }
                    });
        } catch (RuntimeException | Error e) {
            try {
                logError(span, e);
                LOG.error("unexpected error", e);
                throw e;
            } finally {
                span.finish();
            }
        }
    }

}
