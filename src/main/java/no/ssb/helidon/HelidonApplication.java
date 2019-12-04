package no.ssb.helidon;

import io.helidon.config.Config;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerConfiguration;
import io.helidon.webserver.WebServer;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public abstract class HelidonApplication<A> implements Registry {

    private final Registry registry;

    protected HelidonApplication() {
        Config config = loadConfig();
        registry = createRegistry(config);
        registry.add(Config.class, config);
        Routing.Builder routingBuilder = Routing.builder();
        registry.add(Routing.Builder.class, routingBuilder);
        init(config);
        registry.remove(Routing.Builder.class);
        Routing routing = routingBuilder.build();
        registry.add(Routing.class, routing);
        WebServer webServer = createWebServer(config, routing);
        registry.add(WebServer.class, webServer);
        webServer.start();
    }

    protected abstract void init(Config config);

    @Override
    public <T> Registry add(Class<T> clazz, T instance) {
        return registry.add(clazz, instance);
    }

    @Override
    public <T> Registry add(Class<T> clazz, String name, T instance) {
        return registry.add(clazz, name, instance);
    }

    @Override
    public <T> T remove(Class<T> clazz) {
        return registry.remove(clazz);
    }

    @Override
    public <T> T remove(Class<T> clazz, String name) {
        return registry.remove(clazz, name);
    }

    @Override
    public <T> T get(Class<T> clazz) {
        return registry.get(clazz);
    }

    @Override
    public <T> T get(Class<T> clazz, String name) {
        return registry.get(clazz, name);
    }

    protected Registry createRegistry(Config config) {
        return Registry.create();
    }

    protected abstract Config loadConfig();

    protected WebServer createWebServer(Config config, Routing routing) {
        ServerConfiguration configuration = ServerConfiguration.builder(config.get("webserver")).build();
        try {
            return WebServer.create(configuration, routing).start().toCompletableFuture().get(10, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    protected Routing.Builder routing() {
        return registry.get(Routing.Builder.class);
    }

    public Registry registry() {
        return registry;
    }

    public A stop() {
        try {
            registry.get(WebServer.class).shutdown().toCompletableFuture().get(2, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new RuntimeException(e);
        }
        return (A) this;
    }
}
