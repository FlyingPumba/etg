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
        if (!properties.containsKey("testPackageName")) {
            properties.setProperty("testPackageName", properties.getProperty("packageName"));
        }
        return properties.getProperty("testPackageName");
    }

    public String getCompiledPackageName() {
        if (!properties.containsKey("compiledPackageName")) {
            properties.setProperty("compiledPackageName", properties.getProperty("packageName") + ".test");
        }
        return properties.getProperty("compiledPackageName");
    }

    public String getBuildType() {
        if (!properties.containsKey("buildType")) {
            properties.setProperty("buildType", "debug");
        }
        return properties.getProperty("buildType");
    }

    public String[] getProductFlavors() {
        if (!properties.containsKey("productFlavors")) {
            properties.setProperty("productFlavors", "");
        }


        String productFlavors = properties.getProperty("productFlavors");
        return productFlavors.split(",");
    }

    public String getRootProjectPath() {
        return properties.getProperty("rootProjectPath");
    }

    public String getOutputPath() {
        return properties.getProperty("outputPath");
    }

    public String getBuildGradlePath() throws Exception {
        if (!properties.containsKey("buildGradlePath")) {
            String applicationFolderPath = getApplicationFolderPath();
            properties.setProperty("buildGradlePath", applicationFolderPath + "build.gradle");
        }
        return properties.getProperty("buildGradlePath");
    }

    public String getApplicationFolderPath() throws Exception {
        if (!properties.containsKey("applicationFolderPath")) {
            String applicationFolderPath = getApplicationFolderPath(getRootProjectPath());
            properties.setProperty("applicationFolderPath", applicationFolderPath);
        }
        return properties.getProperty("applicationFolderPath");
    }

    private String getApplicationFolderPath(String rootProjectFolderPath) throws Exception {
        String rawCmd = "grep -l -R \"apply plugin: 'com.android.application'\" %s ";
        rawCmd += "| xargs -I {} grep -L \"com.google.android.support:wearable\" {}";
        rawCmd += "| xargs -I {} grep -L \"com.google.android.wearable:wearable\" {}";
        rawCmd += "| grep build.gradle";

        String grepCmd = String.format(rawCmd, rootProjectFolderPath);
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

    public String getEspressoVersion() throws Exception {
        if (!properties.containsKey("espressoVersion")) {
            String espressoVersion = getEspressoVersion(getRootProjectPath());
            properties.setProperty("espressoVersion", espressoVersion);
        }
        return properties.getProperty("espressoVersion");
    }

    private String getEspressoVersion(String rootProjectFolderPath) throws Exception {
        String findEspressoCoreCmd = String.format("find %s -name \"*.gradle\" -type f -exec grep \"espresso-core\" {} \\;",
                rootProjectFolderPath);
        String findEspressoCoreResult = ProcessRunner.runCommand(findEspressoCoreCmd);
        if (findEspressoCoreResult.isEmpty()) {
            throw new Exception("Couldn't find Espresso library in project. Are you sure it has Espresso setup?");
        }

        String[] firstSplit = findEspressoCoreResult.split("espresso-core:");
        if (firstSplit.length < 2) {
            throw new Exception("Couldn't find Espresso library in project. Found the following but it doesn't seem right: " + findEspressoCoreResult);
        }

        String[] secondSplit = firstSplit[1].split("'");
        String version = secondSplit[0];
        return version;
    }
}
