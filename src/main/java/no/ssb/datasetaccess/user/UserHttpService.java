package no.ssb.datasetaccess.user;

import io.helidon.common.http.Http;
import io.helidon.webserver.Handler;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;
import io.opentracing.Span;
import no.ssb.dapla.auth.dataset.protobuf.User;
import no.ssb.helidon.application.TracerAndSpan;
import no.ssb.helidon.application.Tracing;
import no.ssb.helidon.media.protobuf.ProtobufJsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

import static no.ssb.helidon.application.Tracing.*;
import static no.ssb.helidon.application.Tracing.logError;

public class UserHttpService implements Service {

    private static final Logger LOG = LoggerFactory.getLogger(UserHttpService.class);

    final UserRepository repository;

    public UserHttpService(UserRepository repository) {
        this.repository = repository;
    }

    @Override
    public void update(Routing.Rules rules) {
        rules.get("/", this::doGetList);
        rules.get("/{userId}", this::doGet);
        rules.put("/{userId}", Handler.create(User.class, this::doPut));
        rules.delete("/{userId}", this::doDelete);
    }

    private void doGet(ServerRequest req, ServerResponse res) {
        TracerAndSpan tracerAndSpan = spanFromHttp(req, "doGet");
        Span span = tracerAndSpan.span();
        try {
            String userId = req.path().param("userId");
            span.setTag("userId", userId);
            repository.getUser(userId)
                    .thenAccept(user -> {
                        Tracing.restoreTracingContext(req.tracer(), span);
                        if (user == null) {
                            res.status(Http.Status.NOT_FOUND_404).send();
                        } else {
                            res.send(user);
                        }
                    }).thenRun(span::finish)
                    .exceptionally(t -> {
                        try {
                            Tracing.restoreTracingContext(req.tracer(), span);
                            res.status(Http.Status.INTERNAL_SERVER_ERROR_500).send(t.getMessage());
                            return null;
                        } finally {
                            span.finish();
                        }
                    });
        } catch (RuntimeException | Error e) {
            try {
                Tracing.logError(span, e, "unexpected error");
                LOG.error("unexpected error", e);
                throw e;
            } finally {
                span.finish();
            }
        }
    }

    private void doGetList(ServerRequest req, ServerResponse res) {
        TracerAndSpan tracerAndSpan = spanFromHttp(req, "doGetList");
        Span span = tracerAndSpan.span();
        try {
            repository.getUserList(null)
                    .orTimeout(30, TimeUnit.SECONDS)
                    .thenAccept(users -> {
                        Tracing.restoreTracingContext(req.tracer(), span);
                        if (users == null) {
                            res.status(Http.Status.NOT_FOUND_404).send();
                        } else {
                            StringBuffer jsonUsers = new StringBuffer("{\"users\": [");
                            for (User user : users) {
                                jsonUsers.append(ProtobufJsonUtils.toString(user)).append(',');
                            }
                            if (jsonUsers.indexOf(",") > 0) {
                                jsonUsers.deleteCharAt(jsonUsers.length()-1);
                            }
                            jsonUsers.append("]}");
                            res.send(jsonUsers);
                            traceOutputMessage(span, jsonUsers.toString());
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


    private void doPut(ServerRequest req, ServerResponse res, User user) {
        TracerAndSpan tracerAndSpan = spanFromHttp(req, "doPut");
        Span span = tracerAndSpan.span();
        try {
            traceInputMessage(span, user);
            String userId = req.path().param("userId");
            span.setTag("userId", userId);
            if (!userId.equals(user.getUserId())) {
                res.status(Http.Status.BAD_REQUEST_400).send("userId in path must match that in body");
                return;
            }
            repository.createOrUpdateUser(user)
                    .thenRun(() -> {
                        Tracing.restoreTracingContext(req.tracer(), span);
                        res.headers().add("Location", "/user/" + userId);
                        res.status(Http.Status.CREATED_201).send();
                    }).thenRun(span::finish)
                    .exceptionally(t -> {
                        try {
                            Tracing.restoreTracingContext(req.tracer(), span);
                            res.status(Http.Status.INTERNAL_SERVER_ERROR_500).send(t.getMessage());
                            return null;
                        } finally {
                            span.finish();
                        }
                    });
        } catch (RuntimeException | Error e) {
            try {
                Tracing.logError(span, e, "unexpected error");
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
            String userId = req.path().param("userId");
            span.setTag("userId", userId);
            repository.deleteUser(userId)
                    .thenRun(() -> {
                        Tracing.restoreTracingContext(req.tracer(), span);
                        res.send();
                    })
                    .thenRun(span::finish)
                    .exceptionally(t -> {
                        try {
                            Tracing.restoreTracingContext(req.tracer(), span);
                            res.status(Http.Status.INTERNAL_SERVER_ERROR_500).send(t.getMessage());
                            return null;
                        } finally {
                            span.finish();
                        }
                    });
        } catch (RuntimeException | Error e) {
            try {
                Tracing.logError(span, e, "unexpected error");
                LOG.error("unexpected error", e);
                throw e;
            } finally {
                span.finish();
            }
        }
    }
}
