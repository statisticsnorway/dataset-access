package no.ssb.datasetaccess.maintenance;

import io.helidon.common.reactive.Multi;
import no.ssb.datasetaccess.group.GroupRepository;
import no.ssb.datasetaccess.role.RoleRepository;
import no.ssb.datasetaccess.user.UserRepository;

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
