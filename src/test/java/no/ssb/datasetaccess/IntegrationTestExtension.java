package no.ssb.datasetaccess;

import io.helidon.config.Config;
import io.helidon.webserver.WebServer;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import javax.inject.Inject;
import java.lang.reflect.Field;

import static io.helidon.config.ConfigSources.classpath;

public class IntegrationTestExtension implements BeforeEachCallback, BeforeAllCallback, AfterAllCallback {

    Application application;
    TestClient client;

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        application = new Application(Config.builder()
                .sources(classpath("application.yaml"))
                .sources(classpath("application-test.yaml").optional())
                .build());
        application.start().toCompletableFuture().join();
        client = TestClient.newClient("localhost", application.get(WebServer.class).port());
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        Object test = context.getRequiredTestInstance();
        Field[] fields = test.getClass().getDeclaredFields();
        for (Field field : fields) {
            // application
            if (field.isAnnotationPresent(Inject.class) && Application.class.isAssignableFrom(field.getType())) {
                try {
                    field.setAccessible(true);
                    if (field.get(test) == null) {
                        field.set(test, application);
                    }
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
            // test client
            if (field.isAnnotationPresent(Inject.class) && TestClient.class.isAssignableFrom(field.getType())) {
                try {
                    field.setAccessible(true);
                    if (field.get(test) == null) {
                        field.set(test, client);
                    }
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        application.stop();
    }
}
