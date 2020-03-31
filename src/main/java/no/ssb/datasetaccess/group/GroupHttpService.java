package no.ssb.datasetaccess.group;

import io.helidon.common.http.Http;
import io.helidon.webserver.Handler;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;
import io.opentracing.Span;
import no.ssb.dapla.auth.dataset.protobuf.Group;
import no.ssb.helidon.application.TracerAndSpan;
import no.ssb.helidon.application.Tracing;
import no.ssb.helidon.media.protobuf.ProtobufJsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

import static no.ssb.helidon.application.Tracing.*;
import static no.ssb.helidon.application.Tracing.logError;

public class GroupHttpService implements Service {

    private static final Logger LOG = LoggerFactory.getLogger(GroupHttpService.class);

    final GroupRepository repository;

    public GroupHttpService(GroupRepository repository) {
        this.repository = repository;
    }

    @Override
    public void update(Routing.Rules rules) {
        rules.get("/", this::doGetList);
        rules.get("/{groupId}", this::doGet);
        rules.put("/{groupId}", Handler.create(Group.class, this::doPut));
        rules.delete("/{groupId}", this::doDelete);
    }

    private void doGet(ServerRequest req, ServerResponse res) {
        TracerAndSpan tracerAndSpan = spanFromHttp(req, "doGet");
        Span span = tracerAndSpan.span();
        try {
            String groupId = req.path().param("groupId");
            span.setTag("groupId", groupId);
            repository.getGroup(groupId)
                    .thenAccept(group -> {
                        Tracing.restoreTracingContext(req.tracer(), span);
                        if (group == null) {
                            res.status(Http.Status.NOT_FOUND_404).send();
                        } else {
                            res.send(group);
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
            repository.getGroupList()
                    .orTimeout(30, TimeUnit.SECONDS)
                    .thenAccept(groups -> {
                        Tracing.restoreTracingContext(req.tracer(), span);
                        if (groups == null) {
                            res.status(Http.Status.NOT_FOUND_404).send();
                        } else {
                            StringBuffer jsonGroups = new StringBuffer("{ \"groups\": [");
                            for (Group group : groups) {
                                jsonGroups.append(ProtobufJsonUtils.toString(group)).append(',');
                            }
                            jsonGroups.deleteCharAt(jsonGroups.length()-1);
                            jsonGroups.append("]}");
                            res.send(jsonGroups);
                            traceOutputMessage(span, jsonGroups.toString());
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

    
    private void doPut(ServerRequest req, ServerResponse res, Group group) {
        TracerAndSpan tracerAndSpan = spanFromHttp(req, "doPut");
        Span span = tracerAndSpan.span();
        try {
            traceInputMessage(span, group);
            String groupId = req.path().param("groupId");
            span.setTag("groupId", groupId);
            if (!groupId.equals(group.getGroupId())) {
                res.status(Http.Status.BAD_REQUEST_400).send("groupId in path must match that in body");
                return;
            }
            repository.createOrUpdateGroup(group)
                    .thenRun(() -> {
                        Tracing.restoreTracingContext(req.tracer(), span);
                        res.headers().add("Location", "/group/" + groupId);
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
            String groupId = req.path().param("groupId");
            span.setTag("groupId", groupId);
            repository.deleteGroup(groupId)
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
