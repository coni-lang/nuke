package com.example;

import java.io.InputStream;
import java.util.Properties;

public class Main {
    public static void main(String[] args) {
        System.out.println("Result: " + MathLib.add(10, 7));
        
        try (InputStream input = Main.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                System.out.println("Sorry, unable to find config.properties");
                return;
            }
            Properties prop = new Properties();
            prop.load(input);
            System.out.println("Greeting from properties: " + prop.getProperty("app.greeting"));
            System.out.println("Version from properties: " + prop.getProperty("app.version"));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
