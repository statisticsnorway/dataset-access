package no.ssb.datasetaccess.user;

import no.ssb.datasetaccess.role.Role;

import java.util.Objects;
import java.util.Set;

public class User {
    String userId;
    Set<Role> roles;

    public User() {
    }

    public User(String userId, Set<Role> roles) {
        this.userId = userId;
        this.roles = roles;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles;
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
