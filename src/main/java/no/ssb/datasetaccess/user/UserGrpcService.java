package no.ssb.datasetaccess.user;

import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.stub.StreamObserver;
import io.opentracing.Span;
import no.ssb.dapla.auth.dataset.protobuf.UserDeleteRequest;
import no.ssb.dapla.auth.dataset.protobuf.UserDeleteResponse;
import no.ssb.dapla.auth.dataset.protobuf.UserGetRequest;
import no.ssb.dapla.auth.dataset.protobuf.UserGetResponse;
import no.ssb.dapla.auth.dataset.protobuf.UserPutRequest;
import no.ssb.dapla.auth.dataset.protobuf.UserPutResponse;
import no.ssb.dapla.auth.dataset.protobuf.UserServiceGrpc;
import no.ssb.helidon.application.Tracing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

import static no.ssb.helidon.application.Tracing.spanFromGrpc;

public class UserGrpcService extends UserServiceGrpc.UserServiceImplBase {

    private static final Logger LOG = LoggerFactory.getLogger(UserGrpcService.class);

    final UserRepository repository;

    public UserGrpcService(UserRepository repository) {
        this.repository = repository;
    }

    @Override
    public void getUser(UserGetRequest request, StreamObserver<UserGetResponse> responseObserver) {
        Span span = spanFromGrpc(request, "getUser");
        try {
            repository.getUser(request.getUserId())
                    .orTimeout(10, TimeUnit.SECONDS)
                    .thenAccept(user -> {
                        responseObserver.onNext(UserGetResponse.newBuilder().setUser(user).build());
                        responseObserver.onCompleted();
                    }).thenRun(span::finish)
                    .exceptionally(throwable -> {
                        try {
                            LOG.error(String.format("While serving grpc get for user: %s", request.getUserId()), throwable);
                            responseObserver.onError(new StatusException(Status.fromThrowable(throwable)));
                            return null;
                        } finally {
                            span.finish();
                        }
                    });
        } catch (Exception | Error e) {
            try {
                Tracing.logError(span, e, "unexpected error");
            } finally {
                span.finish();
            }
        }
    }

    @Override
    public void putUser(UserPutRequest request, StreamObserver<UserPutResponse> responseObserver) {
        Span span = spanFromGrpc(request, "putUser");
        try {
            repository.createOrUpdateUser(request.getUser())
                    .orTimeout(5, TimeUnit.SECONDS)
                    .thenAccept(aVoid -> {
                        responseObserver.onNext(UserPutResponse.newBuilder().build());
                        responseObserver.onCompleted();
                    }).thenRun(span::finish)
                    .exceptionally(throwable -> {
                        try {
                            LOG.error(String.format("While serving grpc save for user: %s", request.getUser().getUserId()), throwable);
                            responseObserver.onError(throwable);
                            return null;
                        } finally {
                            span.finish();
                        }
                    });
        } catch (Exception | Error e) {
            try {
                Tracing.logError(span, e, "unexpected error");
            } finally {
                span.finish();
            }
        }
    }

    @Override
    public void deleteUser(UserDeleteRequest request, StreamObserver<UserDeleteResponse> responseObserver) {
        Span span = spanFromGrpc(request, "putUser");
        try {
            repository.deleteUser(request.getUserId())
                    .orTimeout(5, TimeUnit.SECONDS)
                    .thenAccept(aVoid -> {
                        responseObserver.onNext(UserDeleteResponse.newBuilder().build());
                        responseObserver.onCompleted();
                    }).thenRun(span::finish)
                    .exceptionally(throwable -> {
                        try {
                            LOG.error(String.format("While serving grpc delete for user: %s", request.getUserId()), throwable);
                            responseObserver.onError(throwable);
                            return null;
                        } finally {
                            span.finish();
                        }
                    });
        } catch (Exception | Error e) {
            try {
                Tracing.logError(span, e, "unexpected error");
            } finally {
                span.finish();
            }
        }
    }
}
