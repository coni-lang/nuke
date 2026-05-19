package com.example;

import java.io.InputStream;
import java.util.Properties;

public class Main {
    public static void main(String[] args) {
        // Local lib: math operations (uses commons-math3 transitively)
        System.out.println("Result: " + MathLib.multiplyAndAdd(5, 3, 2));
        System.out.println("5! = " + AdvancedMath.factorial(5));
        System.out.println("mean(1,2,3,4,5) = " + AdvancedMath.mean(1, 2, 3, 4, 5));

        // Local lib: properties loaded from classpath resources
        try (InputStream input = Main.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                System.out.println("Sorry, unable to find config.properties");
            } else {
                Properties prop = new Properties();
                prop.load(input);
                System.out.println("Greeting from properties: " + prop.getProperty("app.greeting"));
                System.out.println("Version from properties:  " + prop.getProperty("app.version"));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        // Local lib: Nuke template rendered at build time and loaded from classpath
        String rendered = TemplateEngine.render();
        System.out.println("--- Template output ---");
        System.out.print(rendered);
        System.out.println("-----------------------");
    }
}
