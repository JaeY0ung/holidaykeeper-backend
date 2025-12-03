package org.planitsquare.holidaykeeper.holidaykeeperbackend.configuration;

import java.time.Duration;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
@Getter
public class NagerApiConfig {

    @Value("${nager.api.base-url}")
    private String baseUrl;

    @Value("${nager.api.connect-timeout}")
    private int connectTimeout;

    @Value("${nager.api.read-timeout}")
    private int readTimeout;

    @Bean
    public RestTemplate nagerRestTemplate(RestTemplateBuilder builder) {

        return builder
            .connectTimeout(Duration.ofMillis(connectTimeout))
            .readTimeout(Duration.ofMillis(readTimeout))
            .build();
    }

}