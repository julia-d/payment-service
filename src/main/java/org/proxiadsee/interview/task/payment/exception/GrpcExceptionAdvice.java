package org.proxiadsee.interview.task.payment.exception;

import io.grpc.ForwardingServerCallListener.SimpleForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class GrpcExceptionAdvice implements ServerInterceptor {

  @Override
  public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
      ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {

    ServerCall.Listener<ReqT> delegate = next.startCall(call, headers);

    return new SimpleForwardingServerCallListener<>(delegate) {

      @Override
      public void onHalfClose() {
        try {
          super.onHalfClose();
        } catch (ConflictException ex) {
          log.warn("Conflict exception: {}", ex.getMessage());
          call.close(Status.INVALID_ARGUMENT.withDescription(ex.getMessage()), new Metadata());
        } catch (ValidationException ex) {
          log.warn("Validation failed: {}", ex.getMessage());
          call.close(Status.INVALID_ARGUMENT.withDescription(ex.getMessage()), new Metadata());
        } catch (ConstraintViolationException ex) {
          log.warn("Constraint violations: {}", ex.getMessage());
          call.close(Status.INVALID_ARGUMENT.withDescription(ex.getMessage()), new Metadata());
        } catch (ServiceException ex) {
          log.error("Service error: {}", ex.getMessage(), ex);
          call.close(Status.INTERNAL.withDescription("Internal server error"), new Metadata());
        } catch (StatusRuntimeException ex) {
          log.debug("StatusRuntimeException thrown: {}", ex.getMessage());
          call.close(ex.getStatus(), new Metadata());
        } catch (Throwable ex) {
          log.error("Unexpected error in gRPC call", ex);
          call.close(Status.INTERNAL.withDescription("Internal server error"), new Metadata());
        }
      }

      @Override
      public void onMessage(ReqT message) {
        try {
          super.onMessage(message);
        } catch (Throwable ex) {
          log.error("Error while processing message", ex);
          call.close(
              Status.INTERNAL.withDescription("Error while processing message"), new Metadata());
        }
      }

      @Override
      public void onCancel() {
        try {
          super.onCancel();
        } catch (Throwable ex) {
          log.debug("Exception during cancel: {}", ex.getMessage(), ex);
        }
      }

      @Override
      public void onComplete() {
        try {
          super.onComplete();
        } catch (Throwable ex) {
          log.debug("Exception during complete: {}", ex.getMessage(), ex);
        }
      }
    };
  }
}
