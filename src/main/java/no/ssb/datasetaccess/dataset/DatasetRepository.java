package no.ssb.datasetaccess.dataset;


import io.reactiverse.reactivex.pgclient.PgIterator;
import io.reactiverse.reactivex.pgclient.PgPool;
import io.reactiverse.reactivex.pgclient.PgRowSet;
import io.reactiverse.reactivex.pgclient.Row;
import io.reactiverse.reactivex.pgclient.Tuple;
import io.reactiverse.reactivex.pgclient.data.Json;
import io.reactivex.Completable;
import io.reactivex.CompletableObserver;
import io.reactivex.Maybe;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.Map;

import static net.logstash.logback.marker.Markers.appendEntries;

@Singleton
public class DatasetRepository {

    private static final Logger LOG = LoggerFactory.getLogger(DatasetRepository.class);

    private static final String SELECT_DATASET = "SELECT datasetId, document FROM dataset WHERE datasetId = $1";

    private static final String INSERT_DATASET = "INSERT INTO dataset (datasetId, document) VALUES($1, $2) ON CONFLICT (datasetId) DO UPDATE SET document = $2";

    private static final String DELETE_DATASET = "DELETE FROM dataset WHERE datasetId = $1";

    private final PgPool client;

    public DatasetRepository(PgPool client) {
        this.client = client;
    }

    Maybe<Dataset> getDataset(String datasetId) {
        return client.rxPreparedQuery(SELECT_DATASET, Tuple.of(datasetId))
                .flatMapMaybe(pgRowSet -> toDataset(pgRowSet));
    }

    private Maybe<Dataset> toDataset(PgRowSet pgRowSet) {
        PgIterator iterator = pgRowSet.iterator();
        if (!iterator.hasNext()) {
            return Maybe.empty();
        }
        Row row = iterator.next();
        String datasetId = row.getString(0);
        Json document = row.getJson(1);
        Dataset dataset = toDataset((JsonObject) document.value());
        return Maybe.just(dataset);
    }

    static Dataset toDataset(JsonObject jsonObject) {
        String datasetId = jsonObject.getString("datasetId");
        DatasetState state = DatasetState.valueOf(jsonObject.getString("state"));
        Valuation valuation = Valuation.valueOf(jsonObject.getString("valuation"));
        return new Dataset(datasetId, state, valuation);
    }

    static JsonObject toJsonObject(Dataset dataset) {
        return new JsonObject()
                .put("datasetId", dataset.datasetId)
                .put("state", dataset.state)
                .put("valuation", dataset.valuation);
    }

    Completable createDataset(Dataset dataset) {
        JsonObject jsonObject = toJsonObject(dataset);
        final Tuple arguments = Tuple.tuple().addString(dataset.datasetId).addJson(Json.create(jsonObject));
        return client.rxPreparedQuery(INSERT_DATASET, arguments).ignoreElement();
    }

    Completable deleteDataset(String datasetId) {
        return client.rxPreparedQuery(DELETE_DATASET, Tuple.of(datasetId)).flatMapCompletable(rows -> {
            if (rows.rowCount() > 0) {
                LOG.info(appendEntries(Map.of("datasetId", datasetId)), "Deleted dataset");
            }
            return CompletableObserver::onComplete;
        });
    }

    Completable deleteAllDatasets() {
        return client.rxQuery("DELETE FROM dataset").ignoreElement();
    }
}
