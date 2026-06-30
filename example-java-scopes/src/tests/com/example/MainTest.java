package com.example;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import org.apache.commons.lang3.StringUtils;
import javax.servlet.ServletRequest;

public class MainTest {
    @Test
    public void testCapitalize() {
        assertEquals("Hello", StringUtils.capitalize("hello"));
    }
    
    @Test
    public void testServlet() {
        Class<?> clazz = ServletRequest.class;
        assertEquals("javax.servlet.ServletRequest", clazz.getName());
    }
}
