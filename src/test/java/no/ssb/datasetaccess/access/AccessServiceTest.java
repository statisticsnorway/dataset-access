package no.ssb.datasetaccess.access;

import no.ssb.dapla.auth.dataset.protobuf.DatasetState;
import no.ssb.dapla.auth.dataset.protobuf.DatasetStateSet;
import no.ssb.dapla.auth.dataset.protobuf.PathSet;
import no.ssb.dapla.auth.dataset.protobuf.Privilege;
import no.ssb.dapla.auth.dataset.protobuf.Role;
import no.ssb.dapla.auth.dataset.protobuf.Valuation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccessServiceTest {

    @Test
    public void thatMatchRoleWorks() {
        Role role = Role.newBuilder()
                .setPaths(PathSet.newBuilder()
                        .addIncludes("/user/someone/")
                        .addExcludes("/user/someone/private/")
                        .build())
                .setMaxValuation(Valuation.SHIELDED)
                .setStates(DatasetStateSet.newBuilder()
                        .addExcludes(DatasetState.RAW)
                        .build())
                .build();

        // Path
        assertFalse(AccessService.matchRole(role, Privilege.READ, "/user/someoneelse/test1", Valuation.SHIELDED, DatasetState.INPUT));
        assertFalse(AccessService.matchRole(role, Privilege.READ, "/user/someone-else/test1", Valuation.SHIELDED, DatasetState.INPUT));
        assertFalse(AccessService.matchRole(role, Privilege.READ, "/user/anyone-else/test1", Valuation.SHIELDED, DatasetState.INPUT));
        assertTrue(AccessService.matchRole(role, Privilege.READ, "/user/someone/test1", Valuation.SHIELDED, DatasetState.INPUT));
        assertFalse(AccessService.matchRole(role, Privilege.READ, "/user/someone/private/something", Valuation.SHIELDED, DatasetState.INPUT));
        assertTrue(AccessService.matchRole(role, Privilege.READ, "/user/someone/privatethis", Valuation.SHIELDED, DatasetState.INPUT));

        // State exclude set
        assertFalse(AccessService.matchRole(role, Privilege.READ, "/user/someone/test1", Valuation.SHIELDED, DatasetState.RAW));
        assertTrue(AccessService.matchRole(role, Privilege.READ, "/user/someone/test1", Valuation.SHIELDED, DatasetState.INPUT));

        // Valuation limit
        assertTrue(AccessService.matchRole(role, Privilege.READ, "/user/someone/test1", Valuation.OPEN, DatasetState.INPUT));
        assertTrue(AccessService.matchRole(role, Privilege.READ, "/user/someone/test1", Valuation.INTERNAL, DatasetState.INPUT));
        assertTrue(AccessService.matchRole(role, Privilege.READ, "/user/someone/test1", Valuation.SHIELDED, DatasetState.INPUT));
        assertFalse(AccessService.matchRole(role, Privilege.READ, "/user/someone/test1", Valuation.SENSITIVE, DatasetState.INPUT));

        // Privileges
        assertTrue(AccessService.matchRole(role, Privilege.CREATE, "/user/someone/test1", Valuation.SHIELDED, DatasetState.INPUT));
        assertTrue(AccessService.matchRole(role, Privilege.READ, "/user/someone/test1", Valuation.SHIELDED, DatasetState.INPUT));
        assertTrue(AccessService.matchRole(role, Privilege.UPDATE, "/user/someone/test1", Valuation.SHIELDED, DatasetState.INPUT));
        assertTrue(AccessService.matchRole(role, Privilege.DELETE, "/user/someone/test1", Valuation.SHIELDED, DatasetState.INPUT));
    }
}