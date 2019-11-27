package no.ssb.datasetaccess.role;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Delete;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.reactivex.Single;

import java.net.URI;
import java.util.Set;

@Controller("/role")
public class RoleController {

    @Post("/{roleId}")
    public Single<HttpResponse<String>> createRole(@PathVariable String roleId, @Body Role role) {
        return Single.just(HttpResponse.created(URI.create("/role/" + roleId)));
    }

    @Get("/{roleId}")
    public Single<HttpResponse<Role>> getRole(@PathVariable String roleId) {
        return Single.just(HttpResponse.ok(new Role(roleId, Set.of(Privilege.READ))));
    }

    @Delete("/{roleId}")
    public Single<HttpResponse<String>> deleteRole(@PathVariable String roleId) {
        return Single.just(HttpResponse.ok());
    }
}
