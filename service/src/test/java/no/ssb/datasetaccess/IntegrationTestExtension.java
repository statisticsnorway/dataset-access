package no.ssb.datasetaccess;

import io.helidon.config.Config;
import io.helidon.config.spi.ConfigSource;
import io.helidon.webserver.WebServer;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import javax.inject.Inject;
import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static io.helidon.config.ConfigSources.classpath;
import static io.helidon.config.ConfigSources.file;

public class IntegrationTestExtension implements BeforeEachCallback, BeforeAllCallback, AfterAllCallback {

    Application application;
    TestClient client;

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        List<Supplier<ConfigSource>> configSourceSupplierList = new LinkedList<>();
        String overrideFile = System.getenv("HELIDON_CONFIG_FILE");
        if (overrideFile != null) {
            configSourceSupplierList.add(file(overrideFile).optional());
        }
        String profile = System.getenv("HELIDON_CONFIG_PROFILE");
        if (profile == null) {
            profile = "dev";
        }
        if (profile.equalsIgnoreCase("dev")) {
            configSourceSupplierList.add(classpath("application-dev.yaml"));
        } else if (profile.equalsIgnoreCase("drone")) {
            configSourceSupplierList.add(classpath("application-drone.yaml"));
        } else {
            // default to dev
            configSourceSupplierList.add(classpath("application-dev.yaml"));
        }
        configSourceSupplierList.add(classpath("application.yaml"));
        application = new Application(Config.builder().sources(configSourceSupplierList).build());
        application.start().toCompletableFuture().get(5, TimeUnit.SECONDS);
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
