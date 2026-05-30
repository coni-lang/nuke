package com.example;

import com.example.AdvancedMath;

/**
 * Example application demonstrating git-based dependency consumption.
 * Uses AdvancedMath from example-math-lib, resolved via :git-dependencies.
 */
public class GitDepApp {
    public static void main(String[] args) {
        System.out.println("=== Git Dependency Example ===");
        System.out.println();

        int a = 6, b = 7;
        System.out.println(a + " * " + b + " = " + AdvancedMath.multiply(a, b));

        int n = 10;
        System.out.println(n + "! = " + AdvancedMath.factorial(n));

        double avg = AdvancedMath.mean(3.0, 7.0, 11.0, 15.0);
        System.out.println("mean(3, 7, 11, 15) = " + avg);
    }
}
