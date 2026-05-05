package com.stock_tracker.grey_toad;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GreyToadApplication {

    public static void main(String[] args) {
        SpringApplication.run(GreyToadApplication.class, args);
    }

}
