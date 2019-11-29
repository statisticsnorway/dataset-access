package no.ssb.datasetaccess.dataset;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Delete;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Put;
import io.reactivex.Maybe;
import io.reactivex.Single;

import javax.inject.Inject;
import java.net.URI;

@Controller("/dataset")
public class DatasetController {

    @Inject
    DatasetRepository repository;

    @Put("/{datasetId}")
    public Single<HttpResponse<String>> createDataset(@PathVariable String datasetId, @Body Dataset dataset) {
        return repository.createDataset(dataset).toSingleDefault(HttpResponse.created(URI.create("/dataset/" + datasetId)));
    }

    @Get("/{datasetId}")
    public Maybe<HttpResponse<Dataset>> getDataset(@PathVariable String datasetId) {
        return repository.getDataset(datasetId).map(dataset -> HttpResponse.ok(dataset));
    }

    @Delete("/{datasetId}")
    public Single<HttpResponse<Dataset>> deleteDataset(@PathVariable String datasetId) {
        return repository.deleteDataset(datasetId).toSingleDefault(HttpResponse.ok());
    }
}
