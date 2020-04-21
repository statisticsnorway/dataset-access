package no.ssb.datasetaccess.maintenance;

import no.ssb.datasetaccess.group.GroupRepository;
import no.ssb.datasetaccess.role.RoleRepository;
import no.ssb.datasetaccess.user.UserRepository;

import java.util.concurrent.CompletableFuture;

public class MaintenanceRepository {

    private final RoleRepository roleRepository;
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;

    public MaintenanceRepository(RoleRepository roleRepository, GroupRepository groupRepository, UserRepository userRepository) {
        this.roleRepository = roleRepository;
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
    }

    public CompletableFuture<Void> deleteAll() {
        return roleRepository.deleteAllRoles()
                .thenCombine(groupRepository.deleteAllGroups(), (a, b) -> null)
                .thenCombine(userRepository.deleteAllUsers(), (a, b) -> null);
    }
}
