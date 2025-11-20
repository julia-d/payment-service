package org.proxiadsee.interview.task.payment.config;

import io.grpc.ServerInterceptor;
import lombok.RequiredArgsConstructor;
import org.proxiadsee.interview.task.payment.exception.GrpcExceptionAdvice;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.server.GlobalServerInterceptor;

@Configuration
@RequiredArgsConstructor
public class GrpcServerConfiguration {

  private final GrpcExceptionAdvice grpcExceptionAdvice;

  @Bean
  @GlobalServerInterceptor
  public ServerInterceptor grpcExceptionInterceptor() {
    return grpcExceptionAdvice;
  }
}
