package no.ssb.datasetaccess.user;

import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.stub.StreamObserver;
import io.helidon.common.http.Http;
import io.helidon.webserver.Handler;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;
import no.ssb.dapla.auth.dataset.protobuf.User;
import no.ssb.dapla.auth.dataset.protobuf.UserDeleteRequest;
import no.ssb.dapla.auth.dataset.protobuf.UserDeleteResponse;
import no.ssb.dapla.auth.dataset.protobuf.UserGetRequest;
import no.ssb.dapla.auth.dataset.protobuf.UserGetResponse;
import no.ssb.dapla.auth.dataset.protobuf.UserPutRequest;
import no.ssb.dapla.auth.dataset.protobuf.UserPutResponse;
import no.ssb.dapla.auth.dataset.protobuf.UserServiceGrpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class UserService extends UserServiceGrpc.UserServiceImplBase implements Service {

    private static final Logger LOG = LoggerFactory.getLogger(UserService.class);

    final UserRepository repository;

    public UserService(UserRepository repository) {
        this.repository = repository;
    }

    @Override
    public void update(Routing.Rules rules) {
        rules.get("/{userId}", this::doGet);
        rules.put("/{userId}", Handler.create(User.class, this::doPut));
        rules.delete("/{userId}", this::doDelete);
    }

    private void doGet(ServerRequest req, ServerResponse res) {
        String userId = req.path().param("userId");
        repository.getUser(userId)
                .thenAccept(user -> {
                    if (user == null) {
                        res.status(Http.Status.NOT_FOUND_404).send();
                    } else {
                        res.send(user);
                    }
                });
    }

    private void doPut(ServerRequest req, ServerResponse res, User user) {
        String userId = req.path().param("userId");
        if (!userId.equals(user.getUserId())) {
            res.status(Http.Status.BAD_REQUEST_400).send("userId in path must match that in body");
        }
        repository.createOrUpdateUser(user)
                .thenRun(() -> {
                    res.headers().add("Location", "/user/" + userId);
                    res.status(Http.Status.CREATED_201).send();
                })
                .exceptionally(t -> {
                    res.status(Http.Status.INTERNAL_SERVER_ERROR_500).send(t.getMessage());
                    return null;
                });
    }

    private void doDelete(ServerRequest req, ServerResponse res) {
        String userId = req.path().param("userId");
        repository.deleteUser(userId)
                .thenRun(res::send)
                .exceptionally(t -> {
                    res.status(Http.Status.INTERNAL_SERVER_ERROR_500).send(t.getMessage());
                    return null;
                });
    }

    @Override
    public void getUser(UserGetRequest request, StreamObserver<UserGetResponse> responseObserver) {
        repository.getUser(request.getUserId())
                .orTimeout(10, TimeUnit.SECONDS)
                .thenAccept(user -> {
                    responseObserver.onNext(UserGetResponse.newBuilder().setUser(user).build());
                    responseObserver.onCompleted();
                })
                .exceptionally(throwable -> {
                    LOG.error(String.format("While serving grpc get for user: %s", request.getUserId()), throwable);
                    responseObserver.onError(new StatusException(Status.fromThrowable(throwable)));
                    return null;
                });
    }

    @Override
    public void putUser(UserPutRequest request, StreamObserver<UserPutResponse> responseObserver) {
        repository.createOrUpdateUser(request.getUser())
                .orTimeout(5, TimeUnit.SECONDS)
                .thenAccept(aVoid -> {
                    responseObserver.onNext(UserPutResponse.newBuilder().build());
                    responseObserver.onCompleted();
                })
                .exceptionally(throwable -> {
                    LOG.error(String.format("While serving grpc save for user: %s", request.getUser().getUserId()), throwable);
                    responseObserver.onError(throwable);
                    return null;
                });
    }

    @Override
    public void deleteUser(UserDeleteRequest request, StreamObserver<UserDeleteResponse> responseObserver) {
        repository.deleteUser(request.getUserId())
                .orTimeout(5, TimeUnit.SECONDS)
                .thenAccept(aVoid -> {
                    responseObserver.onNext(UserDeleteResponse.newBuilder().build());
                    responseObserver.onCompleted();
                })
                .exceptionally(throwable -> {
                    LOG.error(String.format("While serving grpc delete for user: %s", request.getUserId()), throwable);
                    responseObserver.onError(throwable);
                    return null;
                });
    }
}
