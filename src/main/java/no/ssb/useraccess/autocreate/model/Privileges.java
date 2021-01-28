package no.ssb.useraccess.autocreate.model;

import io.helidon.config.objectmapping.Value;
import no.ssb.dapla.auth.dataset.protobuf.Privilege;
import no.ssb.dapla.auth.dataset.protobuf.PrivilegeSet;

import java.util.List;
import java.util.stream.Collectors;

public class Privileges {
    private final List<String> includes;
    private final List<String> excludes;

    public Privileges(@Value(key = "includes") List<String> includes, @Value(key = "excludes") List<String> excludes) {
        this.includes = includes;
        this.excludes = excludes;
    }

    private Iterable<Privilege> toPrivileges(List<String> privileges) {
        return privileges.stream().map(Privilege::valueOf).collect(Collectors.toList());
    }

    public PrivilegeSet toPrivilegeSet() {
        PrivilegeSet.Builder privileges = PrivilegeSet.newBuilder();
        if (!includes.isEmpty()) {
            privileges.addAllIncludes(toPrivileges(includes));
        }
        if (!excludes.isEmpty()) {
            privileges.addAllExcludes(toPrivileges(excludes));
        }
        return privileges.build();
    }

    public List<String> getIncludes() {
        return includes;
    }

    public List<String> getExcludes() {
        return excludes;
    }
}
