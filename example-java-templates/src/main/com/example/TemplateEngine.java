package com.example;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class TemplateEngine {
    public static String render() {
        try (InputStream input = TemplateEngine.class.getClassLoader().getResourceAsStream("config.txt")) {
            if (input == null) {
                return "Error: config.txt not found on classpath.";
            }
            try (Scanner scanner = new Scanner(input, StandardCharsets.UTF_8.name())) {
                return scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "";
            }
        } catch (Exception e) {
            throw new RuntimeException("Template reading failed", e);
        }
    }
}
