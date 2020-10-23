package org.etg;

import org.etg.espresso.codegen.codeMapper.CodeMapperType;
import org.etg.utils.ProcessRunner;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;

public class ETGProperties {

    private Properties properties;
    private Args args;

    private ETGProperties(Properties properties, Args args) {
        this.properties = properties;
        this.args = args;
    }

    public static ETGProperties loadProperties(String arg, Args args) throws IOException {
        Properties prop = new Properties();
        String configPath = arg;
        FileInputStream ip = new FileInputStream(configPath);
        prop.load(ip);
        return new ETGProperties(prop, args);
    }

    public List<String> getJsonPaths() {
        HashSet<String> jsonPaths = new HashSet<>();
        if (properties.getProperty("jsonPath") != null) {
            jsonPaths.add(properties.getProperty("jsonPath"));
        }
        jsonPaths.addAll(args.getJSONPaths());

        return new ArrayList<>(jsonPaths);
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

    public List<String> getJsonsMD5() {
        List<String> md5s = new ArrayList<>();

        for (String jsonPath : getJsonPaths()) {
            String md5Cmd = String.format("md5sum %s ", jsonPath);
            String md5Result = ProcessRunner.runCommand(md5Cmd);
            md5s.add(md5Result.split(" ")[0]);
        }

        return md5s;
    }

    public String getMainActivity() throws Exception {
        if (!properties.containsKey("mainActivity")) {
            String mainActivity = getMainActivityUsingADB();
            properties.setProperty("mainActivity", mainActivity);
        }

        return properties.getProperty("mainActivity");
    }

    public String getMainActivityUsingADB() throws Exception {
        String pmDumpCmd = String.format("adb shell pm dump %s", getCompiledPackageName());
        String[] lines = ProcessRunner.runCommand(pmDumpCmd).split("\n");

        String activity = "";
        for (String line: lines) {
            if (line.contains("filter")) {
                // we found a new activity down the stream of pm dump
                activity = line.split("/")[1].split(" ")[0];
            }

            if (line.contains("android.intent.category.LAUNCHER")) {
                // the last activity we found is the one we want
                break;
            }
        }

        if (activity.isEmpty()) {
            throw new Exception("Couldn't find Main Activity for package " + getCompiledPackageName());
        }

        if (activity.startsWith(".")) {
            activity = getPackageName() + activity;
        }

        return activity;
    }

    public List<String> getRuntimePermissionsUsingADB() {
        String dumpsysCmd = String.format("adb shell dumpsys package %s", getCompiledPackageName());
        String[] lines = ProcessRunner.runCommand(dumpsysCmd).split("\n");

        List<String> runtimePermissions = new ArrayList<>();
        boolean inRuntimePermissionsSection = false;
        for (String line: lines) {
            if (line.contains("runtime permissions")) {
                // we are in the runtime permissions section
                inRuntimePermissionsSection = true;
                continue;
            }

            if (inRuntimePermissionsSection) {
                if (line.contains("android.permission")) {
                    String permission = line.split("android.permission.")[1].split(":")[0];
                    runtimePermissions.add(permission);
                } else {
                    break;
                }
            }
        }

        return runtimePermissions;
    }

    public String getETGResultsPath() {
        return String.format("%s/etg", args.getResultsPath());
    }

    public boolean useKotlinFormat() {
        return args.useKotlinFormat();
    }

    public String getOutputExtension() {
        return useKotlinFormat()? "kt" : "java";
    }

    public int getSleepAfterActions() {
        return args.getSleepAfterActions();
    }

    public int getSleepAfterLaunch() {
        return args.getSleepAfterLaunch();
    }

    public boolean disableTextMatchers() {
        return args.disableTextMatchers();
    }

    public CodeMapperType getCodeMapper() {
        String codeMapper = args.getCodeMapper();
        if (codeMapper == null) {
            return CodeMapperType.Standard;
        }

        if (codeMapper.equals(CodeMapperType.Standard.name())) {
            return CodeMapperType.Standard;
        } else if (codeMapper.equals(CodeMapperType.RobotPattern.name())) {
            return CodeMapperType.RobotPattern;
        } else {
            throw new IllegalArgumentException(String.format("The code mapper %s is not valid", codeMapper));
        }
    }
}
