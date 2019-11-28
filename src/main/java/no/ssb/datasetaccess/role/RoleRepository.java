package no.ssb.datasetaccess.role;


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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static net.logstash.logback.marker.Markers.appendEntries;

@Singleton
public class RoleRepository {

    private static final Logger LOG = LoggerFactory.getLogger(RoleRepository.class);

    private static final String SELECT_ROLE = "SELECT roleId, document FROM role WHERE roleId = $1";

    private static final String INSERT_ROLE = "INSERT INTO role (roleId, document) VALUES($1, $2) ON CONFLICT (roleId) DO UPDATE SET document = $2";

    private static final String DELETE_ROLE = "DELETE FROM role WHERE roleId = $1";

    private final PgPool client;

    public RoleRepository(PgPool client) {
        this.client = client;
    }

    Maybe<Role> getRole(String roleId) {
        return client.rxPreparedQuery(SELECT_ROLE, Tuple.of(roleId))
                .flatMapMaybe(pgRowSet -> toRole(pgRowSet));
    }

    private Maybe<Role> toRole(PgRowSet pgRowSet) {
        PgIterator iterator = pgRowSet.iterator();
        if (!iterator.hasNext()) {
            return Maybe.empty();
        }
        Row row = iterator.next();
        String roleId = row.getString(0);
        Json document = row.getJson(1);
        JsonObject jsonObject = (JsonObject) document.value();
        JsonArray privileges = jsonObject.getJsonArray("privileges");
        Set<Privilege> privilegesSet = privileges.stream().map(str -> Privilege.valueOf((String) str)).collect(Collectors.toSet());
        return Maybe.just(new Role(roleId, privilegesSet));
    }

    Completable createRole(Role role) {
        JsonArray privilegesArray = new JsonArray();
        role.privileges.stream().forEach(privilege -> privilegesArray.add(privilege));
        JsonObject jsonObject = new JsonObject().put("roleId", role.name).put("privileges", privilegesArray);
        final Tuple arguments = Tuple.tuple().addString(role.name).addJson(Json.create(jsonObject));
        return client.rxPreparedQuery(INSERT_ROLE, arguments).ignoreElement();
    }

    Completable deleteRole(String roleId) {
        return client.rxPreparedQuery(DELETE_ROLE, Tuple.of(roleId)).flatMapCompletable(rows -> {
            if (rows.rowCount() > 0) {
                LOG.info(appendEntries(Map.of("roleId", roleId)), "Deleted role");
            }
            return CompletableObserver::onComplete;
        });
    }

    Completable deleteAllRoles() {
        return client.rxQuery("DELETE FROM role").ignoreElement();
    }
}
