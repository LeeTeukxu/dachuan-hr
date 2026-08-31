package com.tianye.hrsystem.config;

import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertTrue;

public class TomcatHeaderSizeConfigTest {
    private static final int MIN_HEADER_SIZE = 65536;

    @Test
    public void profilesAllowLargeJwtTokenHeaders() throws IOException {
        List<String> profiles = Arrays.asList(
                "application-dev.properties",
                "application-prod.properties",
                "application-temp.properties"
        );

        for (String profile : profiles) {
            Properties properties = load(profile);
            int configuredSize = Integer.parseInt(properties.getProperty("server.max-http-header-size", "0"));
            assertTrue(profile + " should allow large JWT token headers", configuredSize >= MIN_HEADER_SIZE);
        }
    }

    private Properties load(String resourceName) throws IOException {
        Properties properties = new Properties();
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourceName)) {
            if (inputStream != null) {
                properties.load(inputStream);
            }
        }
        return properties;
    }
}
