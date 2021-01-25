package no.ssb.datasetaccess.autocreate;

import io.helidon.config.Config;
import io.helidon.config.ConfigSources;
import io.helidon.config.spi.ConfigNode;
import no.ssb.datasetaccess.autocreate.model.AutoCreate;
import no.ssb.datasetaccess.autocreate.model.Domain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

 class AutoCreateTest {

    AutoCreate autoCreate;
    Config autoCreateConfig;

    @BeforeEach
    public void AccessServiceTest() {

        Config config = Config.create(ConfigSources.create(
                ConfigNode.ObjectNode.builder()
                        .addValue("filename", "autocreate-test.yaml")
                        .build()));

        autoCreateConfig = Config
                .builder()
                .disableSystemPropertiesSource()
                .disableEnvironmentVariablesSource()
                .sources(ConfigSources.classpath(config.get("filename").asString().get()))
                .build();

        autoCreate = autoCreateConfig.get("")
                .as(AutoCreate.class)
                .get();
    }

    @Test
    void autocreateTest() {
        Config config = Config.create(ConfigSources.create(
                ConfigNode.ObjectNode.builder()
                        .addValue("domain", "ssb.no")
                        .build()));

        AutoCreate autoCreate = config.get("")
                .as(AutoCreate.class)
                .get();

        Domain domain = new Domain("ssb.no");
        assertEquals(autoCreate.getItems().stream().findFirst().get().getDomain(), domain);
    }
}
