package com.familyfinance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FamilyFinanceApplication {
    public static void main(String[] args) {
        SpringApplication.run(FamilyFinanceApplication.class, args);
    }
}
