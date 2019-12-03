package no.ssb.datasetaccess;


import io.helidon.config.Config;
import io.helidon.media.jackson.server.JacksonSupport;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerConfiguration;
import io.helidon.webserver.WebServer;
import io.reactiverse.pgclient.PgPoolOptions;
import io.reactiverse.reactivex.pgclient.PgPool;
import no.ssb.datasetaccess.role.RoleController;
import no.ssb.datasetaccess.role.RoleRepository;
import no.ssb.datasetaccess.user.UserRepository;

import static io.helidon.config.ConfigSources.classpath;

public class Application {

    public static void main(String[] args) {
        new Application();
    }

    final UserRepository userRepository;
    final RoleRepository roleRepository;

    public Application() {
        PgPoolOptions pgPoolOptions = new PgPoolOptions()
                .setPort(15432)
                .setHost("localhost")
                .setDatabase("rdc")
                .setUser("rdc")
                .setPassword("rdc")
                .setMaxSize(5);

        // Create the client pool
        PgPool client = PgPool.pool(pgPoolOptions);

        userRepository = new UserRepository(client);
        roleRepository = new RoleRepository(client);

        final JacksonSupport jacksonSupport = JacksonSupport.create();
        final Routing.Builder routingBuilder = Routing.builder();
        routingBuilder.register(jacksonSupport);

        Routing routing = routingBuilder
                .get("/hello", (req, res) -> res.send("Hello World!"))
                .register("/role", new RoleController(roleRepository))
                .build();

        Config config = Config.builder()
                .sources(classpath("application.yaml"))
                .build();

        ServerConfiguration configuration = ServerConfiguration.create(config);

        WebServer webServer = WebServer.create(configuration, routing);
    }

    public UserRepository getUserRepository() {
        return userRepository;
    }

    public RoleRepository getRoleRepository() {
        return roleRepository;
    }
}