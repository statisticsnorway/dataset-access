package no.ssb.useraccess.role;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.helidon.common.http.Http;
import io.helidon.webserver.Handler;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;
import io.opentracing.Span;
import no.ssb.dapla.auth.dataset.protobuf.Role;
import no.ssb.helidon.application.Tracing;
import no.ssb.helidon.media.protobuf.ProtobufJsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static no.ssb.helidon.application.Tracing.logError;
import static no.ssb.helidon.application.Tracing.spanFromHttp;
import static no.ssb.helidon.application.Tracing.traceInputMessage;
import static no.ssb.helidon.application.Tracing.traceOutputMessage;

public class RoleHttpService implements Service {

    private static final Logger LOG = LoggerFactory.getLogger(RoleHttpService.class);

    final ScheduledExecutorService timeoutService;
    final RoleRepository repository;

    public RoleHttpService(ScheduledExecutorService timeoutService, RoleRepository repository) {
        this.timeoutService = timeoutService;
        this.repository = repository;
    }

    final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void update(Routing.Rules rules) {
        rules.get("/{roleId}", this::doGet);
        rules.get("/", this::doGetAll);
        rules.put("/{roleId}", Handler.create(Role.class, this::doPut));
        rules.delete("/{roleId}", this::doDelete);
    }

    private void doGet(ServerRequest req, ServerResponse res) {
        Span span = spanFromHttp(req, "doGet");
        try {
            String roleId = req.path().param("roleId");
            span.setTag("roleId", roleId);
            repository.getRole(roleId)
                    .timeout(30, TimeUnit.SECONDS, timeoutService)
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

    private void doGetAll(ServerRequest req, ServerResponse res) {
        Span span = spanFromHttp(req, "doGetAll");
        try {
            repository.getRoleList(null)
                    .timeout(30, TimeUnit.SECONDS, timeoutService)
                    .collectList()
                    .peek(roles -> {
                        Tracing.restoreTracingContext(req.tracer(), span);
                        if (roles == null) {
                            res.status(Http.Status.NOT_FOUND_404).send();
                        } else {
                            ObjectNode returnObject = mapper.createObjectNode();
                            ArrayNode roleArray = returnObject.putArray("roles");
                            for (Role role : roles) {
                                try {
                                    roleArray.add(mapper.readTree(ProtobufJsonUtils.toString(role)));
                                } catch (JsonProcessingException e) {
                                    logError(span, e);
                                    LOG.error("unexpected error reading role-tree", e);
                                }
                            }
                            res.send(returnObject.toString());
                            traceOutputMessage(span, returnObject.asText());
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
        Span span = spanFromHttp(req, "doPut");
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
                    .timeout(30, TimeUnit.SECONDS, timeoutService)
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
        Span span = spanFromHttp(req, "doDelete");
        try {
            String roleId = req.path().param("roleId");
            span.setTag("roleId", roleId);
            repository.deleteRole(roleId)
                    .timeout(30, TimeUnit.SECONDS, timeoutService)
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
