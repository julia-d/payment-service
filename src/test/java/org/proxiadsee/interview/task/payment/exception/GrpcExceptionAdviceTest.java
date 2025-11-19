package org.proxiadsee.interview.task.payment.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.grpc.ForwardingServerCallListener.SimpleForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class GrpcExceptionAdviceTest {

  private GrpcExceptionAdvice advice;

  @BeforeEach
  void setUp() {
    advice = new GrpcExceptionAdvice();
  }

  @Test
  void validationExceptionIsMappedToInvalidArgument() {
    @SuppressWarnings("unchecked")
    ServerCall<String, String> call = mock(ServerCall.class);
    @SuppressWarnings("unchecked")
    ServerCallHandler<String, String> handler = mock(ServerCallHandler.class);

    SimpleForwardingServerCallListener<String> listener =
        new SimpleForwardingServerCallListener<String>(new ServerCall.Listener<>() {}) {
          @Override
          public void onHalfClose() {
            throw new ValidationException("bad");
          }
        };

    when(handler.startCall(any(), any())).thenReturn(listener);

    advice.interceptCall(call, new Metadata(), handler).onHalfClose();

    ArgumentCaptor<Status> statusCaptor = ArgumentCaptor.forClass(Status.class);
    verify(call).close(statusCaptor.capture(), any(Metadata.class));
    Status status = statusCaptor.getValue();
    assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
  }

  @Test
  void constraintViolationIsMappedToInvalidArgument() {
    @SuppressWarnings("unchecked")
    ServerCall<String, String> call = mock(ServerCall.class);
    @SuppressWarnings("unchecked")
    ServerCallHandler<String, String> handler = mock(ServerCallHandler.class);

    SimpleForwardingServerCallListener<String> listener =
        new SimpleForwardingServerCallListener<String>(new ServerCall.Listener<>() {}) {
          @Override
          public void onHalfClose() {
            throw new ConstraintViolationException(null);
          }
        };

    when(handler.startCall(any(), any())).thenReturn(listener);

    advice.interceptCall(call, new Metadata(), handler).onHalfClose();

    ArgumentCaptor<Status> statusCaptor = ArgumentCaptor.forClass(Status.class);
    verify(call).close(statusCaptor.capture(), any(Metadata.class));
    Status status = statusCaptor.getValue();
    assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
  }

  @Test
  void serviceExceptionIsMappedToInternal() {
    @SuppressWarnings("unchecked")
    ServerCall<String, String> call = mock(ServerCall.class);
    @SuppressWarnings("unchecked")
    ServerCallHandler<String, String> handler = mock(ServerCallHandler.class);

    SimpleForwardingServerCallListener<String> listener =
        new SimpleForwardingServerCallListener<String>(new ServerCall.Listener<>() {}) {
          @Override
          public void onHalfClose() {
            throw new ServiceException("boom");
          }
        };

    when(handler.startCall(any(), any())).thenReturn(listener);

    advice.interceptCall(call, new Metadata(), handler).onHalfClose();

    ArgumentCaptor<Status> statusCaptor = ArgumentCaptor.forClass(Status.class);
    verify(call).close(statusCaptor.capture(), any(Metadata.class));
    Status status = statusCaptor.getValue();
    assertEquals(Status.INTERNAL.getCode(), status.getCode());
  }

  @Test
  void statusRuntimeExceptionIsPassedThrough() {
    @SuppressWarnings("unchecked")
    ServerCall<String, String> call = mock(ServerCall.class);
    @SuppressWarnings("unchecked")
    ServerCallHandler<String, String> handler = mock(ServerCallHandler.class);

    SimpleForwardingServerCallListener<String> listener =
        new SimpleForwardingServerCallListener<String>(new ServerCall.Listener<>() {}) {
          @Override
          public void onHalfClose() {
            throw new StatusRuntimeException(Status.NOT_FOUND.withDescription("not found"));
          }
        };

    when(handler.startCall(any(), any())).thenReturn(listener);

    advice.interceptCall(call, new Metadata(), handler).onHalfClose();

    ArgumentCaptor<Status> statusCaptor = ArgumentCaptor.forClass(Status.class);
    verify(call).close(statusCaptor.capture(), any(Metadata.class));
    Status status = statusCaptor.getValue();
    assertEquals(Status.NOT_FOUND.getCode(), status.getCode());
  }

  @Test
  void unknownThrowableMapsToInternal() {
    @SuppressWarnings("unchecked")
    ServerCall<String, String> call = mock(ServerCall.class);
    @SuppressWarnings("unchecked")
    ServerCallHandler<String, String> handler = mock(ServerCallHandler.class);

    SimpleForwardingServerCallListener<String> listener =
        new SimpleForwardingServerCallListener<String>(new ServerCall.Listener<>() {}) {
          @Override
          public void onHalfClose() {
            throw new RuntimeException("uh oh");
          }
        };

    when(handler.startCall(any(), any())).thenReturn(listener);

    advice.interceptCall(call, new Metadata(), handler).onHalfClose();

    ArgumentCaptor<Status> statusCaptor = ArgumentCaptor.forClass(Status.class);
    verify(call).close(statusCaptor.capture(), any(Metadata.class));
    Status status = statusCaptor.getValue();
    assertEquals(Status.INTERNAL.getCode(), status.getCode());
  }
}
