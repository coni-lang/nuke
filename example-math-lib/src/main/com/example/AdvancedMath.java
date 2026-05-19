package com.example;

import org.apache.commons.math3.util.CombinatoricsUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

public class AdvancedMath {

    public static int multiply(int a, int b) {
        return a * b;
    }

    /** Returns n! using Apache Commons Math */
    public static long factorial(int n) {
        return CombinatoricsUtils.factorial(n);
    }

    /** Returns the mean of the given values using Apache Commons Math */
    public static double mean(double... values) {
        DescriptiveStatistics stats = new DescriptiveStatistics();
        for (double v : values) stats.addValue(v);
        return stats.getMean();
    }
}
