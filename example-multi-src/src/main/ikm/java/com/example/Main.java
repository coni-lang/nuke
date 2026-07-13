package com.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Properties;
import java.io.InputStream;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        logger.info("Hello from multi-src example!");
        
        System.out.println("--- Source Directory Test ---");
        System.out.println(SecondaryClass.getGreeting());
        
        System.out.println("\n--- Resource Directory Test ---");
        printProperty("/main.properties");
        printProperty("/ikm.properties");
        printProperty("/bts.properties");
        
        System.out.println("\nRunning Main class successfully!");
    }
    
    private static void printProperty(String resourceName) {
        try (InputStream is = Main.class.getResourceAsStream(resourceName)) {
            if (is != null) {
                Properties props = new Properties();
                props.load(is);
                System.out.println(resourceName + " -> " + props.getProperty("message"));
            } else {
                System.out.println(resourceName + " -> NOT FOUND!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
