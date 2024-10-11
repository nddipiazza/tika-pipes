package org.apache.tika.pipes.config;

import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import org.apache.tika.fork.ForkParser;

@Configuration
public class ForkParserConfig {
    @Value("${forkparser.pool.core-size}")
    private Integer poolCoreSize;

    @Value("${forkparser.pool.max-size}")
    private Integer poolMaxSize;

    @Value("${forkparser.pool.queue-capacity}")
    private Integer poolQueueCapacity;

    @Bean
    public Executor forkParserExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(poolCoreSize);
        executor.setMaxPoolSize(poolMaxSize);  // maximum number of threads
        executor.setQueueCapacity(poolQueueCapacity);  // queue size for tasks
        executor.setThreadNamePrefix("ForkParserPool-");
        executor.initialize();
        return executor;
    }

    @Bean
    public ForkParser createForkParser() {
        return new ForkParser();
    }
}
