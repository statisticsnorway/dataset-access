package no.ssb.datasetaccess.group;


import io.helidon.metrics.RegistryFactory;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowIterator;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import no.ssb.dapla.auth.dataset.protobuf.Group;
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

public class GroupRepository {

    private static final Logger LOG = LoggerFactory.getLogger(GroupRepository.class);

    private final PgPool client;

    private final Counter groupsCreatedOrUpdatedCount = RegistryFactory.getInstance().getRegistry(MetricRegistry.Type.APPLICATION).counter("groupsCreatedOrUpdatedCount");
    private final Counter groupsDeletedCount = RegistryFactory.getInstance().getRegistry(MetricRegistry.Type.APPLICATION).counter("groupsDeletedCount");
    private final Counter groupsReadCount = RegistryFactory.getInstance().getRegistry(MetricRegistry.Type.APPLICATION).counter("groupsReadCount");

    public GroupRepository(PgPool client) {
        this.client = client;
    }

    public CompletableFuture<Group> getGroup(String groupId) {
        CompletableFuture<Group> future = new CompletableFuture<>();
        client.preparedQuery("SELECT groupId, document FROM UserGroup WHERE groupId = $1", Tuple.of(groupId), ar -> {
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
                Group group = ProtobufJsonUtils.toPojo(Json.encode(jsonObject), Group.class);
                future.complete(group);
                groupsReadCount.inc();
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    public CompletableFuture<List<Group>> getGroups(Collection<String> groupIds) {
        CompletableFuture<List<Group>> future = new CompletableFuture<>();
        StringBuilder sb = new StringBuilder("SELECT groupId, document FROM UserGroup ");
        Tuple arguments = Tuple.tuple();
        int i = 1;
        if (groupIds != null && groupIds.size() > 0) {
            sb.append("WHERE groupId IN (");
            for (String groupId : groupIds) {
                if (i > 1) {
                    sb.append(",");
                }
                sb.append("$").append(i);
                arguments.addString(groupId);
                i++;
            }
            sb.append(") ");
        }

        sb.append("ORDER BY groupId");
        LOG.info("query: {}", sb.toString());
        LOG.info("arguments: {}", arguments);
        client.preparedQuery(sb.toString(), arguments, ar -> {
            try {
                if (!ar.succeeded()) {
                    future.completeExceptionally(ar.cause());
                    return;
                }
                LOG.info("ar: {}", ar);
                RowSet<Row> result = ar.result();
                List<Group> groups = new ArrayList<>(result.rowCount());
                RowIterator<Row> iterator = result.iterator();
                if (!iterator.hasNext()) {
                    future.complete(Collections.emptyList());
                    return;
                }
                while (iterator.hasNext()) {
                    Row row = iterator.next();
                    LOG.info("row: {}", row);
                    String json = Json.encode(row.get(JsonObject.class, 1));
                    Group group = ProtobufJsonUtils.toPojo(json, Group.class);
                    groups.add(group);
                }
                future.complete(groups);
                groupsReadCount.inc(result.rowCount());
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    public CompletableFuture<Void> createOrUpdateGroup(Group group) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        JsonObject value = (JsonObject) Json.decodeValue(ProtobufJsonUtils.toString(group));
        Tuple arguments = Tuple.tuple().addString(group.getGroupId()).addValue(value);
        client.preparedQuery("INSERT INTO UserGroup (groupId, document) VALUES($1, $2) ON CONFLICT (groupId) DO UPDATE SET document = $2",
                arguments, ar -> {
                    try {
                        if (!ar.succeeded()) {
                            future.completeExceptionally(ar.cause());
                            return;
                        }
                        future.complete(null);
                        groupsCreatedOrUpdatedCount.inc();
                    } catch (Throwable t) {
                        future.completeExceptionally(t);
                    }
                });

        return future;
    }

    public CompletableFuture<Void> deleteGroup(String groupId) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        client.preparedQuery("DELETE FROM UserGroup WHERE groupId = $1", Tuple.of(groupId), ar -> {
            try {
                if (!ar.succeeded()) {
                    future.completeExceptionally(ar.cause());
                    return;
                }
                future.complete(null);
                groupsDeletedCount.inc();
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    public CompletableFuture<Void> deleteAllGroups() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        client.query("TRUNCATE TABLE UserGroup", ar -> {
            try {
                if (!ar.succeeded()) {
                    future.completeExceptionally(ar.cause());
                    return;
                }
                int rowsDeleted = ar.result().rowCount();
                future.complete(null);
                groupsDeletedCount.inc(rowsDeleted);
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }
}
