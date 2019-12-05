package no.ssb.datasetaccess.role;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import no.ssb.datasetaccess.JacksonUtils;
import no.ssb.datasetaccess.dataset.DatasetState;
import no.ssb.datasetaccess.dataset.Valuation;

import java.util.Collections;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class Role {
    String roleId;
    Set<Privilege> privileges;
    NavigableSet<String> namespacePrefixes;
    Valuation maxValuation;
    Set<DatasetState> states;

    public Role() {
        roleId = "";
        privileges = Collections.emptySet();
        namespacePrefixes = Collections.emptyNavigableSet();
        maxValuation = Valuation.OPEN;
        states = Collections.emptySet();
    }

    public Role(String roleId, Set<Privilege> privileges, NavigableSet<String> namespacePrefixes, Valuation maxValuation, Set<DatasetState> states) {
        this.roleId = roleId;
        this.privileges = privileges;
        this.namespacePrefixes = namespacePrefixes;
        this.maxValuation = maxValuation;
        this.states = states;
    }

    public static Role fromJackson(JsonNode json) {
        try {
            return JacksonUtils.mapper.treeToValue(json, Role.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static Role fromJsonString(String json) {
        try {
            return JacksonUtils.mapper.readValue(json, Role.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static Role fromVertxJson(JsonObject json) {
        String roleId = json.getString("roleId");
        Set<Privilege> privileges = json.getJsonArray("privileges").stream()
                .map(o -> Privilege.valueOf((String) o)).collect(Collectors.toSet());
        NavigableSet<String> namespacePrefixes = json.getJsonArray("namespacePrefixes").stream()
                .map(o -> (String) o).collect(Collectors.toCollection(TreeSet::new));
        Valuation maxValuation = Valuation.valueOf(json.getString("maxValuation"));
        Set<DatasetState> states = json.getJsonArray("states").stream()
                .map(o -> DatasetState.valueOf((String) o)).collect(Collectors.toSet());
        return new Role(roleId, privileges, namespacePrefixes, maxValuation, states);
    }

    public static JsonObject toVertxJsonObject(Role role) {
        JsonArray privilegesArray = new JsonArray();
        role.getPrivileges().stream().forEach(privilege -> privilegesArray.add(privilege));
        return new JsonObject()
                .put("roleId", role.getRoleId())
                .put("privileges", privilegesArray)
                .put("namespacePrefixes", role.getNamespacePrefixes().stream().collect(() -> new JsonArray(), (ja, nsPrefix) -> ja.add(nsPrefix), (ja1, ja2) -> ja1.addAll(ja2)))
                .put("maxValuation", role.getMaxValuation())
                .put("states", role.getStates().stream().collect(() -> new JsonArray(), (ja, state) -> ja.add(state), (ja1, ja2) -> ja1.addAll(ja2)));
    }

    public String getRoleId() {
        return roleId;
    }

    public void setRoleId(String roleId) {
        this.roleId = roleId;
    }

    public Set<Privilege> getPrivileges() {
        return privileges;
    }

    public void setPrivileges(Set<Privilege> privileges) {
        this.privileges = privileges;
    }

    public NavigableSet<String> getNamespacePrefixes() {
        return namespacePrefixes;
    }

    public void setNamespacePrefixes(NavigableSet<String> namespacePrefixes) {
        this.namespacePrefixes = namespacePrefixes;
    }

    public Valuation getMaxValuation() {
        return maxValuation;
    }

    public void setMaxValuation(Valuation maxValuation) {
        this.maxValuation = maxValuation;
    }

    public Set<DatasetState> getStates() {
        return states;
    }

    public void setStates(Set<DatasetState> states) {
        this.states = states;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Role role = (Role) o;
        return roleId.equals(role.roleId) &&
                privileges.equals(role.privileges) &&
                namespacePrefixes.equals(role.namespacePrefixes) &&
                maxValuation == role.maxValuation &&
                states.equals(role.states);
    }

    @Override
    public int hashCode() {
        return Objects.hash(roleId, privileges, namespacePrefixes, maxValuation, states);
    }

    @Override
    public String toString() {
        return "Role{" +
                "roleId='" + roleId + '\'' +
                ", privileges=" + privileges +
                ", namespacePrefixes=" + namespacePrefixes +
                ", maxValuation=" + maxValuation +
                ", states=" + states +
                '}';
    }
}
