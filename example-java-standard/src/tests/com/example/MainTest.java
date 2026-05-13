package com.example;

import org.junit.Test;
import static org.junit.Assert.assertTrue;

public class MainTest {
    @Test
    public void testMain() {
        assertTrue("This should pass", true);
    }

    @Test
    public void testFailure() {
        assertTrue("This is a deliberate failure for the report", false);
    }
}
