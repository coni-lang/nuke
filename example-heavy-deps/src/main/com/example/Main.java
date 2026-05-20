package com.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.commons.lang3.StringUtils;
import com.google.common.collect.ImmutableList;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;

import java.util.HashMap;
import java.util.Map;

public class Main {
    private static final Logger logger = LogManager.getLogger(Main.class);

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("  Starting Nuke Heavy Dependencies Application  ");
        System.out.println("=================================================");

        // 1. Log4j2 Test
        logger.info("Log4j2 Logger successfully initialized and working!");

        // 2. Commons Lang Test
        String text = "   hello from nuke transitive deps!   ";
        System.out.println("Commons Lang: " + StringUtils.capitalize(StringUtils.trim(text)));

        // 3. Guava Test
        ImmutableList<String> list = ImmutableList.of("Guava", "Transitive", "Resolution", "Works!");
        System.out.println("Guava List: " + list);

        // 4. Jackson Test
        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> map = new HashMap<>();
            map.put("status", "success");
            map.put("transitiveCount", 15);
            String json = mapper.writeValueAsString(map);
            System.out.println("Jackson JSON serialization: " + json);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 5. HttpClient5 Test
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            System.out.println("HttpClient5: CloseableHttpClient successfully instantiated: " + httpClient.getClass().getName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("=================================================");
    }
}
