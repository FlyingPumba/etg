package org.etg;

import org.etg.utils.ProcessRunner;

import java.io.File;
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

    public String getApplicationFolderPath() throws Exception {
        if (!properties.containsKey("applicationFolderPath")) {
            String applicationFolderPath = getApplicationFolderPath(getRootProjectPath());
            properties.setProperty("applicationFolderPath", applicationFolderPath);
        }
        return properties.getProperty("applicationFolderPath");
    }

    private String getApplicationFolderPath(String rootProjectFolderPath) throws Exception {
        String grepCmd = String.format("grep -l -R \"apply plugin: 'com.android.application'\" %s", rootProjectFolderPath);
        String[] grepResult = ProcessRunner.runCommand(grepCmd).split("\n");
        if (grepResult.length != 1) {
            throw new Exception("Unable to find application path inside project.");
        }
        return new File(grepResult[0]).getParent() + File.separator;
    }

    public String getEspressoPackageName() throws Exception {
        if (!properties.containsKey("espressoPackageName")) {
            String espressoPackageName = getEspressoPackageName(getRootProjectPath());
            properties.setProperty("espressoPackageName", espressoPackageName);
        }
        return properties.getProperty("espressoPackageName");
    }

    private String getEspressoPackageName(String rootProjectFolderPath) throws Exception {
        String findSupportTestCmd = String.format("find %s -name \"*.gradle\" -type f -exec grep \"com.android.support.test.espresso\" {} \\;",
                rootProjectFolderPath);
        String findSupportTestResult = ProcessRunner.runCommand(findSupportTestCmd);
        if (!findSupportTestResult.isEmpty()) {
            return "android.support.test";
        }

        String findAndroidXTestCmd = String.format("find %s -name \"*.gradle\" -type f -exec grep \"androidx.test.espresso\" {} \\;",
                rootProjectFolderPath);
        String findAndroidXTestResult = ProcessRunner.runCommand(findAndroidXTestCmd);
        if (!findAndroidXTestResult.isEmpty()) {
            return "androidx.test";
        }

        throw new Exception("Couldn't find Espresso library in project. Are you sure it has Espresso setup?");
    }
}
