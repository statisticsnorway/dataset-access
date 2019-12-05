package no.ssb.datasetaccess;


import io.helidon.config.Config;
import io.helidon.media.jackson.server.JacksonSupport;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static io.helidon.config.ConfigSources.classpath;

public class Application {

    private static final Logger LOG = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();
        Application application = new Application(Config.builder()
                .sources(classpath("application.yaml"))
                .build());
        application.start().thenAccept(webServer -> LOG.info("Webserver running at port: {}, started in {} ms", webServer.port(), System.currentTimeMillis() - startTime));
    }

    private final Map<Class<?>, Object> beans = new ConcurrentHashMap<>();

    public <T> T get(Class<T> clazz) {
        return (T) beans.get(clazz);
    }

    public Application(Config config) {
        beans.put(Config.class, config);

        // repositories
        PgPool pgPool = initPgPool(config.get("pgpool"));
        UserRepository userRepository = new UserRepository(pgPool);
        RoleRepository roleRepository = new RoleRepository(pgPool);
        beans.put(PgPool.class, pgPool);
        beans.put(UserRepository.class, userRepository);
        beans.put(RoleRepository.class, roleRepository);

        // routing
        Routing routing = Routing.builder()
                .register(JacksonSupport.create())
                .register("/role", new RoleService(roleRepository))
                .register("/user", new UserService(userRepository))
                .register("/access", new AccessService(userRepository, roleRepository))
                .build();
        beans.put(Routing.class, routing);

        // web-server
        ServerConfiguration configuration = ServerConfiguration.builder(config.get("webserver")).build();
        WebServer webServer;
        try {
            webServer = WebServer.create(configuration, routing).start().toCompletableFuture().get(10, TimeUnit.SECONDS);
            beans.put(WebServer.class, webServer);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new RuntimeException(e);
        }
        webServer.start();
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