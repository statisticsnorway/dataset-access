package no.ssb.datasetaccess.user;

import io.helidon.common.http.Http;
import io.helidon.webserver.Handler;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;
import io.opentracing.Span;
import no.ssb.dapla.auth.dataset.protobuf.User;
import no.ssb.helidon.application.Tracing;

import static no.ssb.helidon.application.Tracing.spanFromHttp;
import static no.ssb.helidon.application.Tracing.traceInputMessage;

public class UserHttpService implements Service {

    final UserRepository repository;

    public UserHttpService(UserRepository repository) {
        this.repository = repository;
    }

    @Override
    public void update(Routing.Rules rules) {
        rules.get("/{userId}", this::doGet);
        rules.put("/{userId}", Handler.create(User.class, this::doPut));
        rules.delete("/{userId}", this::doDelete);
    }

    private void doGet(ServerRequest req, ServerResponse res) {
        Span span = spanFromHttp(req, "doGet");
        try {
            String userId = req.path().param("userId");
            span.setTag("userId", userId);
            repository.getUser(userId)
                    .thenAccept(user -> {
                        if (user == null) {
                            res.status(Http.Status.NOT_FOUND_404).send();
                        } else {
                            res.send(user);
                        }
                    }).thenRun(span::finish)
                    .exceptionally(t -> {
                        try {
                            res.status(Http.Status.INTERNAL_SERVER_ERROR_500).send(t.getMessage());
                            return null;
                        } finally {
                            span.finish();
                        }
                    });
        } catch (Exception | Error e) {
            try {
                Tracing.logError(span, e, "unexpected error");
            } finally {
                span.finish();
            }
        }
    }

    private void doPut(ServerRequest req, ServerResponse res, User user) {
        Span span = spanFromHttp(req, "doPut");
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
                        res.headers().add("Location", "/user/" + userId);
                        res.status(Http.Status.CREATED_201).send();
                    }).thenRun(span::finish)
                    .exceptionally(t -> {
                        try {
                            res.status(Http.Status.INTERNAL_SERVER_ERROR_500).send(t.getMessage());
                            return null;
                        } finally {
                            span.finish();
                        }
                    });
        } catch (Exception | Error e) {
            try {
                Tracing.logError(span, e, "unexpected error");
            } finally {
                span.finish();
            }
        }
    }

    private void doDelete(ServerRequest req, ServerResponse res) {
        Span span = spanFromHttp(req, "doDelete");
        try {
            String userId = req.path().param("userId");
            span.setTag("userId", userId);
            repository.deleteUser(userId)
                    .thenRun(res::send)
                    .thenRun(span::finish)
                    .exceptionally(t -> {
                        try {
                            res.status(Http.Status.INTERNAL_SERVER_ERROR_500).send(t.getMessage());
                            return null;
                        } finally {
                            span.finish();
                        }
                    });
        } catch (Exception | Error e) {
            try {
                Tracing.logError(span, e, "unexpected error");
            } finally {
                span.finish();
            }
        }
    }
}
