package com.ailink;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@MapperScan("com.ailink.**.mapper")
@SpringBootApplication
@EnableScheduling
public class AILinkApplication {

    public static void main(String[] args) {
        SpringApplication.run(AILinkApplication.class, args);
    }
}
