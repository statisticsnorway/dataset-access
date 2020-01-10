package no.ssb.datasetaccess.role;


import io.helidon.metrics.RegistryFactory;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowIterator;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import no.ssb.dapla.auth.dataset.protobuf.Role;
import no.ssb.helidon.media.protobuf.ProtobufJsonUtils;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
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
                String json = Json.encode(row.get(JsonObject.class, 1));
                Role role = ProtobufJsonUtils.toPojo(json, Role.class);
                future.complete(role);
                rolesReadCount.inc();
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    public CompletableFuture<List<Role>> getRoles(Collection<String> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        CompletableFuture<List<Role>> future = new CompletableFuture<>();
        StringBuilder sb = new StringBuilder("SELECT roleId, document FROM role WHERE roleId IN (");
        Tuple arguments = Tuple.tuple();
        int i = 1;
        for (String roleId : roleIds) {
            if (i > 1) {
                sb.append(",");
            }
            sb.append("$").append(i);
            arguments.addString(roleId);
            i++;
        }
        sb.append(") ORDER BY roleId");
        client.preparedQuery(sb.toString(), arguments, ar -> {
            try {
                if (!ar.succeeded()) {
                    future.completeExceptionally(ar.cause());
                    return;
                }
                RowSet<Row> result = ar.result();
                List<Role> roles = new ArrayList<>(result.rowCount());
                RowIterator<Row> iterator = result.iterator();
                if (!iterator.hasNext()) {
                    future.complete(Collections.emptyList());
                    return;
                }
                while (iterator.hasNext()) {
                    Row row = iterator.next();
                    String json = Json.encode(row.get(JsonObject.class, 1));
                    Role role = ProtobufJsonUtils.toPojo(json, Role.class);
                    roles.add(role);
                }
                future.complete(roles);
                rolesReadCount.inc(result.rowCount());
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    public CompletableFuture<Void> createOrUpdateRole(Role role) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        JsonObject jsonObject = (JsonObject) Json.decodeValue(ProtobufJsonUtils.toString(role));
        Tuple arguments = Tuple.tuple().addString(role.getRoleId()).addValue(jsonObject);
        client.preparedQuery("INSERT INTO role (roleId, document) VALUES($1, $2) ON CONFLICT (roleId) DO UPDATE SET document = $2",
                arguments, ar -> {
                    try {
                        if (!ar.succeeded()) {
                            future.completeExceptionally(ar.cause());
                            return;
                        }
                        future.complete(null);
                        rolesCreatedOrUpdatedCount.inc();
                    } catch (Throwable t) {
                        future.completeExceptionally(t);
                    }
                });
        return future;
    }

    public CompletableFuture<Void> deleteRole(String roleId) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        client.preparedQuery("DELETE FROM role WHERE roleId = $1", Tuple.of(roleId), ar -> {
            try {
                if (!ar.succeeded()) {
                    future.completeExceptionally(ar.cause());
                    return;
                }
                future.complete(null);
                rolesDeletedCount.inc();
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    public CompletableFuture<Void> deleteAllRoles() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        client.query("TRUNCATE TABLE role", ar -> {
            try {
                if (!ar.succeeded()) {
                    future.completeExceptionally(ar.cause());
                    return;
                }
                int rowsDeleted = ar.result().rowCount();
                future.complete(null);
                rolesDeletedCount.inc(rowsDeleted);
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }
}
