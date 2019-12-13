package no.ssb.datasetaccess.user;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class User {
    String userId;
    Set<String> roles;

    public User() {
    }

    public User(String userId, Set<String> roles) {
        this.userId = userId;
        this.roles = roles;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    public static User fromJson(JsonObject json) {
        String userId = json.getString("userId");
        Set<String> roles = json.getJsonArray("roles").stream()
                .map(o -> (String) o).collect(Collectors.toSet());
        return new User(userId, roles);
    }

    public static JsonObject toJsonObject(User user) {
        JsonArray rolesArray = new JsonArray();
        user.getRoles().stream().forEach(role -> rolesArray.add(role));
        return new JsonObject()
                .put("userId", user.getUserId())
                .put("roles", rolesArray);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return userId.equals(user.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }

    @Override
    public String toString() {
        return "User{" +
                "userId='" + userId + '\'' +
                ", roles=" + roles +
                '}';
    }
}
