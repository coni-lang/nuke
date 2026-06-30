package com.example;

import org.apache.commons.lang3.StringUtils;
import javax.servlet.ServletRequest;

public class Main {
    public static void main(String[] args) {
        System.out.println(StringUtils.capitalize("hello world from scoped dependencies!"));
        
        // Just referencing a provided dependency class to ensure it compiles
        Class<?> clazz = ServletRequest.class;
        System.out.println("ServletRequest is available at runtime (in nuke run): " + clazz.getName());
    }
}
