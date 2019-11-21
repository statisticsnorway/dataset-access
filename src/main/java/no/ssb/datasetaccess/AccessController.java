package no.ssb.datasetaccess;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.annotation.Post;
import io.reactivex.Single;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

@Controller("/access")
public class AccessController {

    private static final Logger LOG = LoggerFactory.getLogger(AccessController.class);

    @Inject
    private AccessRepository accessRepository;

    @Get
    public Single<HttpResponse<String>> access(@Header("Authorization") Single<String> authorization) {
        return accessRepository.doesUserHaveAccessToDataset(authorization.map(auth -> new AccessRepository.UserAndDataset(new User("xxx-yyy-zzz"), new Dataset("1"))))
                .map(userHasAccessToDataset -> {
                    if (userHasAccessToDataset) {
                        return HttpResponse.ok();
                    }
                    return HttpResponse.unauthorized();
                });
    }

    @Post
    public Single<HttpResponse<String>> createAccess(@Body("user_id") Single<String> singleUserId, @Body("dataset_id") Single<String> singleDatasetId) {
        Single<HttpResponse<String>> response = accessRepository.addDatasetUserAccessIfNotExists(
                singleUserId.zipWith(singleDatasetId, (uid, did) -> new AccessRepository.UserAndDataset(new User(uid), new Dataset(did))))
                .toSingle(HttpResponse::ok);
        return response
                .onErrorReturn(t -> HttpResponse.serverError());
    }
}
