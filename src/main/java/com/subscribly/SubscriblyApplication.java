package com.subscribly;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SubscriblyApplication {
    public static void main(String[] args) {
        SpringApplication.run(SubscriblyApplication.class, args);
    }
}
