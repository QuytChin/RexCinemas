package com.rexchain.cinema;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RexChainCinemaApplication {
    public static void main(String[] args) {
        SpringApplication.run(RexChainCinemaApplication.class, args);
    }
}
