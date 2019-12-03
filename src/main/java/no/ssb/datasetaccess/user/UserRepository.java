package no.ssb.datasetaccess.user;


import io.reactiverse.reactivex.pgclient.PgIterator;
import io.reactiverse.reactivex.pgclient.PgPool;
import io.reactiverse.reactivex.pgclient.PgRowSet;
import io.reactiverse.reactivex.pgclient.Row;
import io.reactiverse.reactivex.pgclient.Tuple;
import io.reactiverse.reactivex.pgclient.data.Json;
import io.reactivex.Completable;
import io.reactivex.CompletableObserver;
import io.reactivex.Maybe;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import no.ssb.datasetaccess.role.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static net.logstash.logback.marker.Markers.appendEntries;

public class UserRepository {

    private static final Logger LOG = LoggerFactory.getLogger(UserRepository.class);

    private static final String SELECT_USER = "SELECT userId, document FROM user_permission WHERE userId = $1";

    private static final String INSERT_USER = "INSERT INTO user_permission (userId, document) VALUES($1, $2) ON CONFLICT (userId) DO UPDATE SET document = $2";

    private static final String DELETE_USER = "DELETE FROM user_permission WHERE userId = $1";

    private final PgPool client;

    public UserRepository(PgPool client) {
        this.client = client;
    }

    public Maybe<User> getUser(String userId) {
        return client.rxPreparedQuery(SELECT_USER, Tuple.of(userId)).flatMapMaybe(this::toUser);
    }

    private Maybe<User> toUser(PgRowSet pgRowSet) {
        PgIterator iterator = pgRowSet.iterator();
        if (!iterator.hasNext()) {
            return Maybe.empty();
        }
        Row row = iterator.next();
        String userId = row.getString(0);
        Json document = row.getJson(1);
        JsonObject jsonObject = (JsonObject) document.value();

        Set<Role> roles = jsonObject.getJsonArray("roles").stream()
                .map(jsonRole -> Role.fromJson((JsonObject) jsonRole))
                .collect(Collectors.toSet());

        return Maybe.just(new User(userId, roles));
    }

    Completable createUser(User user) {

        JsonObject document = new JsonObject().put("roles", new JsonArray(new ArrayList<>(user.getRoles())));

        Tuple arguments = Tuple.tuple().addString(user.getUserId()).addJson(Json.create(document));

        return client.rxPreparedQuery(INSERT_USER, arguments).ignoreElement();
    }

    Completable deleteUser(String userId) {
        return client.rxPreparedQuery(DELETE_USER, Tuple.tuple().addString(userId)).flatMapCompletable(rows -> {
            if (rows.rowCount() > 0) {
                LOG.info(appendEntries(Map.of("userId", userId)), "Deleted user");
            }
            return CompletableObserver::onComplete;
        });
    }

    Completable deleteAllUsers() {
        return client.rxQuery("DELETE FROM user_permission").ignoreElement();
    }
}
