package com.ailink.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableConfigurationProperties(WechatPayProperties.class)
public class WechatPayConfig {

    @Bean
    public RestTemplate wechatPayRestTemplate() {
        return new RestTemplate();
    }
}
