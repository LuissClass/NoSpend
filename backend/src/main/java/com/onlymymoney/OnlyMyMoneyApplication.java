package com.onlymymoney;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of the OnlyMyMoney application.
 * <p>
 * {@link SpringBootApplication} enables auto-configuration and component scanning
 * starting from the {@code com.onlymymoney} package.
 */
@SpringBootApplication
public class OnlyMyMoneyApplication {
    /**
     * Starts the Spring Boot application.
     *
     * @param args command-line arguments passed to Spring
     */
    public static void main(String[] args) {
        SpringApplication.run(OnlyMyMoneyApplication.class, args);
    }
}