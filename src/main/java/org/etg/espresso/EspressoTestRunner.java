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
        if (!animationsDisabled) {
            disableAnimations();
        }

        ArrayList<Integer> failingPerforms = new ArrayList<>();

        compileTests(properties);

        String apkTestPath = getAndroidTestApkPath(properties);

        installApk(apkTestPath);

        clearPackage(properties);

        String junitRunner = getJunitRunner(properties);

        String testResult = fireTest(properties, espressoTestCase, junitRunner);

        if (!testResult.contains("OK")) {
            System.out.println("There was an error running test case: " + espressoTestCase.getTestName());
            System.out.println(testResult);
            return failingPerforms;
        }

        parseFailingPerforms(failingPerforms);

        return failingPerforms;
    }

    private static String fireTest(ETGProperties properties, EspressoTestCase espressoTestCase, String junitRunner) {
        String instrumentCmd = String.format("adb shell am instrument -w -r -e emma true -e debug false -e class " +
                        "%s.%s %s.test/%s", properties.getTestPackageName(), espressoTestCase.getTestName(),
                properties.getPackageName(), junitRunner);
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
        String junitRunner = "";
        if (properties.getEspressoPackageName().contains("androidx")) {
            junitRunner = "androidx.test.runner.AndroidJUnitRunner";
        } else {
            junitRunner = "android.support.test.runner.AndroidJUnitRunner";
        }
        return junitRunner;
    }

    private static void clearPackage(ETGProperties properties) {
        String clearCmd = String.format("adb shell pm clear %s", properties.getPackageName());
        ProcessRunner.runCommand(clearCmd);
    }

    private static void installApk(String apkTestPath) {
        String installCmd = String.format("adb install %s", apkTestPath);
        ProcessRunner.runCommand(installCmd);
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
