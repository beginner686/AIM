package com.ailink.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.time.Duration;

@Configuration
@EnableConfigurationProperties(WechatPayProperties.class)
public class WechatPayConfig {

    @Bean
    public RestTemplate wechatPayRestTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(10))
                .additionalInterceptors((request, body, execution) -> {
                    int maxRetries = 3;
                    IOException lastException = null;
                    for (int i = 0; i < maxRetries; i++) {
                        try {
                            return execution.execute(request, body);
                        } catch (IOException e) {
                            lastException = e;
                        }
                    }
                    if (lastException != null) {
                        throw lastException;
                    }
                    throw new IOException("Execute failed without exception"); // Should never happen
                })
                .build();
    }
}
