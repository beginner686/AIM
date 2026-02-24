package com.ailink;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.ailink.**.mapper")
@SpringBootApplication
public class AILinkApplication {

    public static void main(String[] args) {
        SpringApplication.run(AILinkApplication.class, args);
    }
}
