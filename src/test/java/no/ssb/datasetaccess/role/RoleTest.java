package no.ssb.datasetaccess.role;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RoleTest {

    @Test
    void thatFromJsonWorks() {
        JsonArray privileges = new JsonArray().add("READ");
        JsonObject json = new JsonObject(Map.of("name", "john", "privileges", privileges));

        Role actual = Role.fromJson(json);
        Role expected = new Role("john", Set.of(Privilege.READ));

        assertThat(actual).isEqualTo(expected);
    }
}
