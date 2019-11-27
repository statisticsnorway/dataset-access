package no.ssb.datasetaccess.user;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Delete;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.reactivex.Single;
import no.ssb.datasetaccess.role.Privilege;
import no.ssb.datasetaccess.role.Role;

import java.net.URI;
import java.util.Set;
import java.util.TreeSet;

@Controller("/user")
public class UserController {

    @Post("/{userId}")
    public Single<HttpResponse<String>> createUser(@PathVariable String userId, @Body User user) {
        return Single.just(HttpResponse.created(URI.create("/user/" + userId)));
    }

    @Get("/{userId}")
    public Single<HttpResponse<User>> getUser(@PathVariable String userId) {
        return Single.just(HttpResponse.ok(new User("john", Set.of(new Role("reader", Set.of(Privilege.READ))), new TreeSet<>(Set.of("/a/b/c")))));
    }

    @Delete("/{userId}")
    public Single<HttpResponse<User>> deleteUser(@PathVariable String userId) {
        return Single.just(HttpResponse.ok());
    }
}
