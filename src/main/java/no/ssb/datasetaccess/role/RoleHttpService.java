package no.ssb.datasetaccess.role;


import io.helidon.common.http.Http;
import io.helidon.webserver.Handler;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;
import io.opentracing.Span;
import no.ssb.dapla.auth.dataset.protobuf.Role;
import no.ssb.helidon.application.TracerAndSpan;
import no.ssb.helidon.application.Tracing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

import static no.ssb.helidon.application.Tracing.logError;
import static no.ssb.helidon.application.Tracing.spanFromHttp;
import static no.ssb.helidon.application.Tracing.traceInputMessage;
import static no.ssb.helidon.application.Tracing.traceOutputMessage;

public class RoleHttpService implements Service {

    private static final Logger LOG = LoggerFactory.getLogger(RoleHttpService.class);

    final RoleRepository repository;

    public RoleHttpService(RoleRepository repository) {
        this.repository = repository;
    }

    @Override
    public void update(Routing.Rules rules) {
        rules.get("/{roleId}", this::doGet);
        rules.put("/{roleId}", Handler.create(Role.class, this::doPut));
        rules.delete("/{roleId}", this::doDelete);
    }

    private void doGet(ServerRequest req, ServerResponse res) {
        TracerAndSpan tracerAndSpan = spanFromHttp(req, "doGet");
        Span span = tracerAndSpan.span();
        try {
            String roleId = req.path().param("roleId");
            span.setTag("roleId", roleId);
            repository.getRole(roleId)
                    .orTimeout(30, TimeUnit.SECONDS)
                    .thenAccept(role -> {
                        Tracing.restoreTracingContext(req.tracer(), span);
                        if (role == null) {
                            res.status(Http.Status.NOT_FOUND_404).send();
                        } else {
                            res.send(role);
                            traceOutputMessage(span, role);
                        }
                    }).thenRun(span::finish)
                    .exceptionally(t -> {
                        try {
                            Tracing.restoreTracingContext(req.tracer(), span);
                            logError(span, t);
                            res.status(Http.Status.INTERNAL_SERVER_ERROR_500).send(t.getMessage());
                            return null;
                        } finally {
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

    private void doPut(ServerRequest req, ServerResponse res, Role role) {
        TracerAndSpan tracerAndSpan = spanFromHttp(req, "doPut");
        Span span = tracerAndSpan.span();
        try {
            traceInputMessage(span, role);
            String roleId = req.path().param("roleId");
            span.setTag("roleId", roleId);
            if (!roleId.equals(role.getRoleId())) {
                span.log("roleId in path must match that in body");
                res.status(Http.Status.BAD_REQUEST_400).send("roleId in path must match that in body");
                span.finish();
                return;
            }
            repository.createOrUpdateRole(role)
                    .orTimeout(30, TimeUnit.SECONDS)
                    .thenRun(() -> {
                        Tracing.restoreTracingContext(req.tracer(), span);
                        res.headers().add("Location", "/role/" + roleId);
                        res.status(Http.Status.CREATED_201).send();
                    }).thenRun(span::finish)
                    .exceptionally(t -> {
                        try {
                            Tracing.restoreTracingContext(req.tracer(), span);
                            logError(span, t);
                            res.status(Http.Status.INTERNAL_SERVER_ERROR_500).send(t.getMessage());
                            return null;
                        } finally {
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

    private void doDelete(ServerRequest req, ServerResponse res) {
        TracerAndSpan tracerAndSpan = spanFromHttp(req, "doDelete");
        Span span = tracerAndSpan.span();
        try {
            String roleId = req.path().param("roleId");
            span.setTag("roleId", roleId);
            repository.deleteRole(roleId)
                    .orTimeout(30, TimeUnit.SECONDS)
                    .thenRun(() -> {
                        Tracing.restoreTracingContext(req.tracer(), span);
                        res.send();
                    })
                    .thenRun(span::finish)
                    .exceptionally(t -> {
                        try {
                            Tracing.restoreTracingContext(req.tracer(), span);
                            logError(span, t);
                            res.status(Http.Status.INTERNAL_SERVER_ERROR_500).send(t.getMessage());
                            return null;
                        } finally {
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
