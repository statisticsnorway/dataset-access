package no.ssb.datasetaccess.user;


import io.helidon.metrics.RegistryFactory;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowIterator;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import no.ssb.dapla.auth.dataset.protobuf.User;
import no.ssb.helidon.media.protobuf.ProtobufJsonUtils;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class UserRepository {

    private static final Logger LOG = LoggerFactory.getLogger(UserRepository.class);

    private final PgPool client;

    private final Counter usersCreatedOrUpdatedCount = RegistryFactory.getInstance().getRegistry(MetricRegistry.Type.APPLICATION).counter("usersCreatedOrUpdatedCount");
    private final Counter usersDeletedCount = RegistryFactory.getInstance().getRegistry(MetricRegistry.Type.APPLICATION).counter("usersDeletedCount");
    private final Counter usersReadCount = RegistryFactory.getInstance().getRegistry(MetricRegistry.Type.APPLICATION).counter("usersReadCount");

    public UserRepository(PgPool client) {
        this.client = client;
    }

    public CompletableFuture<User> getUser(String userId) {
        CompletableFuture<User> future = new CompletableFuture<>();
        client.preparedQuery("SELECT userId, document FROM user_permission WHERE userId = $1", Tuple.of(userId), ar -> {
            try {
                if (!ar.succeeded()) {
                    future.completeExceptionally(ar.cause());
                    return;
                }
                RowSet<Row> result = ar.result();
                RowIterator<Row> iterator = result.iterator();
                if (!iterator.hasNext()) {
                    future.complete(null);
                    return;
                }
                Row row = iterator.next();
                JsonObject jsonObject = row.get(JsonObject.class, 1);
                User user = ProtobufJsonUtils.toPojo(Json.encode(jsonObject), User.class);
                future.complete(user);
                usersReadCount.inc();
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    public CompletableFuture<List<User>> getUserList(String userIdPart) {
        StringBuilder query = new StringBuilder("SELECT userId, document FROM user_permission");
        if (userIdPart != null && userIdPart.length() > 0) {
            query.append(" WHERE userId LIKE '%").append(userIdPart).append("%'");
        }
        query.append(" ORDER BY userId");
//        LOG.info("query: {}", query);
        return getUserList(query, Tuple.tuple());
    }

    private CompletableFuture<List<User>> getUserList(StringBuilder query, Tuple arguments) {
        CompletableFuture<List<User>> future = new CompletableFuture<>();
        client.preparedQuery(query.toString(), arguments, ar -> {
            try {
                if (!ar.succeeded()) {
                    future.completeExceptionally(ar.cause());
                    return;
                }
                RowSet<Row> result = ar.result();
                List<User> users = new ArrayList<>(result.rowCount());
                RowIterator<Row> iterator = result.iterator();
                if (!iterator.hasNext()) {
                    future.complete(Collections.emptyList());
                    return;
                }
                while (iterator.hasNext()) {
                    Row row = iterator.next();
                    String json = Json.encode(row.get(JsonObject.class, 1));
                    User user = ProtobufJsonUtils.toPojo(json, User.class);
                    users.add(user);
                }
                future.complete(users);
                usersReadCount.inc(result.rowCount());
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    public CompletableFuture<Void> createOrUpdateUser(User user) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        JsonObject value = (JsonObject) Json.decodeValue(ProtobufJsonUtils.toString(user));
        Tuple arguments = Tuple.tuple().addString(user.getUserId()).addValue(value);
        client.preparedQuery("INSERT INTO user_permission (userId, document) VALUES($1, $2) ON CONFLICT (userId) DO UPDATE SET document = $2",
                arguments, ar -> {
                    try {
                        if (!ar.succeeded()) {
                            future.completeExceptionally(ar.cause());
                            return;
                        }
                        future.complete(null);
                        usersCreatedOrUpdatedCount.inc();
                    } catch (Throwable t) {
                        future.completeExceptionally(t);
                    }
                });

        return future;
    }

    public CompletableFuture<Void> deleteUser(String userId) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        client.preparedQuery("DELETE FROM user_permission WHERE userId = $1", Tuple.of(userId), ar -> {
            try {
                if (!ar.succeeded()) {
                    future.completeExceptionally(ar.cause());
                    return;
                }
                future.complete(null);
                usersDeletedCount.inc();
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    public CompletableFuture<Void> deleteAllUsers() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        client.query("TRUNCATE TABLE user_permission", ar -> {
            try {
                if (!ar.succeeded()) {
                    future.completeExceptionally(ar.cause());
                    return;
                }
                int rowsDeleted = ar.result().rowCount();
                future.complete(null);
                usersDeletedCount.inc(rowsDeleted);
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }
}
