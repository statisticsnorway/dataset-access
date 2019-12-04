package no.ssb.datasetaccess.testing;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import javax.inject.Inject;
import java.lang.reflect.Field;

public class IntegrationTestExtension implements BeforeEachCallback, BeforeAllCallback, AfterAllCallback {

    TestServer server;
    TestClient client;

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        server = new TestServer();
        client = server.client();
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        Object test = context.getRequiredTestInstance();
        Field[] fields = test.getClass().getDeclaredFields();
        for (Field field : fields) {
            // test server
            if (field.isAnnotationPresent(Inject.class) && TestServer.class.isAssignableFrom(field.getType())) {
                try {
                    field.setAccessible(true);
                    if (field.get(test) == null) {
                        field.set(test, server);
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
        server.shutdown();
    }
}
