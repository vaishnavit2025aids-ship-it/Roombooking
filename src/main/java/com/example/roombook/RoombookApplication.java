package com.example.roombook;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RoombookApplication {

    public static void main(String[] args) {
        SpringApplication.run(RoombookApplication.class, args);
    }
}