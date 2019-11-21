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
import java.util.Map;

import static net.logstash.logback.marker.Markers.appendEntries;

@Controller("/access")
public class AccessController {

    private static final Logger LOG = LoggerFactory.getLogger(AccessController.class);

    @Inject
    private AccessRepository accessRepository;

    @Get
    public Single<HttpResponse<String>> access(@Header("Authorization") Single<String> authorization) {
        return authorization.flatMap(auth -> {
            final User user = new User("xxx-yyy-zzz");
            final Dataset dataset = new Dataset("1");

            return accessRepository.doesUserHaveAccessToDataset(user, dataset).map(userHasAccessToDataset -> {
                if (userHasAccessToDataset) {
                    return HttpResponse.ok();
                }
                return HttpResponse.unauthorized();
            });
        });
    }

    @Post
    public Single<HttpResponse<String>> createAccess(@Body("user_id") Single<String> singleUserId, @Body("dataset_id") Single<String> singleDatasetId) {
        return singleUserId.zipWith(singleDatasetId, (uid, did) -> Map.of("user_id", uid, "dataset_id", did)).map(m -> {
            String userId = m.get("user_id");
            String datasetId = m.get("dataset_id");

            LOG.info(appendEntries(Map.of("user", userId, "dataset", datasetId)), "Incoming request");

            final User user = new User(userId);
            final Dataset dataset = new Dataset(datasetId);

            try {

                accessRepository.addDatasetUserAccessIfNotExists(user, dataset);

            } catch (AccessRepository.AccessRepositoryException e) {
                LOG.warn(appendEntries(Map.of("user", userId, "dataset", datasetId)), "Could not give user access to dataset", e);
                return HttpResponse.serverError();
            }

            return HttpResponse.ok();
        });
    }
}
