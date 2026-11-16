package com.smartbank;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SmartBankingApplication {

    private static final Logger logger = LoggerFactory.getLogger(SmartBankingApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(SmartBankingApplication.class, args);
        logger.info("==========================================================");
        logger.info("  Smart Banking Management System started successfully!");
        logger.info("  Web Portal: http://localhost:8080");
        logger.info("  REST APIs:  http://localhost:8080/api");
        logger.info("==========================================================");
    }
}
