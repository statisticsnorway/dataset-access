package no.ssb.datasetaccess;


import ch.qos.logback.classic.util.ContextInitializer;
import io.helidon.config.Config;
import io.helidon.config.spi.ConfigSource;
import io.helidon.media.jackson.server.JacksonSupport;
import io.helidon.metrics.MetricsSupport;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerConfiguration;
import io.helidon.webserver.WebServer;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import no.ssb.datasetaccess.access.AccessService;
import no.ssb.datasetaccess.role.RoleRepository;
import no.ssb.datasetaccess.role.RoleService;
import no.ssb.datasetaccess.user.UserRepository;
import no.ssb.datasetaccess.user.UserService;
import org.flywaydb.core.Flyway;
import org.postgresql.ds.PGSimpleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import static io.helidon.config.ConfigSources.classpath;
import static io.helidon.config.ConfigSources.file;

public class Application {

    private static final Logger LOG;

    static {
        String logbackConfigurationFile = System.getenv("LOGBACK_CONFIGURATION_FILE");
        if (logbackConfigurationFile != null) {
            System.setProperty(ContextInitializer.CONFIG_FILE_PROPERTY, logbackConfigurationFile);
        }
        LOG = LoggerFactory.getLogger(Application.class);
    }

    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();
        List<Supplier<ConfigSource>> configSourceSupplierList = new LinkedList<>();
        String overrideFile = System.getenv("HELIDON_CONFIG_FILE");
        if (overrideFile != null) {
            configSourceSupplierList.add(file(overrideFile).optional());
        }
        configSourceSupplierList.add(file("conf/application.yaml").optional());
        configSourceSupplierList.add(classpath("application.yaml"));
        Application application = new Application(Config.builder().sources(configSourceSupplierList).build());
        application.start().thenAccept(webServer -> LOG.info("Webserver running at port: {}, started in {} ms", webServer.port(), System.currentTimeMillis() - startTime));
    }

    private final Map<Class<?>, Object> instanceByType = new ConcurrentHashMap<>();

    public <T> T put(Class<T> clazz, T instance) {
        return (T) instanceByType.put(clazz, instance);
    }

    public <T> T get(Class<T> clazz) {
        return (T) instanceByType.get(clazz);
    }

    public Application(Config config) {
        put(Config.class, config);

        checkDatabaseConnectivity(config);

        migrateDatabaseSchema(config.get("flyway"));

        // repositories
        PgPool pgPool = initPgPool(config.get("pgpool"));
        UserRepository userRepository = new UserRepository(pgPool);
        RoleRepository roleRepository = new RoleRepository(pgPool);
        put(PgPool.class, pgPool);
        put(UserRepository.class, userRepository);
        put(RoleRepository.class, roleRepository);

        // routing
        Routing routing = Routing.builder()
                .register(JacksonSupport.create())
                .register(MetricsSupport.create())
                .register("/role", new RoleService(roleRepository))
                .register("/user", new UserService(userRepository))
                .register("/access", new AccessService(userRepository, roleRepository))
                .build();
        put(Routing.class, routing);

        // web-server
        ServerConfiguration configuration = ServerConfiguration.builder(config.get("webserver")).build();
        WebServer webServer;
        try {
            webServer = WebServer.create(configuration, routing).start().toCompletableFuture().get(10, TimeUnit.SECONDS);
            put(WebServer.class, webServer);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    private void checkDatabaseConnectivity(Config config) {
        String host = config.get("pgpool.connect-options.host").asString().orElse("somehost");
        int port = config.get("pgpool.connect-options.port").asInt().orElse(5432);
        String database = config.get("pgpool.connect-options.database").asString().orElse("somedb");
        String user = config.get("pgpool.connect-options.user").asString().orElse("someuser");
        String password = config.get("pgpool.connect-options.password").asString().orElse("somepassword");

        PGSimpleDataSource ds = new PGSimpleDataSource();
        ds.setServerName(host);
        ds.setPortNumber(port);
        ds.setDatabaseName(database);
        ds.setUser(user);
        ds.setPassword(password);
        try (Connection connection = ds.getConnection()) {
            connection.createStatement().execute("SELECT 1");
            LOG.info("Successfully connected to {}:{}/{} with user {} and password ****", host, port, database, user);
        } catch (SQLException e) {
            LOG.error("Unable to connect to {}:{}/{} with user {} and password ****", host, port, database, user);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private void migrateDatabaseSchema(Config flywayConfig) {
        Flyway flyway = Flyway.configure()
                .dataSource(
                        flywayConfig.get("url").asString().orElse("jdbc:postgresql://localhost:15432/rdc"),
                        flywayConfig.get("user").asString().orElse("rdc"),
                        flywayConfig.get("password").asString().orElse("rdc")
                )
                .load();
        flyway.migrate();
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

    public CompletionStage<WebServer> start() {
        return get(WebServer.class).start();
    }

    public Application stop() {
        try {
            get(WebServer.class).shutdown().toCompletableFuture().get(2, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new RuntimeException(e);
        }
        return this;
    }
}