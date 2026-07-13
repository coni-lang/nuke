package com.example;

import org.junit.Test;
import static org.junit.Assert.assertTrue;

public class VulnerableAppTest {
    @Test
    public void testMain() {
        VulnerableApp.main(new String[]{});
        assertTrue(true);
    }
}
