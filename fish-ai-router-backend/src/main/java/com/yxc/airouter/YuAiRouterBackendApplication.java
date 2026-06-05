package com.yxc.airouter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class YuAiRouterBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(YuAiRouterBackendApplication.class, args);
    }

}
