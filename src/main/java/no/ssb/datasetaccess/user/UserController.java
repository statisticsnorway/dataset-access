package no.ssb.datasetaccess.user;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Delete;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Put;
import io.reactivex.Single;

import javax.inject.Inject;
import java.net.URI;

@Controller("/user")
public class UserController {

    @Inject
    UserRepository repository;

    @Put("/{userId}")
    public Single<HttpResponse<String>> createUser(@PathVariable String userId, @Body User user) {
        return repository.createUser(user).toSingleDefault(HttpResponse.created(URI.create("/user/" + userId)));
    }

    @Get("/{userId}")
    public Single<HttpResponse<User>> getUser(@PathVariable String userId) {
        return repository.getUser(userId).map(HttpResponse::ok);
    }

    @Delete("/{userId}")
    public Single<HttpResponse<User>> deleteUser(@PathVariable String userId) {
        return repository.deleteUser(userId).toSingleDefault(HttpResponse.ok());
    }
}
