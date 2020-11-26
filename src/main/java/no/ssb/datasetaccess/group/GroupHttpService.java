package no.ssb.datasetaccess.group;

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
import no.ssb.dapla.auth.dataset.protobuf.Group;
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

public class GroupHttpService implements Service {

    private static final Logger LOG = LoggerFactory.getLogger(GroupHttpService.class);

    final ScheduledExecutorService timeoutService;
    final GroupRepository repository;

    public GroupHttpService(ScheduledExecutorService timeoutService, GroupRepository repository) {
        this.timeoutService = timeoutService;
        this.repository = repository;
    }

    final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void update(Routing.Rules rules) {
        rules.get("/", this::doGetList);
        rules.get("/{groupId}", this::doGet);
        rules.put("/{groupId}", Handler.create(Group.class, this::doPut));
        rules.delete("/{groupId}", this::doDelete);
    }

    private void doGet(ServerRequest req, ServerResponse res) {
        Span span = spanFromHttp(req, "doGet");
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
        Span span = spanFromHttp(req, "doGetList");
        try {
            repository.getAllGroups()
                    .timeout(30, TimeUnit.SECONDS, timeoutService)
                    .collectList()
                    .peek(groups -> {
                        Tracing.restoreTracingContext(req.tracer(), span);
                        if (groups == null) {
                            res.status(Http.Status.NOT_FOUND_404).send();
                        } else {
                            ObjectNode returnObject = mapper.createObjectNode();
                            ArrayNode groupArray = returnObject.putArray("groups");
                            for (Group group : groups) {
                                try {
                                    groupArray.add(mapper.readTree(ProtobufJsonUtils.toString(group)));
                                } catch (JsonProcessingException e) {
                                    logError(span, e);
                                    LOG.error("unexpected error reading user-tree", e);
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


    private void doPut(ServerRequest req, ServerResponse res, Group group) {
        Span span = spanFromHttp(req, "doPut");
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
        Span span = spanFromHttp(req, "doDelete");
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
