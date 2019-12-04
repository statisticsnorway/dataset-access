package no.ssb.datasetaccess.testing;

import io.helidon.webserver.WebServer;
import no.ssb.datasetaccess.Application;
import no.ssb.helidon.Registry;

public class TestServer implements Registry {

    final Application application = new Application();

    public TestClient client() {
        return TestClient.newClient("localhost", application.get(WebServer.class).port());
    }

    public Application getApplication() {
        return application;
    }

    public void shutdown() {
        application.stop();
    }

    @Override
    public <T> Registry add(Class<T> clazz, T instance) {
        return application.registry().add(clazz, instance);
    }

    @Override
    public <T> Registry add(Class<T> clazz, String name, T instance) {
        return application.registry().add(clazz, name, instance);
    }

    @Override
    public <T> T remove(Class<T> clazz) {
        return application.registry().remove(clazz);
    }

    @Override
    public <T> T remove(Class<T> clazz, String name) {
        return application.registry().remove(clazz, name);
    }

    @Override
    public <T> T get(Class<T> clazz) {
        return application.registry().get(clazz);
    }

    @Override
    public <T> T get(Class<T> clazz, String name) {
        return application.registry().get(clazz, name);
    }
}
