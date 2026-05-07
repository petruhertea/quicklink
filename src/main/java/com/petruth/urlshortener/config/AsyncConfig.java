// new file: src/main/java/com/petruth/urlshortener/config/AsyncConfig.java
package com.petruth.urlshortener.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class AsyncConfig implements AsyncConfigurer {

    @Override
    @Bean(name = "taskExecutor")
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);       // always-on threads
        executor.setMaxPoolSize(8);        // burst capacity
        executor.setQueueCapacity(500);    // queue before rejecting
        executor.setThreadNamePrefix("analytics-");
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        // CallerRunsPolicy: if queue is full, run on the calling thread
        // This provides backpressure instead of crashing
        executor.initialize();
        return executor;
    }
}