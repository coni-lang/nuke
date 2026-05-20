package com.example;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

public class Main {
    public static void main(String[] args) {
        String greeting = "¡Hola, mundo! \uD83C\uDF0D";
        System.out.println(greeting);
        
        try {
            Method method = Main.class.getMethod("sayHello", String.class, int.class);
            System.out.println("Method parameters reflected at runtime (-parameters flag test):");
            for (Parameter p : method.getParameters()) {
                System.out.println("  - Parameter: " + p.getName() + " (type: " + p.getType().getSimpleName() + ")");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sayHello(String customGreetingMessage, int repetitionCount) {
        // Dummy method to reflect parameters
    }
}
