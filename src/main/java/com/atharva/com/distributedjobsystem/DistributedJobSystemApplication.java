package com.atharva.com.distributedjobsystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DistributedJobSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(DistributedJobSystemApplication.class, args);
    }

}
