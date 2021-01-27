package no.ssb.datasetaccess.autocreate;

import io.helidon.common.reactive.Single;
import io.helidon.config.Config;
import io.helidon.config.ConfigSources;
import io.opentracing.Span;
import no.ssb.dapla.auth.dataset.protobuf.User;
import no.ssb.datasetaccess.autocreate.model.AutoCreate;
import no.ssb.datasetaccess.autocreate.model.AutoCreateItem;
import no.ssb.datasetaccess.autocreate.model.Role;
import no.ssb.datasetaccess.group.GroupRepository;
import no.ssb.datasetaccess.role.RoleRepository;
import no.ssb.datasetaccess.user.UserRepository;
import no.ssb.datasetaccess.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class AutoCreateService {
    private static final Logger LOG = LoggerFactory.getLogger(AutoCreateService.class);

    final UserRepository userRepository;
    final GroupRepository groupRepository;
    final RoleRepository roleRepository;
    final Config autoCreateConfig;
    final AutoCreate autoCreate;

    public AutoCreateService(Config config, UserRepository userRepository, GroupRepository groupRepository, RoleRepository roleRepository) {

        autoCreateConfig = Config
                .builder()
                .disableSystemPropertiesSource()
                .disableEnvironmentVariablesSource()
                .sources(ConfigSources.classpath(config.get("filename").asString().get()))
                .build();

        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
        this.roleRepository = roleRepository;
        autoCreate = autoCreateConfig.get("")
                .as(AutoCreate.class)
                .get();
    }
    private boolean shouldAutoCreate(String userId, AutoCreate autoCreate) {
        return StringUtil.isValidEmail(userId)
                && matchDomain(userId, autoCreate);
    }

    private boolean matchDomain(String userId, AutoCreate autoCreate) {
        List<AutoCreateItem> items = autoCreate.getItems();
        if(!items.isEmpty()) {
            return StringUtil.getDomain(userId).get().equalsIgnoreCase(items.get(0).getDomain().getDomain());
        } else {
            return false;
        }
    }

    public Single<User> createNewUser(String userId, Span span) {
        if (this.shouldAutoCreate(userId, this.getAutoCreate())) {
            span.log("auto-creating new user");
            return Single.just(this.create(userId, span));
        }
        return Single.empty();
    }

    private User create(String userId, Span span) {
        try {
            if ( autoCreate.getItems().stream().findFirst().isPresent() ) {
                List<AutoCreateItem> items = autoCreate.getItems();
                if (items.isEmpty()) {
                    throw new IllegalArgumentException("autoCreate.getItems() can not be empty");
                }
                String userIdPart = StringUtil.getUserIdPart(userId).get();
                User user = items.get(0).getUser().toProtoUser(userId);
                List<Role> roles = items.get(0).getRoles();
                userRepository.createOrUpdateUser(user).await(1, TimeUnit.SECONDS);
                roles.forEach(role -> {
                    roleRepository.createOrUpdateRole(role.toRole(userIdPart)).await(1, TimeUnit.SECONDS);
                });
                span.log("new user created for "+ userId);
                return user;
            }
        } catch (RuntimeException | Error e) {
            LOG.error("unexpected error", e);
            throw e;
        }
        return null;
    }

    public AutoCreate getAutoCreate() {
        return autoCreate;
    }
}
