package no.ssb.useraccess.role;


import io.helidon.common.reactive.Multi;
import io.helidon.common.reactive.Single;
import io.helidon.dbclient.DbClient;
import io.helidon.metrics.RegistryFactory;
import no.ssb.dapla.auth.dataset.protobuf.Role;
import no.ssb.helidon.media.protobuf.ProtobufJsonUtils;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

public class RoleRepository {

    private static final Logger LOG = LoggerFactory.getLogger(RoleRepository.class);

    private final DbClient client;

    private final Counter rolesCreatedOrUpdatedCount = RegistryFactory.getInstance().getRegistry(MetricRegistry.Type.APPLICATION).counter("rolesCreatedOrUpdatedCount");
    private final Counter rolesDeletedCount = RegistryFactory.getInstance().getRegistry(MetricRegistry.Type.APPLICATION).counter("rolesDeletedCount");
    private final Counter rolesReadCount = RegistryFactory.getInstance().getRegistry(MetricRegistry.Type.APPLICATION).counter("rolesReadCount");

    public RoleRepository(DbClient client) {
        this.client = client;
    }

    public Single<Role> getRole(String roleId) {
        return client.execute(exec -> exec.get("SELECT roleId, document::JSON FROM role WHERE roleId = ?", roleId)
                .flatMapSingle(optDbRow -> optDbRow.map(dbRow -> {
                    String jsonDoc = dbRow.column(2).as(String.class);
                    Role role = ProtobufJsonUtils.toPojo(jsonDoc, Role.class);
                    rolesReadCount.inc();
                    return Single.just(role);
                }).orElseGet(Single::empty))
        );
    }

    public Multi<Role> getRoles(List<String> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return Multi.empty();
        }
        String inIds = roleIds.stream()
                .map(s -> "'" + s.replace("'", "''") + "'")
                .collect(Collectors.joining(","));
        return client.execute(exec -> exec.query("SELECT roleId, document::JSON FROM role WHERE roleId IN (" + inIds + ") ORDER BY roleId")
                .map(dbRow -> {
                    String jsonDoc = dbRow.column(2).as(String.class);
                    Role role = ProtobufJsonUtils.toPojo(jsonDoc, Role.class);
                    rolesReadCount.inc();
                    return role;
                })
        );
    }

    public Multi<Role> getRoleList(String roleIdPart) {
        StringBuilder query = new StringBuilder("SELECT roleId, document::JSON FROM role");
        if (roleIdPart != null && roleIdPart.length() > 0) {
            query.append(" WHERE roleId LIKE '%").append(roleIdPart.replace("'", "''")).append("%'");
        }
        query.append(" ORDER BY roleId");
        return client.execute(exec -> exec.query(query.toString())
                .map(dbRow -> {
                    String jsonDoc = dbRow.column(2).as(String.class);
                    Role role = ProtobufJsonUtils.toPojo(jsonDoc, Role.class);
                    rolesReadCount.inc();
                    return role;
                })
        );
    }

    public Single<Long> createOrUpdateRole(Role role) {
        return client.execute(exec -> {
                    String roleId = role.getRoleId();
                    String documentJson = ProtobufJsonUtils.toString(role);
                    return exec.createInsert("INSERT INTO role (roleId, document) VALUES(?, ?::JSON) ON CONFLICT (roleId) DO UPDATE SET document = ?::JSON")
                            .addParam(roleId)
                            .addParam(documentJson)
                            .addParam(documentJson)
                            .execute()
                            .peek(rolesCreatedOrUpdatedCount::inc);
                }
        );
    }

    public Single<Long> deleteRole(String roleId) {
        return client.execute(exec -> exec.delete("DELETE FROM role WHERE roleId = ?", roleId)
                .peek(rolesDeletedCount::inc)
        );
    }

    public Single<Long> deleteAllRoles() {
        return client.execute(exec -> exec.delete("TRUNCATE TABLE role")
                .peek(rolesDeletedCount::inc)
        );
    }
}
