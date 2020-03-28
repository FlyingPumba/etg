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

    public String getCompiledTestPackageName() {
        if (!properties.containsKey("compiledTestPackageName")) {
            properties.setProperty("compiledTestPackageName", properties.getProperty("packageName") + ".test");
        }
        return properties.getProperty("compiledTestPackageName");
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
        if (productFlavors.isEmpty()) {
            return new String[0];
        } else {
            return productFlavors.split(",");
        }
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
        String rawCmd = "grep -l -R \"'com.android.application'\" %s ";
        rawCmd += "| xargs -I {} grep -L \"com.google.android.support:wearable\" {}";
        rawCmd += "| xargs -I {} grep -L \"com.google.android.wearable:wearable\" {}";
        rawCmd += "| grep \"build.gradle$\"";

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
            String espressoVersion = getEspressoVersionFromGradleFile(getRootProjectPath());
            if (espressoVersion.startsWith("$")) {
                // The actual version is defined elsewhere
                // Try to use the gradle dependencies command to get the value.
                espressoVersion = getEspressoVersionFromGradleDependencies(getRootProjectPath());
            }
            properties.setProperty("espressoVersion", espressoVersion);
        }
        return properties.getProperty("espressoVersion");
    }

    private String getEspressoVersionFromGradleFile(String rootProjectFolderPath) throws Exception {
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

    private String getEspressoVersionFromGradleDependencies(String rootProjectFolderPath) throws Exception {
        String dependenciesCmd = String.format("%sgradlew -p %s androidDependencies 2>&1 | grep espresso-core | head -n 1",
                rootProjectFolderPath, rootProjectFolderPath);
        String dependenciesResult = ProcessRunner.runCommand(dependenciesCmd);

        if (dependenciesResult.isEmpty()) {
            throw new Exception("Couldn't find Espresso library in project after looking at gradle dependencies");
        }

        String[] firstSplit = dependenciesResult.split("espresso-core:");
        if (firstSplit.length < 2) {
            throw new Exception("Couldn't find Espresso library in project after looking at gradle dependencies. Found the following but it doesn't seem right: " + dependenciesResult);
        }

        String[] secondSplit = firstSplit[1].split(" ");
        String[] thirdSplit = secondSplit[0].split("@aar");
        String version = thirdSplit[0];
        return version;
    }

    public String getJsonMD5() {
        String jsonPath = this.getJsonPath();
        String md5Cmd = String.format("md5sum %s ", jsonPath);
        String md5Result = ProcessRunner.runCommand(md5Cmd);
        return md5Result.split(" ")[0];
    }

    public String getMainActivity() throws Exception {
        if (!properties.containsKey("mainActivity")) {
            String mainActivity = getMainActivityUsingADB();
            properties.setProperty("mainActivity", mainActivity);
        }

        return properties.getProperty("mainActivity");
    }

    public String getMainActivityUsingADB() throws Exception {
        String mainActivityCmd = String.format("adb shell pm dump %s |  grep -A 1 'filter' | head -n 1 | cut -d ' ' -f 10 | cut -d'/' -f 2", getCompiledPackageName());
        String mainActivityResult = ProcessRunner.runCommand(mainActivityCmd).replace("\n", "");
        if (mainActivityResult.isEmpty()) {
            throw new Exception("Couldn't find Main Activity for package " + getCompiledPackageName());
        }

        if (mainActivityResult.startsWith(".")) {
            mainActivityResult = getPackageName() + mainActivityResult;
        }

        return mainActivityResult;
    }
}
