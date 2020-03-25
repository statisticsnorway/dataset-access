package no.ssb.datasetaccess.catalog;


import io.helidon.common.http.Http;
import io.helidon.webserver.*;
import io.opentracing.Span;
import no.ssb.dapla.catalog.protobuf.Dataset;
import no.ssb.helidon.application.TracerAndSpan;
import no.ssb.helidon.application.Tracing;
import no.ssb.helidon.media.protobuf.ProtobufJsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

import static no.ssb.helidon.application.Tracing.*;

public class CatalogHttpService implements Service {

    private static final Logger LOG = LoggerFactory.getLogger(CatalogHttpService.class);

    final CatalogRepository repository;

    public CatalogHttpService(CatalogRepository repository) {
        this.repository = repository;
    }

    @Override
    public void update(Routing.Rules rules) {
        rules.get("/", this::doGetAll);
    }

    private void doGetAll(ServerRequest req, ServerResponse res) {
        TracerAndSpan tracerAndSpan = spanFromHttp(req, "doGetAll");
        Span span = tracerAndSpan.span();
        try {
            repository.getCatalogs()
                    .orTimeout(30, TimeUnit.SECONDS)
                    .thenAccept(catalogs -> {
                        Tracing.restoreTracingContext(req.tracer(), span);
                        if (catalogs == null) {
                            res.status(Http.Status.NOT_FOUND_404).send();
                        } else {
                            StringBuffer jsonCatalogs = new StringBuffer("{ \"catalogs\": [");
                            for (Dataset catalog : catalogs) {
                                LOG.info("catalog: {}", catalog);
                                jsonCatalogs.append(ProtobufJsonUtils.toString(catalog)).append(',');                            }
                            jsonCatalogs.deleteCharAt(jsonCatalogs.length() - 1);
                            jsonCatalogs.append("]}");
                            res.send(jsonCatalogs);
                            traceOutputMessage(span, jsonCatalogs.toString());
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
}
