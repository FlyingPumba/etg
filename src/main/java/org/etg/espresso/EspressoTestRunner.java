package org.etg.espresso;

import org.etg.ETGProperties;
import org.etg.utils.ProcessRunner;

import java.util.ArrayList;
import java.util.List;

public class EspressoTestRunner {

    private static boolean animationsDisabled = false;

    public static void disableAnimations() {
        // disable animations on emulator
        ProcessRunner.runCommand("adb shell settings put global window_animation_scale 0");
        ProcessRunner.runCommand("adb shell settings put global transition_animation_scale 0");
        ProcessRunner.runCommand("adb shell settings put global animator_duration_scale 0");
    }

    public static ArrayList<Integer> runTestCase(ETGProperties properties, EspressoTestCase espressoTestCase) throws Exception {
        String junitRunner = prepareForTestRun(properties);

        ArrayList<Integer> failingPerforms = new ArrayList<>();

        String testResult = fireTest(properties, espressoTestCase, junitRunner);

        if (!testResult.contains("OK")) {
            System.out.println("There was an error running test case: " + espressoTestCase.getTestName());
            System.out.println(testResult);
            throw new Exception("There was an error running test case: " + espressoTestCase.getTestName() + "\n"
                    + testResult);
        }

        parseFailingPerforms(failingPerforms);

        return failingPerforms;
    }

    public static String prepareForTestRun(ETGProperties properties) throws Exception {
        if (!animationsDisabled) {
            disableAnimations();
        }

        compileTests(properties);

        String apkTestPath = getAndroidTestApkPath(properties);
        uninstallPackage(properties.getCompiledTestPackageName());
        installApk(apkTestPath);

        // just in case..
        clearPackage(properties.getCompiledPackageName());
        clearPackage(properties.getCompiledTestPackageName());
        clearPackage(properties.getPackageName());
        clearPackage(properties.getTestPackageName());

        return getJunitRunner(properties);
    }

    private static String fireTest(ETGProperties properties, EspressoTestCase espressoTestCase, String junitRunner) {
        String instrumentCmd = String.format("adb shell am instrument -w -r -e emma true -e debug false -e class " +
                        "%s.%s %s/%s", properties.getTestPackageName(), espressoTestCase.getTestName(),
                properties.getCompiledTestPackageName(), junitRunner);
        return ProcessRunner.runCommand(instrumentCmd);
    }

    private static void parseFailingPerforms(ArrayList<Integer> failingPerforms) {
        String logcatCmd = "adb logcat -d -s System.out";
        String[] logcatLines = ProcessRunner.runCommand(logcatCmd).split("\n");
        for (int i = logcatLines.length - 1; i >= 0; i--) {
            String logcatLine = logcatLines[i];

            if (logcatLine.contains("Starting run of")) {
                // we reached the beginning of the test run
                break;
            } else if (logcatLine.contains("ERROR: when executing line number")) {
                String lineNumberStr = logcatLine.split("perform number: ")[1];
                Integer performNumber = Integer.valueOf(lineNumberStr);
                failingPerforms.add(performNumber);
            }
        }
    }

    private static String getJunitRunner(ETGProperties properties) throws Exception {
        String buildGradlePath = properties.getBuildGradlePath();
        String findRunnerCmd = String.format("cat %s | grep testInstrumentationRunner", buildGradlePath);
        String[] findRunnerResult = ProcessRunner.runCommand(findRunnerCmd).split("\n");

        String validRunnerLine = null;
        for (String rawLine : findRunnerResult) {
            String line = rawLine.trim();
            if (!line.startsWith("//")) {
                if (validRunnerLine == null) {
                    validRunnerLine = line;
                } else {
                    throw new Exception("Couldn't decide which Instrumentation Runner was declared: " + String.join("\n", findRunnerResult));
                }
            }
        }

        String testRunner = "";
        if (validRunnerLine == null) {
            // no custom test runner, infer it based on Espresso dependencies
            if (properties.getEspressoPackageName().contains("androidx")) {
                testRunner = "androidx.test.runner.AndroidJUnitRunner";
            } else {
                testRunner = "android.support.test.runner.AndroidJUnitRunner";
            }
        } else {
            // there is a custom test runner, use that one
            testRunner = validRunnerLine.split("testInstrumentationRunner ")[1];
            testRunner = testRunner.replace("\"", "");
            testRunner = testRunner.replace("'", "");
        }

        return testRunner;
    }

    private static void clearPackage(String packageName) {
        String clearCmd = String.format("adb shell pm clear %s", packageName);
        ProcessRunner.runCommand(clearCmd);
    }

    private static void installApk(String apkTestPath) {
        String installCmd = String.format("adb install %s", apkTestPath);
        ProcessRunner.runCommand(installCmd);
    }

    private static void uninstallPackage(String packageName) {
        String uninstallCmd = String.format("adb uninstall %s", packageName);
        ProcessRunner.runCommand(uninstallCmd);
    }

    private static String getAndroidTestApkPath(ETGProperties properties) throws Exception {
        // find where is androidTest apk
        String findApkCmd = String.format("find %s -name *androidTest.apk", properties.getApplicationFolderPath());
        String findApkResult = ProcessRunner.runCommand(findApkCmd);
        if (findApkResult.contains("No such file or directory")) {
            throw new Exception("Unable to find compiled Espresso Tests");
        }

        String[] apkPaths = findApkResult.split("\n");
        List<String> filteredApks = new ArrayList<>();
        for (String apkPath : apkPaths) {
            String[] aux = apkPath.split("/");
            String apkFileName = aux[aux.length-1];
            String lowerCaseAPK = apkFileName.toLowerCase();
            if (!lowerCaseAPK.contains(properties.getBuildType().toLowerCase())) {
                continue;
            }

            boolean discard = false;
            for (String productFlavor : properties.getProductFlavors()) {
                if (!lowerCaseAPK.contains(productFlavor.toLowerCase())) {
                    discard = true;
                    break;
                }
            }

            if (!discard) {
                filteredApks.add(apkPath);
            }
        }

        if (filteredApks.size() != 1) {
            throw new Exception("Unable to find compiled Espresso Tests");
        }
        return filteredApks.get(0);
    }

    private static void compileTests(ETGProperties properties) throws Exception {
        // delete previously built APKs
        String rmCmd = String.format("find %s -name *.apk -delete", properties.getApplicationFolderPath());
        ProcessRunner.runCommand(rmCmd);

        // compile tests
        String compileCmd = String.format("%sgradlew -p %s assembleAndroidTest",
                properties.getRootProjectPath(), properties.getRootProjectPath());
        String compileResult = ProcessRunner.runCommand(compileCmd);
        if (!compileResult.contains("BUILD SUCCESSFUL")) {
            throw new Exception("Unable to compile Espresso Tests:\n" + compileResult);
        }
    }
}
