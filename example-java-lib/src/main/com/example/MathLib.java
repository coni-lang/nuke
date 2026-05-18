package com.example;

public class MathLib {
    public static int add(int a, int b) {
        return a + b;
    }

    public static int multiplyAndAdd(int a, int b, int c) {
        return AdvancedMath.multiply(a, b) + c;
    }
}
