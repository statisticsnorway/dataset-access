package no.ssb.datasetaccess.role;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Delete;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.reactivex.Single;

import javax.inject.Inject;
import java.net.URI;

@Controller("/role")
public class RoleController {

    @Inject
    RoleRepository repository;

    @Post("/{roleId}")
    public Single<HttpResponse<String>> createRole(@PathVariable String roleId, @Body Role role) {
        return repository.createRole(role).toSingleDefault(HttpResponse.created(URI.create("/role/" + roleId)));
    }

    @Get("/{roleId}")
    public Single<HttpResponse<Role>> getRole(@PathVariable String roleId) {
        return repository.getRole(roleId).map(role -> HttpResponse.ok(role));
    }

    @Delete("/{roleId}")
    public Single<HttpResponse<String>> deleteRole(@PathVariable String roleId) {
        return Single.just(HttpResponse.ok());
    }
}
