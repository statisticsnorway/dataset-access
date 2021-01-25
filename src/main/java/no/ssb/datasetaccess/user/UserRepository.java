package no.ssb.datasetaccess.user;


import io.helidon.common.reactive.Multi;
import io.helidon.common.reactive.Single;
import io.helidon.dbclient.DbClient;
import io.helidon.metrics.RegistryFactory;
import no.ssb.dapla.auth.dataset.protobuf.User;
import no.ssb.helidon.media.protobuf.ProtobufJsonUtils;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserRepository {

    private static final Logger LOG = LoggerFactory.getLogger(UserRepository.class);

    private final DbClient client;

    private final Counter usersCreatedOrUpdatedCount = RegistryFactory.getInstance().getRegistry(MetricRegistry.Type.APPLICATION).counter("usersCreatedOrUpdatedCount");
    private final Counter usersDeletedCount = RegistryFactory.getInstance().getRegistry(MetricRegistry.Type.APPLICATION).counter("usersDeletedCount");
    private final Counter usersReadCount = RegistryFactory.getInstance().getRegistry(MetricRegistry.Type.APPLICATION).counter("usersReadCount");

    public UserRepository(DbClient client) {
        this.client = client;
    }

    public Single<User> getUser(String userId) {
        return client.execute(exec -> exec.get("SELECT userId, document::JSON FROM user_permission WHERE userId = ?", userId)
                .flatMapSingle(optDbRow -> optDbRow.map(dbRow -> {
                    String jsonDoc = dbRow.column(2).as(String.class);
                    User user = ProtobufJsonUtils.toPojo(jsonDoc, User.class);
                    usersReadCount.inc();
                    return Single.just(user);
                }).orElseGet(Single::empty))
        );
    }

    public Multi<User> getUserList(String userIdPart) {
        StringBuilder query = new StringBuilder("SELECT userId, document::JSON FROM user_permission");
        if (userIdPart != null && userIdPart.length() > 0) {
            query.append(" WHERE userId LIKE '%").append(userIdPart.replace("'", "''")).append("%'");
        }
        query.append(" ORDER BY userId");
        return client.execute(exec -> exec.query(query.toString())
                .map(dbRow -> {
                    String jsonDoc = dbRow.column(2).as(String.class);
                    User user = ProtobufJsonUtils.toPojo(jsonDoc, User.class);
                    usersReadCount.inc();
                    return user;
                })
        );
    }

    public Single<Long> createOrUpdateUser(User user) {
        return client.execute(exec -> {
            String documentJson = ProtobufJsonUtils.toString(user);
            return exec.insert("INSERT INTO user_permission (userId, document) VALUES(?, ?::JSON) ON CONFLICT (userId) DO UPDATE SET document = ?::JSON",
                    user.getUserId(), documentJson, documentJson)
                    .peek(usersCreatedOrUpdatedCount::inc);
            }
        );
    }

    public Single<Long> deleteUser(String userId) {
        return client.execute(exec -> exec.delete("DELETE FROM user_permission WHERE userId = ?",
                userId)
                .peek(usersDeletedCount::inc)
        );
    }

    public Single<Long> deleteAllUsers() {
        return client.execute(exec -> exec.delete("TRUNCATE TABLE user_permission")
                .peek(usersDeletedCount::inc)
        );
    }
}
