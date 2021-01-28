package no.ssb.useraccess.autocreate.model;

import io.helidon.config.objectmapping.Value;
import no.ssb.dapla.auth.dataset.protobuf.Valuation;

public class Role {
    private final RoleId roleId;
    private final Description description;
    private final MaxValuation maxValuation;
    private final Privileges privileges;
    private final States states;
    private final Paths paths;

    public Role(@Value(key = "roleId") RoleId roleId,
                @Value(key = "description") Description description,
                @Value(key = "privileges") Privileges privileges,
                @Value(key = "paths") Paths paths,
                @Value(key = "maxValuation") MaxValuation maxValuation,
                @Value(key = "states") States states) {
        this.roleId = roleId;
        this.description = description;
        this.privileges = privileges;
        this.paths = paths;
        this.maxValuation = maxValuation;
        this.states = states;
    }

    public no.ssb.dapla.auth.dataset.protobuf.Role toRole(String userId) {
        return no.ssb.dapla.auth.dataset.protobuf.Role.newBuilder()
                .setRoleId(roleId.getRoleId(userId))
                .setDescription(description.getDescription(userId))
                .setPrivileges(privileges.toPrivilegeSet())
                .setPaths(paths.toPathSet(userId))
                .setMaxValuation(Valuation.valueOf(maxValuation.getMaxValuation()))
                .setStates(states.toSet())
                .build();
    }

    public RoleId getRoleId() {
        return roleId;
    }

    public Description getDescription() {
        return description;
    }

    public Privileges getPrivileges() {
        return privileges;
    }

    public Paths getPaths() {
        return paths;
    }

    public MaxValuation getMaxValuation() {
        return maxValuation;
    }

    public States getStates() {
        return states;
    }
}
