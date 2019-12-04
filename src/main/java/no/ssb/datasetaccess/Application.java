package no.ssb.datasetaccess;


import io.helidon.config.Config;
import io.helidon.media.jackson.server.JacksonSupport;
import io.helidon.webserver.WebServer;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import no.ssb.datasetaccess.access.AccessService;
import no.ssb.datasetaccess.role.RoleRepository;
import no.ssb.datasetaccess.role.RoleService;
import no.ssb.datasetaccess.user.UserRepository;
import no.ssb.datasetaccess.user.UserService;
import no.ssb.helidon.HelidonApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.helidon.config.ConfigSources.classpath;

public class Application extends HelidonApplication<Application> {

    private static final Logger LOG = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        Application application = new Application();
        LOG.info("Webserver running at port: {}", application.registry().get(WebServer.class).port());
    }

    protected Config loadConfig() {
        return Config.builder()
                .sources(classpath("application.yaml"))
                .build();
    }

    @Override
    protected void init(Config config) {

        // repositories
        PgPool pgPool = initPgPool(config.get("pgpool"));
        UserRepository userRepository = new UserRepository(pgPool);
        RoleRepository roleRepository = new RoleRepository(pgPool);

        add(PgPool.class, pgPool);
        add(UserRepository.class, userRepository);
        add(RoleRepository.class, roleRepository);

        // routing
        routing().register(JacksonSupport.create())
                .register("/role", new RoleService(roleRepository))
                .register("/user", new UserService(userRepository))
                .register("/access", new AccessService(userRepository, roleRepository))
        ;
    }

    private PgPool initPgPool(Config config) {
        Config connectConfig = config.get("connect-options");
        PgConnectOptions connectOptions = new PgConnectOptions()
                .setPort(connectConfig.get("port").asInt().orElse(15432))
                .setHost(connectConfig.get("host").asString().orElse("localhost"))
                .setDatabase(connectConfig.get("database").asString().orElse("rdc"))
                .setUser(connectConfig.get("user").asString().orElse("rdc"))
                .setPassword(connectConfig.get("password").asString().orElse("rdc"));

        Config poolConfig = config.get("pool-options");
        PoolOptions poolOptions = new PoolOptions()
                .setMaxSize(poolConfig.get("max-size").asInt().orElse(5));

        return PgPool.pool(connectOptions, poolOptions);
    }
}