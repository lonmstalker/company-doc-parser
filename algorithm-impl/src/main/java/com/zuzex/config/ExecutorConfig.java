package com.zuzex.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;

@Configuration(proxyBeanMethods = false)
public class ExecutorConfig {

  @Bean
  public ExecutorCompletionService<Object> executorCompletionService() {
    return new ExecutorCompletionService<>(
        Executors.newWorkStealingPool(Runtime.getRuntime().availableProcessors()));
  }
}
