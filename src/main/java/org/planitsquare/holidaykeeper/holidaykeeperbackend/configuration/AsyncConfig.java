package org.planitsquare.holidaykeeper.holidaykeeperbackend.configuration;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "holidayExecutor")
    public Executor holidayExecutor() {

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(10);   // 동시 국가/연도 작업 수행 스레드 수
        executor.setMaxPoolSize(20);    // 최대 확장 개수
        executor.setQueueCapacity(100); // 대기 큐
        executor.setThreadNamePrefix("HolidaySync-");
        executor.initialize();

        return executor;
    }
}