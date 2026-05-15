package com.buddkitv2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class BuddkitV2Application {

    public static void main(String[] args) {
        SpringApplication.run(BuddkitV2Application.class, args);
    }

}
