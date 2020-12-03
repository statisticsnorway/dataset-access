package no.ssb.datasetaccess.maintenance;


import io.helidon.common.http.Http;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;
import io.opentracing.Span;
import no.ssb.helidon.application.Tracing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledExecutorService;

import static no.ssb.helidon.application.Tracing.logError;
import static no.ssb.helidon.application.Tracing.spanFromHttp;

public class MaintenanceHttpService implements Service {

    private static final Logger LOG = LoggerFactory.getLogger(MaintenanceHttpService.class);

    final MaintenanceRepository repository;

    final ScheduledExecutorService timeoutService;

    public MaintenanceHttpService(ScheduledExecutorService timeoutService, MaintenanceRepository repository) {
        this.timeoutService = timeoutService;
        this.repository = repository;
    }

    @Override
    public void update(Routing.Rules rules) {
        rules.delete("/all", this::doDelete);
    }

    private void doDelete(ServerRequest req, ServerResponse res) {
        Span span = spanFromHttp(req, "doDelete");
        try {
            repository.deleteAll()
                    .collectList()
                    .peek(deletedCountList -> {
                        Tracing.restoreTracingContext(req.tracer(), span);
                        res.send();
                    })
                    .peek(deletedCountList -> span.finish())
                    .onError(t -> {
                        try {
                            Tracing.restoreTracingContext(req.tracer(), span);
                            logError(span, t);
                            res.status(Http.Status.INTERNAL_SERVER_ERROR_500).send(t.getMessage());
                        } finally {
                            span.finish();
                        }
                    })
                    .subscribe(deletedCountList -> {
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
