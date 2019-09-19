package org.etg;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class ETGProperties {

    private Properties properties;

    private ETGProperties(Properties properties) {
        this.properties = properties;
    }

    public static ETGProperties loadProperties(String arg) throws IOException {
        Properties prop = new Properties();
        String configPath = arg;
        FileInputStream ip = new FileInputStream(configPath);
        prop.load(ip);
        return new ETGProperties(prop);
    }

    public String getJsonPath() {
        return properties.getProperty("jsonPath");
    }

    public String getPackageName() {
        return properties.getProperty("packageName");
    }

    public String getTestPackageName() {
        return properties.getProperty("testPackageName");
    }

    public String getBuildVariant() {
        return properties.getProperty("buildVariant");
    }

    public String getRootProjectPath() {
        return properties.getProperty("rootProjectPath");
    }

    public String getOutputPath() {
        return properties.getProperty("outputPath");
    }
}
