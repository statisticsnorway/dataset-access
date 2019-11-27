package no.ssb.datasetaccess.access;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.reactivex.Single;

@Controller("/access")
public class AccessController {

    @Get("/{userId}/{namespace}")
    public Single<HttpResponse<String>> getRole(@PathVariable String userId, @PathVariable String namespace) {
        return Single.just(HttpResponse.ok());
    }
}
