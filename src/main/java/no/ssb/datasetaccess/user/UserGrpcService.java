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
import no.ssb.helidon.application.TracerAndSpan;
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
        TracerAndSpan tracerAndSpan = spanFromGrpc(request, "getUser");
        Span span = tracerAndSpan.span();
        try {
            repository.getUser(request.getUserId())
                    .orTimeout(10, TimeUnit.SECONDS)
                    .thenAccept(user -> {
                        Tracing.restoreTracingContext(tracerAndSpan);
                        UserGetResponse.Builder responseBuilder = UserGetResponse.newBuilder();
                        if (user != null) {
                            responseBuilder.setUser(user);
                        }
                        responseObserver.onNext(responseBuilder.build());
                        responseObserver.onCompleted();
                    }).thenRun(span::finish)
                    .exceptionally(throwable -> {
                        try {
                            Tracing.restoreTracingContext(tracerAndSpan);
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
                LOG.error("unexpected error", e);
                responseObserver.onError(new StatusException(Status.fromThrowable(e)));
            } finally {
                span.finish();
            }
        }
    }

    @Override
    public void putUser(UserPutRequest request, StreamObserver<UserPutResponse> responseObserver) {
        TracerAndSpan tracerAndSpan = spanFromGrpc(request, "putUser");
        Span span = tracerAndSpan.span();
        try {
            repository.createOrUpdateUser(request.getUser())
                    .orTimeout(5, TimeUnit.SECONDS)
                    .thenAccept(aVoid -> {
                        Tracing.restoreTracingContext(tracerAndSpan);
                        responseObserver.onNext(UserPutResponse.newBuilder().build());
                        responseObserver.onCompleted();
                    }).thenRun(span::finish)
                    .exceptionally(throwable -> {
                        try {
                            Tracing.restoreTracingContext(tracerAndSpan);
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
                LOG.error("unexpected error", e);
                responseObserver.onError(new StatusException(Status.fromThrowable(e)));
            } finally {
                span.finish();
            }
        }
    }

    @Override
    public void deleteUser(UserDeleteRequest request, StreamObserver<UserDeleteResponse> responseObserver) {
        TracerAndSpan tracerAndSpan = spanFromGrpc(request, "putUser");
        Span span = tracerAndSpan.span();
        try {
            repository.deleteUser(request.getUserId())
                    .orTimeout(5, TimeUnit.SECONDS)
                    .thenAccept(aVoid -> {
                        Tracing.restoreTracingContext(tracerAndSpan);
                        responseObserver.onNext(UserDeleteResponse.newBuilder().build());
                        responseObserver.onCompleted();
                    }).thenRun(span::finish)
                    .exceptionally(throwable -> {
                        try {
                            Tracing.restoreTracingContext(tracerAndSpan);
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
                LOG.error("unexpected error", e);
                responseObserver.onError(new StatusException(Status.fromThrowable(e)));
            } finally {
                span.finish();
            }
        }
    }
}
