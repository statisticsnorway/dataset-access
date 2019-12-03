package no.ssb.datasetaccess.access;

import no.ssb.datasetaccess.dataset.DatasetState;
import no.ssb.datasetaccess.dataset.Valuation;
import no.ssb.datasetaccess.role.Privilege;
import no.ssb.datasetaccess.role.Role;
import no.ssb.datasetaccess.user.UserRepository;

// /access
public class AccessController {

    UserRepository userRepository;

    //@Get("/{userId}")
    //public Maybe<HttpResponse<Boolean>> getRole(@PathVariable String userId, @QueryValue Privilege privilege, @QueryValue String namespace, @QueryValue Valuation valuation, @QueryValue DatasetState state) {
    public void getRole(String userId, Privilege privilege, String namespace, Valuation valuation, DatasetState state) {
        userRepository.getUser(userId).map(user -> {
            for (Role role : user.getRoles()) {
                if (!role.getPrivileges().contains(privilege)) {
                    continue;
                }
                String floor = role.getNamespacePrefixes().floor(namespace);
                if (!namespace.startsWith(floor)) {
                    continue;
                }
                if (!role.getMaxValuation().grantsAccessTo(valuation)) {
                    continue;
                }
                if (!role.getStates().contains(state)) {
                    continue;
                }
                //return HttpResponse.ok(true);
            }
            //return HttpResponseFactory.INSTANCE.status(HttpStatus.FORBIDDEN);
            return null;
        });
    }
}
