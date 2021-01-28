package no.ssb.useraccess.maintenance;

import io.helidon.common.reactive.Multi;
import no.ssb.useraccess.group.GroupRepository;
import no.ssb.useraccess.role.RoleRepository;
import no.ssb.useraccess.user.UserRepository;

public class MaintenanceRepository {

    private final RoleRepository roleRepository;
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;

    public MaintenanceRepository(RoleRepository roleRepository, GroupRepository groupRepository, UserRepository userRepository) {
        this.roleRepository = roleRepository;
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
    }

    public Multi<Long> deleteAll() {
        return Multi.concat(roleRepository.deleteAllRoles(), groupRepository.deleteAllGroups(), userRepository.deleteAllUsers());
    }
}
