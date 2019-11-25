package no.ssb.datasetaccess;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpResponseFactory;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.reactivex.Single;
import no.ssb.datasetaccess.AccessRepository.UserAndDataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

@Controller("/access")
public class AccessController {

    private static final Logger LOG = LoggerFactory.getLogger(AccessController.class);

    @Inject
    private AccessRepository accessRepository;

    @Get("/{datasetId}")
    public Single<HttpResponse<String>> getAccess(@Header("Authorization") Single<String> authorization, @PathVariable Single<String> datasetId) {
        final Single<HttpResponse<String>> response = accessRepository.doesUserHaveAccessToDataset(
                authorization
                        .map(Token::create)
                        .zipWith(datasetId, (t, d) -> new UserAndDataset(new User(t.getUserId()), new Dataset(d))))
                .map(
                        userHasAccessToDataset -> {
                            if (userHasAccessToDataset) {
                                return HttpResponse.ok();
                            }
                            return HttpResponseFactory.INSTANCE.status(HttpStatus.FORBIDDEN);
                        }
                );

        return response.onErrorReturn(throwable -> {
            if (throwable instanceof Token.TokenParseException) {
                return HttpResponse.badRequest(throwable.getMessage());
            } else {
                LOG.error("Got error while serving access request", throwable);
                return HttpResponse.serverError();
            }
        });
    }

    @Post
    public Single<HttpResponse<String>> grantAccess(@Body("user_id") String singleUserId, @Body("dataset_id") String singleDatasetId) {

        Single<HttpResponse<String>> response = accessRepository
                .addDatasetUserAccessIfNotExists(new UserAndDataset(new User(singleUserId), new Dataset(singleDatasetId)))
                .toSingle(HttpResponse::ok);

        return response.onErrorReturn(t -> {
            LOG.error("Could not grant access", t);
            return HttpResponse.serverError();
        });
    }
}
