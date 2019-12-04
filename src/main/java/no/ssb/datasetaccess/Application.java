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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static io.helidon.config.ConfigSources.classpath;

public class Application {

    private static final Logger LOG = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        Application application = new Application();
        LOG.info("Webserver running at port: {}", application.webServer.port());
    }

    final UserRepository userRepository;
    final RoleRepository roleRepository;
    final WebServer webServer;

    public Application() {
        PgConnectOptions connectOptions = new PgConnectOptions()
                .setPort(15432)
                .setHost("localhost")
                .setDatabase("rdc")
                .setUser("rdc")
                .setPassword("rdc");

        PoolOptions poolOptions = new PoolOptions()
                .setMaxSize(5);

        PgPool client = PgPool.pool(connectOptions, poolOptions);

        userRepository = new UserRepository(client);
        roleRepository = new RoleRepository(client);

        final JacksonSupport jacksonSupport = JacksonSupport.create();
        final Routing.Builder routingBuilder = Routing.builder();
        routingBuilder.register(jacksonSupport);

        Routing routing = routingBuilder
                .get("/hello", (req, res) -> res.send("Hello World!"))
                .register("/role", new RoleService(roleRepository))
                .register("/user", new UserService(userRepository))
                .register("/access", new AccessService(userRepository, roleRepository))
                .build();

        Config config = Config.builder()
                .sources(classpath("application.yaml"))
                .build();

        ServerConfiguration configuration = ServerConfiguration.builder(config).build();

        try {
            webServer = WebServer.create(configuration, routing).start().toCompletableFuture().get(10, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    public UserRepository getUserRepository() {
        return userRepository;
    }

    public RoleRepository getRoleRepository() {
        return roleRepository;
    }

    public WebServer getWebServer() {
        return webServer;
    }
}