package no.ssb.datasetaccess.dataset;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Delete;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.reactivex.Single;

import java.net.URI;

@Controller("/dataset")
public class DatasetController {

    @Post("/{datasetId}")
    public Single<HttpResponse<String>> createDataset(@PathVariable String datasetId, @Body Dataset dataset) {
        return Single.just(HttpResponse.created(URI.create("/dataset/" + datasetId)));
    }

    @Get("/{datasetId}")
    public Single<HttpResponse<Dataset>> getDataset(@PathVariable String datasetId) {
        return Single.just(HttpResponse.ok(new Dataset(DatasetState.RAW, Valuation.OPEN)));
    }

    @Delete("/{datasetId}")
    public Single<HttpResponse<Dataset>> deleteDataset(@PathVariable String datasetId) {
        return Single.just(HttpResponse.ok());
    }
}
