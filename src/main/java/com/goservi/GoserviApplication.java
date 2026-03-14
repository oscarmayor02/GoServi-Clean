package com.goservi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GoserviApplication {
    public static void main(String[] args) {
        SpringApplication.run(GoserviApplication.class, args);
    }
}
