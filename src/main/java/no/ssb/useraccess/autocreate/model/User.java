package no.ssb.useraccess.autocreate.model;

import io.helidon.config.objectmapping.Value;
import no.ssb.useraccess.util.StringUtil;

import java.util.List;
import java.util.stream.Collectors;

public class User {
    private final UserId userId;
    private final List<String> roles;
    private final List<String> groups;

    public User(@Value(key= "userId") UserId userId, @Value(key= "roles") List<String> roles, @Value(key ="groups") List<String> groups) {
        this.userId = userId;
        this.roles = roles;
        this.groups = groups;
    }

    public no.ssb.dapla.auth.dataset.protobuf.User toProtoUser(String userId) {
        String processedUserId;

        processedUserId = StringUtil.substitudeVariable(this.userId.getUserId(), StringUtil.getUserIdPart(userId).get());
        List<String> processedRoles = roles.stream().map(role -> StringUtil.substitudeVariable(role, StringUtil.getUserIdPart(userId).get())).collect(Collectors.toList());

        return no.ssb.dapla.auth.dataset.protobuf.User.newBuilder()
                .setUserId(processedUserId)
                .addAllRoles(processedRoles)
                .addAllGroups(groups)
                .build();
    }

    public UserId getUserId () {
        return this.userId;
    }

    public List<String> getRoles() {
        return roles;
    }

    public List<String> getGroups() {
        return groups;
    }
}
