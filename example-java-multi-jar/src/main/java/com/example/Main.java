package com.example;
import java.io.InputStream;
import java.util.Properties;

public class Main {
    public static void main(String[] args) {
        System.out.println("Hello from Multi-Jar example!");
        try {
            Properties props = new Properties();
            InputStream in = Main.class.getResourceAsStream("/app-a.properties");
            if (in != null) {
                props.load(in);
                System.out.println("Loaded app-a.properties: " + props.getProperty("name"));
            } else {
                in = Main.class.getResourceAsStream("/app-b.properties");
                if (in != null) {
                    props.load(in);
                    System.out.println("Loaded app-b.properties: " + props.getProperty("name"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
