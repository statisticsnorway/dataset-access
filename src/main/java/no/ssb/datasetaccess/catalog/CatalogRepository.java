package no.ssb.datasetaccess.catalog;


import io.helidon.metrics.RegistryFactory;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowIterator;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import no.ssb.dapla.auth.dataset.protobuf.Role;
import no.ssb.helidon.media.protobuf.ProtobufJsonUtils;
import no.ssb.dapla.catalog.protobuf.Dataset;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class CatalogRepository {

    private static final Logger LOG = LoggerFactory.getLogger(CatalogRepository.class);

    private final PgPool client;

    private final Counter DatasetsReadCount = RegistryFactory.getInstance().getRegistry(MetricRegistry.Type.APPLICATION).counter("DatasetsReadCount");

    public CatalogRepository(PgPool client) {
        this.client = client;
    }

    public CompletableFuture<List<Dataset>> getCatalogs() {
        CompletableFuture<List<Dataset>> future = new CompletableFuture<>();
        String query = "SELECT path, document FROM Dataset ORDER BY path";
        Tuple arguments = Tuple.tuple();
        client.preparedQuery(query, arguments, ar -> {
            try {
                if (!ar.succeeded()) {
                    future.completeExceptionally(ar.cause());
                    return;
                }
                RowSet<Row> result = ar.result();
                List<Dataset> catalogs = new ArrayList<>(result.rowCount());
                RowIterator<Row> iterator = result.iterator();
                if (!iterator.hasNext()) {
                    future.complete(Collections.emptyList());
                    return;
                }
                while (iterator.hasNext()) {
                    Row row = iterator.next();
                    LOG.info("Row: {}", row.get(JsonObject.class, 1));
                    String json = Json.encode(row.get(JsonObject.class, 1));
                    Dataset dataset = ProtobufJsonUtils.toPojo(json, Dataset.class);
                    catalogs.add(dataset);
                }
                future.complete(catalogs);
                DatasetsReadCount.inc(result.rowCount());
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }
}
