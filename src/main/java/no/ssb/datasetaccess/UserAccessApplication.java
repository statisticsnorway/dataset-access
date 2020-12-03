package no.ssb.datasetaccess;


import io.helidon.common.reactive.Single;
import io.helidon.config.Config;
import io.helidon.dbclient.DbClient;
import io.helidon.health.HealthSupport;
import io.helidon.health.checks.HealthChecks;
import io.helidon.metrics.MetricsSupport;
import io.helidon.tracing.TracerBuilder;
import io.helidon.webserver.Routing;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.WebTracingConfig;
import io.helidon.webserver.accesslog.AccessLogSupport;
import io.opentracing.Tracer;
import no.ssb.datasetaccess.access.AccessHttpService;
import no.ssb.datasetaccess.access.AccessService;
import no.ssb.datasetaccess.group.GroupHttpService;
import no.ssb.datasetaccess.group.GroupRepository;
import no.ssb.datasetaccess.maintenance.MaintenanceHttpService;
import no.ssb.datasetaccess.maintenance.MaintenanceRepository;
import no.ssb.datasetaccess.role.RoleHttpService;
import no.ssb.datasetaccess.role.RoleRepository;
import no.ssb.datasetaccess.user.UserHttpService;
import no.ssb.datasetaccess.user.UserRepository;
import no.ssb.helidon.application.DefaultHelidonApplication;
import no.ssb.helidon.application.HelidonApplication;
import no.ssb.helidon.media.protobuf.ProtobufJsonSupport;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.util.Optional.ofNullable;

public class UserAccessApplication extends DefaultHelidonApplication implements HelidonApplication {

    private static final Logger LOG;

    static {
        installSlf4jJulBridge();
        LOG = LoggerFactory.getLogger(UserAccessApplication.class);
    }

    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();
        new UserAccessApplicationBuilder().build().start()
                .toCompletableFuture()
                .orTimeout(10, TimeUnit.SECONDS)
                .thenAccept(app -> LOG.info("Webserver running at port: {}, started in {} ms",
                        app.get(WebServer.class).port(), System.currentTimeMillis() - startTime))
                .exceptionally(throwable -> {
                    LOG.error("While starting application", throwable);
                    System.exit(1);
                    return null;
                });
    }

    public UserAccessApplication(Config config) {
        put(Config.class, config);

        TracerBuilder<?> tracerBuilder = TracerBuilder.create(config.get("tracing")).registerGlobal(false);
        Tracer tracer = tracerBuilder.build();

        // schema migration using flyway and jdbc
        migrateDatabaseSchema(config.get("flyway"));

        DbClient dbClient = DbClient.builder()
                .config(config.get("db"))
                // .mapperProvider(new JsonProcessingMapperProvider())
                .build();

        HealthSupport health = HealthSupport.builder()
                .addLiveness(HealthChecks.healthChecks())
                .addReadiness()
                .build();

        // repositories
        UserRepository userRepository = new UserRepository(dbClient);
        GroupRepository groupRepository = new GroupRepository(dbClient);
        RoleRepository roleRepository = new RoleRepository(dbClient);
        MaintenanceRepository maintenanceRepository = new MaintenanceRepository(roleRepository, groupRepository, userRepository);
        put(DbClient.class, dbClient);
        put(UserRepository.class, userRepository);
        put(GroupRepository.class, groupRepository);
        put(RoleRepository.class, roleRepository);
        put(MaintenanceRepository.class, maintenanceRepository);

        // services
        AccessService accessService = new AccessService(userRepository, groupRepository, roleRepository);
        ScheduledExecutorService timeoutService = Executors.newSingleThreadScheduledExecutor();

        // routing
        Routing routing = Routing.builder()
                .register(AccessLogSupport.create(config.get("webserver.access-log")))
                .register(WebTracingConfig.create(config.get("tracing")))
                .register(MetricsSupport.create())
                .register(health)
                .register("/role", new RoleHttpService(timeoutService, roleRepository))
                .register("/user", new UserHttpService(timeoutService, userRepository))
                .register("/group", new GroupHttpService(timeoutService, groupRepository))
                .register("/maintenance", new MaintenanceHttpService(timeoutService, maintenanceRepository))
                .register("/access", new AccessHttpService(timeoutService, accessService))
                .build();
        put(Routing.class, routing);

        // web-server
        WebServer webServer = WebServer.builder()
                .config(config.get("webserver"))
                .tracer(tracer)
                .addMediaSupport(ProtobufJsonSupport.create())
                .routing(routing)
                .build();
        put(WebServer.class, webServer);
    }

    private void migrateDatabaseSchema(Config flywayConfig) {
        Flyway flyway = Flyway.configure()
                .dataSource(
                        flywayConfig.get("url").asString().orElse("jdbc:postgresql://localhost:15432/rdc"),
                        flywayConfig.get("user").asString().orElse("rdc"),
                        flywayConfig.get("password").asString().orElse("rdc")
                )
                .connectRetries(flywayConfig.get("connect-retries").asInt().orElse(120))
                .load();
        flyway.migrate();
    }

    public Single<UserAccessApplication> start() {
        return ofNullable(get(WebServer.class))
                .map(webServer -> webServer.start().map(ws -> this))
                .orElse(Single.just(this));
    }

    public Single<UserAccessApplication> stop() {
        return Single.create(ofNullable(get(WebServer.class))
                .map(webServer -> webServer.shutdown().map(ws -> this))
                .orElse(Single.just(this))
        );
    }
}