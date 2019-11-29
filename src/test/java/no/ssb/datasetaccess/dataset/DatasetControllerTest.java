package no.ssb.datasetaccess.dataset;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.RxHttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.annotation.MicronautTest;
import no.ssb.datasetaccess.HttpClientTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@MicronautTest
class DatasetControllerTest {

    @Inject
    @Client("/")
    RxHttpClient httpClient;

    @Inject
    DatasetRepository repository;

    @BeforeEach
    void clearRepository() {
        repository.deleteAllDatasets().blockingAwait(3, TimeUnit.SECONDS);
    }

    void createDataset(String datasetId, DatasetState state, Valuation valuation) {
        repository.createDataset(new Dataset(datasetId, state, valuation)).blockingAwait(3, TimeUnit.SECONDS);
    }

    Dataset readDataset(String datasetId) {
        return repository.getDataset(datasetId).blockingGet();
    }

    @Test
    void thatGetDatasetWorks() {
        createDataset("ds1", DatasetState.INPUT, Valuation.OPEN);
        HttpResponse<Dataset> response = httpClient.exchange(HttpRequest.GET("/dataset/ds1"), Dataset.class).blockingFirst();
        Dataset dataset = response.getBody().orElseThrow();
        assertThat((CharSequence) response.getStatus()).isEqualTo(HttpStatus.OK);
        assertThat(dataset.datasetId).isEqualTo("ds1");
        assertThat(dataset.state).isEqualTo(DatasetState.INPUT);
        assertThat(dataset.valuation).isEqualTo(Valuation.OPEN);
    }

    @Test
    void thatGetNonExistentDatasetRespondsWith404NotFound() {
        HttpResponse<Dataset> response = httpClient.exchange(HttpRequest.GET("/dataset/non-existent"), Dataset.class)
                .onErrorReturn(HttpClientTestUtils::toHttpResponse).blockingFirst();
        assertThat((CharSequence) response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void thatPutDatasetWorks() {
        HttpResponse<String> response = httpClient.exchange(HttpRequest.PUT("/dataset/dsx", new Dataset("dsx", DatasetState.RAW, Valuation.SHIELDED)), String.class).blockingFirst();
        assertThat((CharSequence) response.getStatus()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getHeaders().getFirst("Location").orElseThrow()).isEqualTo("/dataset/dsx");
        Dataset dataset = readDataset("dsx");
        assertThat(dataset.datasetId).isEqualTo("dsx");
        assertThat(dataset.state).isEqualTo(DatasetState.RAW);
        assertThat(dataset.valuation).isEqualTo(Valuation.SHIELDED);
    }

    @Test
    void thatCreateUpsertPutDatasetWorks() {
        HttpResponse<String> upsert1 = httpClient.exchange(HttpRequest.PUT("/dataset/upsert_dataset", new Dataset("upsert_dataset", DatasetState.RAW, Valuation.SENSITIVE)), String.class).blockingFirst();
        assertThat((CharSequence) upsert1.getStatus()).isEqualTo(HttpStatus.CREATED);
        Dataset ds1 = readDataset("upsert_dataset");
        assertThat(ds1.state).isEqualTo(DatasetState.RAW);
        assertThat(ds1.valuation).isEqualTo(Valuation.SENSITIVE);
        HttpResponse<String> upsert2 = httpClient.exchange(HttpRequest.PUT("/dataset/upsert_dataset", new Dataset("upsert_dataset", DatasetState.INPUT, Valuation.INTERNAL)), String.class).blockingFirst();
        assertThat((CharSequence) upsert2.getStatus()).isEqualTo(HttpStatus.CREATED);
        Dataset ds2 = readDataset("upsert_dataset");
        assertThat(ds2.state).isEqualTo(DatasetState.INPUT);
        assertThat(ds2.valuation).isEqualTo(Valuation.INTERNAL);
    }

    @Test
    void thatDeleteDatasetWorks() {
        createDataset("dataset_to_be_deleted", DatasetState.RAW, Valuation.INTERNAL);
        HttpResponse<String> response = httpClient.exchange(HttpRequest.DELETE("/dataset/dataset_to_be_deleted"), String.class).blockingFirst();
        assertThat((CharSequence) response.getStatus()).isEqualTo(HttpStatus.OK);
        assertThat(readDataset("dataset_to_be_deleted")).isNull();
    }
}
