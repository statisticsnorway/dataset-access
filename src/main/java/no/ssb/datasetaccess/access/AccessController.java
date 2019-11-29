package no.ssb.datasetaccess.access;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpResponseFactory;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.QueryValue;
import io.reactivex.Maybe;
import no.ssb.datasetaccess.dataset.DatasetState;
import no.ssb.datasetaccess.dataset.Valuation;
import no.ssb.datasetaccess.role.Privilege;
import no.ssb.datasetaccess.role.Role;
import no.ssb.datasetaccess.user.UserRepository;

import javax.inject.Inject;

@Controller("/access")
public class AccessController {

    @Inject
    UserRepository userRepository;

    @Get("/{userId}")
    public Maybe<HttpResponse<Boolean>> getRole(@PathVariable String userId, @QueryValue Privilege privilege, @QueryValue String namespace, @QueryValue Valuation valuation, @QueryValue DatasetState state) {
        return userRepository.getUser(userId).map(user -> {
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
                return HttpResponse.ok(true);
            }
            return HttpResponseFactory.INSTANCE.status(HttpStatus.FORBIDDEN);
        });
    }
}
