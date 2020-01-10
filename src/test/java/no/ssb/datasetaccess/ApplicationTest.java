package no.ssb.datasetaccess;

import io.helidon.config.Config;
import io.helidon.config.spi.ConfigSource;
import io.helidon.webserver.WebServer;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import static io.helidon.config.ConfigSources.classpath;
import static io.helidon.config.ConfigSources.file;

class ApplicationTest {

    @Test
    void thatApplicationStackCanBeStarted() throws InterruptedException, ExecutionException, TimeoutException {
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
        Application application = new Application(Config.builder().sources(configSourceSupplierList).build());
        try {
            application.start().toCompletableFuture().get(5, TimeUnit.SECONDS);
            TestClient client = TestClient.newClient("localhost", application.get(WebServer.class).port());
            client.put("/user/a", "{\"userId\": \"a\", \"roles\": []}").expect201Created();
            client.get("/user/a").expect200Ok();
        } finally {
            application.stop();
        }
    }
}
