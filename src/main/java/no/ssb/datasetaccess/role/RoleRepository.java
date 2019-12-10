package no.ssb.datasetaccess.role;


import io.helidon.metrics.RegistryFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowIterator;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

public class RoleRepository {

    private static final Logger LOG = LoggerFactory.getLogger(RoleRepository.class);

    private final PgPool client;

    private final Counter rolesCreatedOrUpdatedCount = RegistryFactory.getInstance().getRegistry(MetricRegistry.Type.APPLICATION).counter("rolesCreatedOrUpdatedCount");
    private final Counter rolesDeletedCount = RegistryFactory.getInstance().getRegistry(MetricRegistry.Type.APPLICATION).counter("rolesDeletedCount");
    private final Counter rolesReadCount = RegistryFactory.getInstance().getRegistry(MetricRegistry.Type.APPLICATION).counter("rolesReadCount");

    public RoleRepository(PgPool client) {
        this.client = client;
    }

    public CompletableFuture<Role> getRole(String roleId) {
        CompletableFuture<Role> future = new CompletableFuture<>();
        client.preparedQuery("SELECT roleId, document FROM role WHERE roleId = $1", Tuple.of(roleId), ar -> {
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
            Role role = Role.fromVertxJson(row.get(JsonObject.class, 1));
            future.complete(role);
            rolesReadCount.inc();
        });
        return future;
    }

    public CompletableFuture<Void> createRole(Role role) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        Tuple arguments = Tuple.tuple().addString(role.roleId).addValue(Role.toVertxJsonObject(role));
        client.preparedQuery("INSERT INTO role (roleId, document) VALUES($1, $2) ON CONFLICT (roleId) DO UPDATE SET document = $2",
                arguments, ar -> {
                    if (!ar.succeeded()) {
                        future.completeExceptionally(ar.cause());
                        return;
                    }
                    future.complete(null);
                    rolesCreatedOrUpdatedCount.inc();
                });
        return future;
    }

    public CompletableFuture<Void> deleteRole(String roleId) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        client.preparedQuery("DELETE FROM role WHERE roleId = $1", Tuple.of(roleId), ar -> {
            if (!ar.succeeded()) {
                future.completeExceptionally(ar.cause());
                return;
            }
            future.complete(null);
            rolesDeletedCount.inc();
        });
        return future;
    }

    public CompletableFuture<Void> deleteAllRoles() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        client.query("DELETE FROM role", ar -> {
            if (!ar.succeeded()) {
                future.completeExceptionally(ar.cause());
                return;
            }
            int rowsDeleted = ar.result().rowCount();
            future.complete(null);
            rolesDeletedCount.inc(rowsDeleted);
        });
        return future;
    }
}
