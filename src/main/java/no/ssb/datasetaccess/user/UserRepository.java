package no.ssb.datasetaccess.user;


import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowIterator;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

public class UserRepository {

    private static final Logger LOG = LoggerFactory.getLogger(UserRepository.class);

    private final PgPool client;

    public UserRepository(PgPool client) {
        this.client = client;
    }

    public CompletableFuture<User> getUser(String userId) {
        CompletableFuture<User> future = new CompletableFuture<>();
        client.preparedQuery("SELECT userId, document FROM user_permission WHERE userId = $1", Tuple.of(userId), ar -> {
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
            User user = User.fromJson(row.get(JsonObject.class, 1));
            future.complete(user);
        });
        return future;
    }

    public CompletableFuture<Void> createUser(User user) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        Tuple arguments = Tuple.tuple().addString(user.getUserId()).addValue(User.toJsonObject(user));
        client.preparedQuery("INSERT INTO user_permission (userId, document) VALUES($1, $2) ON CONFLICT (userId) DO UPDATE SET document = $2",
                arguments, ar -> {
                    if (!ar.succeeded()) {
                        future.completeExceptionally(ar.cause());
                        return;
                    }
                    future.complete(null);
                });
        return future;
    }

    public CompletableFuture<Void> deleteUser(String userId) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        client.preparedQuery("DELETE FROM user_permission WHERE userId = $1", Tuple.of(userId), ar -> {
            if (!ar.succeeded()) {
                future.completeExceptionally(ar.cause());
                return;
            }
            future.complete(null);
        });
        return future;
    }

    public CompletableFuture<Void> deleteAllUsers() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        client.query("DELETE FROM user_permission", ar -> {
            if (!ar.succeeded()) {
                future.completeExceptionally(ar.cause());
                return;
            }
            future.complete(null);
        });
        return future;
    }
}
