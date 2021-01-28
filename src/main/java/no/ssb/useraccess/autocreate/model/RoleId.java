package no.ssb.useraccess.autocreate.model;

import io.helidon.config.objectmapping.Value;
import no.ssb.useraccess.util.StringUtil;

public class RoleId {
    private final String roleId;
    public RoleId (@Value(key="roleId") String roleId){
        this.roleId =roleId;
    }

    public String getRoleId(String userId) {
        return StringUtil.substitudeVariable(roleId, userId);
    }
}
