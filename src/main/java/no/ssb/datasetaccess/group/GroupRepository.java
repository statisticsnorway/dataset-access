package no.ssb.datasetaccess.group;


import io.helidon.common.reactive.Multi;
import io.helidon.common.reactive.Single;
import io.helidon.dbclient.DbClient;
import io.helidon.metrics.RegistryFactory;
import no.ssb.dapla.auth.dataset.protobuf.Group;
import no.ssb.helidon.media.protobuf.ProtobufJsonUtils;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

public class GroupRepository {

    private static final Logger LOG = LoggerFactory.getLogger(GroupRepository.class);

    private final DbClient client;

    private final Counter groupsCreatedOrUpdatedCount = RegistryFactory.getInstance().getRegistry(MetricRegistry.Type.APPLICATION).counter("groupsCreatedOrUpdatedCount");
    private final Counter groupsDeletedCount = RegistryFactory.getInstance().getRegistry(MetricRegistry.Type.APPLICATION).counter("groupsDeletedCount");
    private final Counter groupsReadCount = RegistryFactory.getInstance().getRegistry(MetricRegistry.Type.APPLICATION).counter("groupsReadCount");

    public GroupRepository(DbClient client) {
        this.client = client;
    }

    public Single<Group> getGroup(String groupId) {
        return client.execute(exec -> exec.get("SELECT groupId, document::JSON FROM UserGroup WHERE groupId = ?", groupId)
                .flatMapSingle(optDbRow -> optDbRow.map(dbRow -> {
                    String jsonDoc = dbRow.column(2).as(String.class);
                    Group group = ProtobufJsonUtils.toPojo(jsonDoc, Group.class);
                    groupsReadCount.inc();
                    return Single.just(group);
                }).orElseGet(Single::empty))
        );
    }

    public Multi<Group> getGroups(List<String> groupIds) {
        if (groupIds == null || groupIds.isEmpty()) {
            return Multi.empty();
        }
        String inIds = groupIds.stream()
                .map(s -> "'" + s.replace("'", "''") + "'")
                .collect(Collectors.joining(","));
        return client.execute(exec -> exec.query("SELECT groupId, document::JSON FROM UserGroup WHERE groupId IN (" + inIds + ") ORDER BY groupId")
                .map(dbRow -> {
                    String jsonDoc = dbRow.column(2).as(String.class);
                    Group group = ProtobufJsonUtils.toPojo(jsonDoc, Group.class);
                    groupsReadCount.inc();
                    return group;
                })
        );
    }

    public Multi<Group> getAllGroups() {
        return client.execute(exec -> exec.query("SELECT groupId, document::JSON FROM UserGroup ORDER BY groupId")
                .map(dbRow -> {
                    String jsonDoc = dbRow.column(2).as(String.class);
                    Group group = ProtobufJsonUtils.toPojo(jsonDoc, Group.class);
                    groupsReadCount.inc();
                    return group;
                })
        );
    }

    public Single<Long> createOrUpdateGroup(Group group) {
        return client.execute(exec -> {
                    String documentJson = ProtobufJsonUtils.toString(group);
                    return exec.insert("INSERT INTO UserGroup (groupId, document) VALUES(?, ?::JSON) ON CONFLICT (groupId) DO UPDATE SET document = ?::JSON",
                            group.getGroupId(), documentJson, documentJson)
                            .peek(groupsCreatedOrUpdatedCount::inc);
                }
        );
    }

    public Single<Long> deleteGroup(String groupId) {
        return client.execute(exec -> exec.delete("DELETE FROM UserGroup WHERE groupId = ?",
                groupId)
                .peek(groupsDeletedCount::inc)
        );
    }

    public Single<Long> deleteAllGroups() {
        return client.execute(exec -> exec.delete("TRUNCATE TABLE UserGroup")
                .peek(groupsDeletedCount::inc)
        );
    }
}
