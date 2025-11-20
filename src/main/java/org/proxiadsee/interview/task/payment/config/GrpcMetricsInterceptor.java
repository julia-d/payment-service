package org.proxiadsee.interview.task.payment.config;

import io.grpc.ForwardingServerCall;
import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(0)
public class GrpcMetricsInterceptor implements ServerInterceptor {

  private final MeterRegistry meterRegistry;

  public GrpcMetricsInterceptor(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
    log.info("GrpcMetricsInterceptor initialized with MeterRegistry: {}", meterRegistry.getClass().getSimpleName());
  }

  @Override
  public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
      ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {

    String serviceName = call.getMethodDescriptor().getServiceName();
    String methodName = call.getMethodDescriptor().getBareMethodName();
    String fullMethodName = call.getMethodDescriptor().getFullMethodName();

    log.debug("Intercepting gRPC call: service={}, method={}", serviceName, methodName);

    Tags baseTags =
        Tags.of("grpc.service", serviceName != null ? serviceName : "unknown")
            .and("grpc.method", methodName != null ? methodName : "unknown")
            .and("grpc.full_method", fullMethodName != null ? fullMethodName : "unknown");

    meterRegistry.counter("grpc.server.calls.started", baseTags).increment();

    long startTime = System.nanoTime();

    ServerCall<ReqT, RespT> monitoringCall =
        new ForwardingServerCall.SimpleForwardingServerCall<>(call) {
          @Override
          public void close(Status status, Metadata trailers) {
            long durationNanos = System.nanoTime() - startTime;

            Tags tagsWithStatus = baseTags.and("grpc.status", status.getCode().name());

            meterRegistry
                .timer("grpc.server.call.duration", tagsWithStatus)
                .record(durationNanos, java.util.concurrent.TimeUnit.NANOSECONDS);

            meterRegistry.counter("grpc.server.calls.completed", tagsWithStatus).increment();

            log.debug("Recorded gRPC call: service={}, method={}, status={}, duration={}ms",
                serviceName, methodName, status.getCode().name(), durationNanos / 1_000_000.0);

            super.close(status, trailers);
          }
        };

    return new ForwardingServerCallListener.SimpleForwardingServerCallListener<>(
        next.startCall(monitoringCall, headers)) {};
  }
}
