package no.ssb.datasetaccess;


import io.micronaut.tracing.annotation.NewSpan;
import io.micronaut.tracing.annotation.SpanTag;
import io.reactiverse.reactivex.pgclient.PgPool;
import io.reactiverse.reactivex.pgclient.PgRowSet;
import io.reactiverse.reactivex.pgclient.Tuple;
import io.reactivex.Completable;
import io.reactivex.Single;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.Map;

import static net.logstash.logback.marker.Markers.appendEntries;

@Singleton
public class AccessRepository {

    private static final Logger LOG = LoggerFactory.getLogger(AccessRepository.class);

    private static final String DOES_USER_HAVE_ACCESS = "" +
            "SELECT * FROM dataset_user_permission " +
            "WHERE dataset_user_id = $1 " +
            "AND dataset_id = $2";

    private static final String CREATE_USER = "INSERT INTO dataset_user (id) VALUES ($1) ON CONFLICT DO NOTHING";

    private static final String CREATE_DATASET = "INSERT INTO dataset (id) VALUES ($1) ON CONFLICT DO NOTHING";

    private static final String CREATE_DATASET_USER_ACCESS = "" +
            "INSERT INTO dataset_user_permission (dataset_user_id, dataset_id) " +
            "VALUES ($1, $2) ON CONFLICT DO NOTHING";

    private final Config config;

    private final PgPool client;

    public AccessRepository(Config config, PgPool client) {
        this.config = config;
        this.client = client;
    }

    static class UserAndDataset {
        final User user;
        final Dataset dataset;

        UserAndDataset(User user, Dataset dataset) {
            this.user = user;
            this.dataset = dataset;
        }

        @Override
        public String toString() {
            return "UserAndDataset{" +
                    "user=" + user +
                    ", dataset=" + dataset +
                    '}';
        }
    }

    @NewSpan
    Single<Boolean> doesUserHaveAccessToDataset(Single<UserAndDataset> _uad) throws AccessRepositoryException {
        return _uad.flatMap(uad -> client.rxPreparedQuery(DOES_USER_HAVE_ACCESS, Tuple.of(uad.user.getId(), uad.dataset.getId())).map(pgRowSet -> mapUserAccess(uad, pgRowSet)));
    }

    @NewSpan
    Boolean mapUserAccess(@SpanTag UserAndDataset uad, PgRowSet pgRowSet) {
        boolean hasAccess = pgRowSet.iterator().hasNext();
        if (hasAccess) {
            LOG.info(
                    appendEntries(Map.of("dataset_id", uad.dataset.getId(), "user_id", uad.user.getId())),
                    "User can access dataset"
            );
        }
        return hasAccess;
    }

    private Completable addUserIfNotExists(final User user) throws AccessRepositoryException {
        return client.rxPreparedQuery(CREATE_USER, Tuple.of(user.getId())).flatMapCompletable(pgRowSet -> {
            int rowsAffected = 1; // TODO get from query
            if (rowsAffected == 0) {
                LOG.info(appendEntries(Map.of("user_id", user.getId())), "User already exists");
            }
            return null; // signal insert complete
        });
    }

    private Completable addDatasetIfNotExists(final Dataset dataset) throws AccessRepositoryException {
        return client.rxPreparedQuery(CREATE_DATASET, Tuple.of(dataset.getId())).flatMapCompletable(pgRowSet -> null);
        // if (rowsAffected == 0) LOG.info(appendEntries(Map.of("dataset_id", dataset.getId())), "Dataset already exists");
        // TODO query timeout
    }

    Completable addDatasetUserAccessIfNotExists(UserAndDataset uad) throws AccessRepositoryException {
        return client.rxPreparedQuery(CREATE_DATASET_USER_ACCESS, Tuple.of(uad.user.getId(), uad.dataset.getId())).ignoreElement();
    }

    static class AccessRepositoryException extends RuntimeException {
        AccessRepositoryException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
