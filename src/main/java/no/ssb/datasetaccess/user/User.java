package no.ssb.datasetaccess.user;

import no.ssb.datasetaccess.role.Role;

import java.util.NavigableSet;
import java.util.Objects;
import java.util.Set;

public class User {
    String userId;
    Set<Role> roles;
    NavigableSet<String> namespacePrefixes;

    public User() {
    }

    public User(String userId, Set<Role> roles, NavigableSet<String> namespacePrefixes) {
        this.userId = userId;
        this.roles = roles;
        this.namespacePrefixes = namespacePrefixes;
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

    public NavigableSet<String> getNamespacePrefixes() {
        return namespacePrefixes;
    }

    public void setNamespacePrefixes(NavigableSet<String> namespacePrefixes) {
        this.namespacePrefixes = namespacePrefixes;
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
                ", namespacePrefixes=" + namespacePrefixes +
                '}';
    }
}
