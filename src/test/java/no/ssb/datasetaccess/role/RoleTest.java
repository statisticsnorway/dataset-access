package no.ssb.datasetaccess.role;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import no.ssb.datasetaccess.dataset.DatasetState;
import no.ssb.datasetaccess.dataset.Valuation;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;

class RoleTest {

    @Test
    void thatFromJsonWorks() {
        JsonArray privileges = new JsonArray().add(Privilege.READ.name());
        JsonArray states = new JsonArray().add(DatasetState.RAW.name());
        JsonArray namespacePrefixes = new JsonArray().add("/ns/a/b/c");
        JsonObject json = new JsonObject()
                .put("roleId", "john")
                .put("privileges", privileges)
                .put("namespacePrefixes", namespacePrefixes)
                .put("maxValuation", Valuation.INTERNAL)
                .put("states", states);

        Role actual = Role.fromJson(json);
        Role expected = new Role("john", Set.of(Privilege.READ), new TreeSet<>(Set.of("/ns/a/b/c")), Valuation.INTERNAL, Set.of(DatasetState.RAW));

        assertThat(actual).isEqualTo(expected);
    }
}
