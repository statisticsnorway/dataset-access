package no.ssb.datasetaccess;

import io.helidon.config.Config;
import io.helidon.webserver.WebServer;
import no.ssb.testing.helidon.TestClient;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static io.helidon.config.ConfigSources.classpath;
import static io.helidon.config.ConfigSources.file;
import static java.util.Optional.ofNullable;

class UserAccessApplicationTest {

    @Test
    void thatApplicationStackCanBeStarted() throws InterruptedException, ExecutionException, TimeoutException {
        Config.Builder builder = Config.builder();
        String overrideFile = ofNullable(System.getProperty("helidon.config.file"))
                .orElseGet(() -> System.getenv("HELIDON_CONFIG_FILE"));
        if (overrideFile != null) {
            builder.addSource(file(overrideFile).optional());
        }
        String profile = ofNullable(System.getProperty("helidon.config.profile"))
                .orElseGet(() -> ofNullable(System.getenv("HELIDON_CONFIG_PROFILE"))
                        .orElse("dev") // default
                );
        String profileFilename = String.format("application-%s.yaml", profile);
        builder.addSource(file(profileFilename).optional());
        builder.addSource(classpath(profileFilename).optional());
        builder.addSource(file("conf/application.yaml").optional());
        builder.addSource(classpath("application.yaml"));
        Config config = builder.build();

        UserAccessApplication application = new UserAccessApplication(config);
        try {
            application.start().toCompletableFuture().get(5, TimeUnit.SECONDS);
            TestClient client = TestClient.newClient("localhost", application.get(WebServer.class).port());
            client.put("/user/a", "{\"userId\": \"a\", \"roles\": []}", null).expect201Created();
            client.get("/user/a").expect200Ok();
        } finally {
            application.stop();
        }
    }
}
