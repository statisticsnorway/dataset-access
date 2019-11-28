package no.ssb.datasetaccess.role;

import io.vertx.core.json.JsonObject;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class Role {
    String name;
    Set<Privilege> privileges;

    public Role() {
        name = "";
        privileges = Collections.emptySet();
    }

    public Role(String name, Set<Privilege> privileges) {
        this.name = name;
        this.privileges = privileges;
    }

    public static Role fromJson(JsonObject json) {
        String name = json.getString("name");
        Set<Privilege> privileges = json.getJsonArray("privileges").stream()
                .map(o -> Privilege.valueOf((String) o)).collect(Collectors.toSet());
        return new Role(name, privileges);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<Privilege> getPrivileges() {
        return privileges;
    }

    public void setPrivileges(Set<Privilege> privileges) {
        this.privileges = privileges;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Role role = (Role) o;
        return name.equals(role.name) &&
                privileges.equals(role.privileges);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, privileges);
    }

    @Override
    public String toString() {
        return "Role{" +
                "name='" + name + '\'' +
                ", privileges=" + privileges +
                '}';
    }
}