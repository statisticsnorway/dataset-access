package no.ssb.datasetaccess;


import io.grpc.MethodDescriptor;
import io.helidon.config.Config;
import io.helidon.grpc.server.GrpcRouting;
import io.helidon.grpc.server.GrpcServer;
import io.helidon.grpc.server.GrpcServerConfiguration;
import io.helidon.grpc.server.GrpcTracingConfig;
import io.helidon.grpc.server.ServerRequestAttribute;
import io.helidon.metrics.MetricsSupport;
import io.helidon.tracing.TracerBuilder;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerConfiguration;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.accesslog.AccessLogSupport;
import io.opentracing.Tracer;
import io.opentracing.contrib.grpc.OperationNameConstructor;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import no.ssb.datasetaccess.access.AccessGrpcService;
import no.ssb.datasetaccess.access.AccessHttpService;
import no.ssb.datasetaccess.access.AccessService;
import no.ssb.datasetaccess.health.Health;
import no.ssb.datasetaccess.health.HealthAwarePgPool;
import no.ssb.datasetaccess.health.ReadinessSample;
import no.ssb.datasetaccess.role.RoleHttpService;
import no.ssb.datasetaccess.role.RoleRepository;
import no.ssb.datasetaccess.user.UserHttpService;
import no.ssb.datasetaccess.user.UserRepository;
import no.ssb.helidon.application.AuthorizationInterceptor;
import no.ssb.helidon.application.DefaultHelidonApplication;
import no.ssb.helidon.application.HelidonApplication;
import no.ssb.helidon.application.HelidonGrpcWebTranscoding;
import no.ssb.helidon.media.protobuf.ProtobufJsonSupport;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class Application extends DefaultHelidonApplication {

    private static final Logger LOG;

    static {
        installSlf4jJulBridge();
        LOG = LoggerFactory.getLogger(Application.class);
    }

    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();
        new ApplicationBuilder().build().start()
                .toCompletableFuture()
                .orTimeout(10, TimeUnit.SECONDS)
                .thenAccept(app -> LOG.info("Webserver running at port: {}, Grpcserver running at port: {}, started in {} ms",
                        app.get(WebServer.class).port(), app.get(GrpcServer.class).port(), System.currentTimeMillis() - startTime))
                .exceptionally(throwable -> {
                    LOG.error("While starting application", throwable);
                    System.exit(1);
                    return null;
                });
    }

    public Application(Config config) {
        put(Config.class, config);

        TracerBuilder<?> tracerBuilder = TracerBuilder.create(config.get("tracing")).registerGlobal(true);
        Tracer tracer = tracerBuilder.build();

        AtomicReference<ReadinessSample> lastReadySample = new AtomicReference<>(new ReadinessSample(false, System.currentTimeMillis()));

        // reactive postgres client
        PgPool pgPool = new HealthAwarePgPool(initPgPool(config.get("pgpool")), lastReadySample);

        // initialize health, including a database connectivity wait-loop
        Health health = new Health(config, pgPool, lastReadySample, () -> get(WebServer.class));

        // schema migration using flyway and jdbc
        migrateDatabaseSchema(config.get("flyway"));

        // repositories
        UserRepository userRepository = new UserRepository(pgPool);
        RoleRepository roleRepository = new RoleRepository(pgPool);
        put(PgPool.class, pgPool);
        put(UserRepository.class, userRepository);
        put(RoleRepository.class, roleRepository);

        // services
        AccessService accessService = new AccessService(userRepository, roleRepository);
        AccessGrpcService accessGrpcService = new AccessGrpcService(accessService);

        // grpc-server
        GrpcServer grpcServer = GrpcServer.create(
                GrpcServerConfiguration.builder(config.get("grpcserver"))
                        .tracer(tracer)
                        .tracingConfig(GrpcTracingConfig.builder()
                                .withStreaming()
                                .withVerbosity()
                                .withOperationName(new OperationNameConstructor() {
                                    @Override
                                    public <ReqT, RespT> String constructOperationName(MethodDescriptor<ReqT, RespT> method) {
                                        return "Grpc server received " + method.getFullMethodName();
                                    }
                                })
                                .withTracedAttributes(ServerRequestAttribute.CALL_ATTRIBUTES,
                                        ServerRequestAttribute.HEADERS,
                                        ServerRequestAttribute.METHOD_NAME)
                                .build()
                        ),
                GrpcRouting.builder()
                        .intercept(new AuthorizationInterceptor())
                        .register(accessGrpcService)
                        .build()
        );
        put(GrpcServer.class, grpcServer);

        // routing
        Routing routing = Routing.builder()
                .register(AccessLogSupport.create(config.get("webserver.access-log")))
                .register(ProtobufJsonSupport.create())
                .register(MetricsSupport.create())
                .register(health)
                .register("/role", new RoleHttpService(roleRepository))
                .register("/user", new UserHttpService(userRepository))
                .register("/access", new AccessHttpService(accessService))
                .register("/rpc/", new HelidonGrpcWebTranscoding(accessGrpcService))
                .build();
        put(Routing.class, routing);

        // web-server
        WebServer webServer = WebServer.create(
                ServerConfiguration.builder(config.get("webserver"))
                        .tracer(tracer)
                        .build(),
                routing);
        put(WebServer.class, webServer);
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

    @Override
    public CompletionStage<HelidonApplication> stop() {
        return super.stop()
                .thenCombine(CompletableFuture.runAsync(() -> get(PgPool.class).close()), (a, v) -> this);
    }
}