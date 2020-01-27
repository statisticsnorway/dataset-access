package no.ssb.datasetaccess.role;


import io.helidon.common.http.Http;
import io.helidon.webserver.Handler;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;
import io.opentracing.Span;
import no.ssb.dapla.auth.dataset.protobuf.Role;

import java.util.concurrent.TimeUnit;

import static no.ssb.helidon.application.Tracing.logError;
import static no.ssb.helidon.application.Tracing.spanFromHttp;
import static no.ssb.helidon.application.Tracing.traceInputMessage;
import static no.ssb.helidon.application.Tracing.traceOutputMessage;

public class RoleHttpService implements Service {

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
        Span span = spanFromHttp(req, "doGet");
        try {
            String roleId = req.path().param("roleId");
            span.setTag("roleId", roleId);
            repository.getRole(roleId)
                    .orTimeout(30, TimeUnit.SECONDS)
                    .thenAccept(role -> {
                        if (role == null) {
                            res.status(Http.Status.NOT_FOUND_404).send();
                        } else {
                            res.send(role);
                            traceOutputMessage(span, role);
                        }
                    }).thenRun(span::finish)
                    .exceptionally(t -> {
                        try {
                            logError(span, t);
                            res.status(Http.Status.INTERNAL_SERVER_ERROR_500).send(t.getMessage());
                            return null;
                        } finally {
                            span.finish();
                        }
                    });
        } catch (Exception | Error e) {
            try {
                logError(span, e);
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
                    .orTimeout(30, TimeUnit.SECONDS)
                    .thenRun(() -> {
                        res.headers().add("Location", "/role/" + roleId);
                        res.status(Http.Status.CREATED_201).send();
                    }).thenRun(span::finish)
                    .exceptionally(t -> {
                        try {
                            logError(span, t);
                            res.status(Http.Status.INTERNAL_SERVER_ERROR_500).send(t.getMessage());
                            return null;
                        } finally {
                            span.finish();
                        }
                    });
        } catch (Exception | Error e) {
            try {
                logError(span, e);
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
                    .orTimeout(30, TimeUnit.SECONDS)
                    .thenRun(res::send)
                    .thenRun(span::finish)
                    .exceptionally(t -> {
                        try {
                            logError(span, t);
                            res.status(Http.Status.INTERNAL_SERVER_ERROR_500).send(t.getMessage());
                            return null;
                        } finally {
                            span.finish();
                        }
                    });
        } catch (Exception | Error e) {
            try {
                logError(span, e);
            } finally {
                span.finish();
            }
        }
    }
}
