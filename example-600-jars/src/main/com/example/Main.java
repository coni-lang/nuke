package com.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Minimal main class that imports from a few of our 600+ transitive
 * dependency JARs, just enough to prove the compile succeeds.
 */
public class Main {
    public static void main(String[] args) throws Exception {
        System.out.println("=== example-600-jars ===");

        // Commons Lang
        String greeting = StringUtils.capitalize("hello from 600 jars!");
        System.out.println(greeting);

        // Guava
        ImmutableList<String> items = ImmutableList.of("spring", "camel", "hibernate", "aws", "netty");
        System.out.println("Frameworks : " + items);

        // Jackson
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> info = new HashMap<>();
        info.put("project", "example-600-jars");
        info.put("purpose", "test @argfile for long classpaths on Windows");
        info.put("jarCount", "~600");
        System.out.println("Info: " + mapper.writeValueAsString(info));

        System.out.println("=== compile + run succeeded! ===");
    }
}
