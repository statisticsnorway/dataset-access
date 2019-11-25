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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

@Controller("/access")
public class AccessController {

    private static final Logger LOG = LoggerFactory.getLogger(AccessController.class);

    @Inject
    private AccessRepository accessRepository;

    @Get("/{datasetId}")
    public Single<HttpResponse<String>> getAccess(@Header("Authorization") String authorization, @PathVariable String datasetId) {

        final Token token;
        try {
            token = Token.create(authorization);
        } catch (Token.TokenParseException e) {
            LOG.warn("Could not parse request token", e);
            return Single.just(HttpResponse.badRequest(e.getMessage()));
        }

        final Single<HttpResponse<String>> response = accessRepository
                .doesUserHaveAccessToDataset(new User(token.getUserId()), new Dataset(datasetId))
                .map(
                        userHasAccessToDataset -> {
                            if (userHasAccessToDataset) {
                                return HttpResponse.ok();
                            }
                            return HttpResponseFactory.INSTANCE.status(HttpStatus.FORBIDDEN);
                        }
                );

        return response.onErrorReturn(t -> {
            LOG.error("Got error while serving access request", t);
            return HttpResponse.serverError();
        });
    }

    @Post
    public Single<HttpResponse<String>> grantAccess(@Body("user_id") String userId, @Body("dataset_id") String datasetId) {

        Single<HttpResponse<String>> response = accessRepository
                .addDatasetUserAccessIfNotExists(new User(userId), new Dataset(datasetId))
                .toSingle(HttpResponse::ok);

        return response.onErrorReturn(t -> {
            LOG.error("Could not grant access", t);
            return HttpResponse.serverError();
        });
    }
}
