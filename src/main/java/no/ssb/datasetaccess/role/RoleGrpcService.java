package no.ssb.datasetaccess.role;

import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.stub.StreamObserver;
import io.opentracing.Span;
import no.ssb.dapla.auth.dataset.protobuf.RoleDeleteRequest;
import no.ssb.dapla.auth.dataset.protobuf.RoleDeleteResponse;
import no.ssb.dapla.auth.dataset.protobuf.RoleGetRequest;
import no.ssb.dapla.auth.dataset.protobuf.RoleGetResponse;
import no.ssb.dapla.auth.dataset.protobuf.RolePutRequest;
import no.ssb.dapla.auth.dataset.protobuf.RolePutResponse;
import no.ssb.dapla.auth.dataset.protobuf.RoleServiceGrpc;
import no.ssb.helidon.application.Tracing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

import static no.ssb.helidon.application.Tracing.spanFromGrpc;

public class RoleGrpcService extends RoleServiceGrpc.RoleServiceImplBase {

    private static final Logger LOG = LoggerFactory.getLogger(RoleGrpcService.class);

    final RoleRepository repository;

    public RoleGrpcService(RoleRepository repository) {
        this.repository = repository;
    }

    @Override
    public void getRole(RoleGetRequest request, StreamObserver<RoleGetResponse> responseObserver) {
        Span span = spanFromGrpc(request, "getRole");
        try {
            repository.getRole(request.getRoleId())
                    .orTimeout(10, TimeUnit.SECONDS)
                    .thenAccept(role -> {
                        RoleGetResponse.Builder responseBuilder = RoleGetResponse.newBuilder();
                        if (role != null) {
                            responseBuilder.setRole(role);
                        }
                        responseObserver.onNext(responseBuilder.build());
                        responseObserver.onCompleted();
                    }).thenRun(span::finish)
                    .exceptionally(throwable -> {
                        try {
                            LOG.error(String.format("While serving grpc get for role: %s", request.getRoleId()), throwable);
                            Tracing.logError(span, throwable);
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
    public void putRole(RolePutRequest request, StreamObserver<RolePutResponse> responseObserver) {
        Span span = spanFromGrpc(request, "putRole");
        try {
            repository.createOrUpdateRole(request.getRole())
                    .orTimeout(5, TimeUnit.SECONDS)
                    .thenAccept(aVoid -> {
                        responseObserver.onNext(RolePutResponse.newBuilder().build());
                        responseObserver.onCompleted();
                    }).thenRun(span::finish)
                    .exceptionally(throwable -> {
                        try {
                            LOG.error(String.format("While serving grpc save for role: %s", request.getRole().getRoleId()), throwable);
                            Tracing.logError(span, throwable);
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
    public void deleteRole(RoleDeleteRequest request, StreamObserver<RoleDeleteResponse> responseObserver) {
        Span span = spanFromGrpc(request, "deleteRole");
        try {
            repository.deleteRole(request.getRoleId())
                    .orTimeout(5, TimeUnit.SECONDS)
                    .thenAccept(aVoid -> {
                        responseObserver.onNext(RoleDeleteResponse.newBuilder().build());
                        responseObserver.onCompleted();
                    }).thenRun(span::finish)
                    .exceptionally(throwable -> {
                        try {
                            LOG.error(String.format("While serving grpc delete for role: %s", request.getRoleId()), throwable);
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
