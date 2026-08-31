package com.tianye.hrsystem.config;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ApplicationProfileConfigTest {

    @Test
    public void activeProfileLoadsDatasourceUrlForDebugStartup() throws IOException {
        Properties baseProperties = loadRequired("application.properties");
        String activeProfile = baseProperties.getProperty("spring.profiles.active");
        Assert.assertEquals("dev", activeProfile);

        Properties mergedProperties = new Properties();
        mergedProperties.putAll(baseProperties);
        try (InputStream inputStream = openRequired("application-" + activeProfile + ".properties")) {
            mergedProperties.load(inputStream);
        }

        String datasourceUrl = mergedProperties.getProperty("spring.datasource.url");
        Assert.assertNotNull("Active profile must provide spring.datasource.url", datasourceUrl);
        Assert.assertTrue("Datasource URL must be a MySQL JDBC URL", datasourceUrl.startsWith("jdbc:mysql://"));
    }

    private Properties loadRequired(String resourceName) throws IOException {
        Properties properties = new Properties();
        try (InputStream inputStream = openRequired(resourceName)) {
            properties.load(inputStream);
        }
        return properties;
    }

    private InputStream openRequired(String resourceName) {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourceName);
        Assert.assertNotNull("Missing classpath resource: " + resourceName, inputStream);
        return inputStream;
    }
}
