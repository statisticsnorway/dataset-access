package no.ssb.datasetaccess.access;

import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.stub.StreamObserver;
import io.opentracing.Span;
import no.ssb.dapla.auth.dataset.protobuf.AccessCheckRequest;
import no.ssb.dapla.auth.dataset.protobuf.AccessCheckResponse;
import no.ssb.dapla.auth.dataset.protobuf.AuthServiceGrpc;
import no.ssb.dapla.auth.dataset.protobuf.DatasetState;
import no.ssb.dapla.auth.dataset.protobuf.Privilege;
import no.ssb.dapla.auth.dataset.protobuf.Valuation;
import no.ssb.helidon.application.TracerAndSpan;
import no.ssb.helidon.application.Tracing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

import static no.ssb.helidon.application.Tracing.logError;
import static no.ssb.helidon.application.Tracing.traceOutputMessage;

public class AccessGrpcService extends AuthServiceGrpc.AuthServiceImplBase {

    private static final Logger LOG = LoggerFactory.getLogger(AccessGrpcService.class);

    private final AccessService accessService;

    public AccessGrpcService(AccessService accessService) {
        this.accessService = accessService;
    }

    @Override
    public void hasAccess(AccessCheckRequest request, StreamObserver<AccessCheckResponse> responseObserver) {
        TracerAndSpan tracerAndSpan = Tracing.spanFromGrpc(request, "hasAccess");
        Span span = tracerAndSpan.span();
        try {
            String userId = request.getUserId();
            Privilege privilege = Privilege.valueOf(request.getPrivilege());
            String path = request.getNamespace();
            Valuation valuation = Valuation.valueOf(request.getValuation());
            DatasetState state = DatasetState.valueOf(request.getState());
            accessService.hasAccess(span, userId, privilege, path, valuation, state)
                    .orTimeout(10, TimeUnit.SECONDS)
                    .thenAccept(access -> {
                        Tracing.restoreTracingContext(tracerAndSpan);
                        responseObserver.onNext(traceOutputMessage(span, AccessCheckResponse.newBuilder().setAllowed(access).build()));
                        responseObserver.onCompleted();
                        span.finish();
                    })
                    .exceptionally(throwable -> {
                        try {
                            Tracing.restoreTracingContext(tracerAndSpan);
                            logError(span, throwable, "error in hasAccess()");
                            LOG.error(String.format("hasAccess()"), throwable);
                            responseObserver.onError(new StatusException(Status.fromThrowable(throwable)));
                            return null;
                        } finally {
                            span.finish();
                        }
                    });
        } catch (RuntimeException | Error e) {
            try {
                logError(span, e, "top-level error");
                LOG.error("top-level error", e);
                responseObserver.onError(new StatusException(Status.fromThrowable(e)));
            } finally {
                span.finish();
            }
        }
    }
}
