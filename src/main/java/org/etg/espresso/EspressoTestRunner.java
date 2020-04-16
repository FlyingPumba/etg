package org.etg.espresso;

import org.etg.ETGProperties;
import org.etg.espresso.codegen.TestCodeGenerator;
import org.etg.utils.ProcessRunner;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class EspressoTestRunner {

    private static boolean animationsDisabled = false;

    public static void disableAnimations() {
        // disable animations on emulator
        ProcessRunner.runCommand("adb shell settings put global window_animation_scale 0");
        ProcessRunner.runCommand("adb shell settings put global transition_animation_scale 0");
        ProcessRunner.runCommand("adb shell settings put global animator_duration_scale 0");
    }

    public static List<Integer> runTestCase(EspressoTestCase espressoTestCase, boolean coverage) throws Exception {
        String junitRunner = prepareForTestRun(espressoTestCase.getEtgProperties());

        String testResult = fireTest(espressoTestCase, junitRunner, coverage);

        if (!testResult.contains("OK")) {
            System.out.println("There was an error running test case: " + espressoTestCase.getTestName());
            System.out.println(testResult);
            throw new Exception("There was an error running test case: " + espressoTestCase.getTestName() + "\n"
                    + testResult);
        }

        return parseFailingPerforms();
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

        grantAllPermissions(properties.getCompiledPackageName());

        return getJunitRunner(properties);
    }

    private static String fireTest(EspressoTestCase espressoTestCase, String junitRunner,
                                   boolean coverage) {
        String coverageFlags = "";
        if (coverage) {
            // Add coverage flags to command and make sure folder exists
            coverageFlags = String.format("-e coverage true -e coverageFile %s",
                    Coverage.getRemoteCoverageEcPath(espressoTestCase.getEtgProperties()));
            ProcessRunner.runCommand(String.format("adb shell mkdir -p %s",
                    Coverage.getRemoteCoverageEcFolderPath(espressoTestCase.getEtgProperties())));
        }

        String instrumentCmd = String.format("adb shell am instrument -w -r %s -e debug false -e class " +
                        "%s.%s %s/%s",
                coverageFlags,
                espressoTestCase.getEtgProperties().getTestPackageName(),
                espressoTestCase.getTestName(),
                espressoTestCase.getEtgProperties().getCompiledTestPackageName(),
                junitRunner);
        String output = ProcessRunner.runCommand(instrumentCmd);

        // Add some time sleep after executing test to avoid clogging the emulator
        try {
            System.out.println("Waiting 5 seconds after test");
            int seconds = 5;
            Thread.sleep(seconds * 1000);
        } catch (InterruptedException e) {
            // do nothing
        }

        return output;
    }

    private static List<Integer> parseFailingPerforms() {
        List<Integer> failingPerforms = new ArrayList<>();

        String logcatCmd = "adb logcat -d -s System.out";
        String[] logcatLines = ProcessRunner.runCommand(logcatCmd).split("\n");
        for (int i = logcatLines.length - 1; i >= 0; i--) {
            String logcatLine = logcatLines[i];

            if (logcatLine.contains("Starting run of")) {
                // we reached the beginning of the test run
                break;
            } else if (logcatLine.contains("ERROR: when executing line number")) {
                String performNumberStr = logcatLine.split("perform number: ")[1];
                Integer performNumber = Integer.valueOf(performNumberStr);
                failingPerforms.add(performNumber);
            }
        }

        // sort by lowest number first.
        failingPerforms.sort(Comparator.comparingInt(Integer::intValue));

        return failingPerforms;
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

    private static void grantAllPermissions(String packageName) {
        String permissionsCmd = "adb shell pm list permissions -d -g | grep permission: | cut -d':' -f2";
        String output = ProcessRunner.runCommand(permissionsCmd);
        for (String permission: output.split("\n")) {
            grantPermission(packageName, permission);
        }
    }

    private static void grantPermission(String packageName, String permission) {
        String grantCmd = String.format("adb shell pm grant %s %s >/dev/null 2>&1", packageName, permission);
        ProcessRunner.runCommand(grantCmd);
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

    public static void cleanOutputPath(ETGProperties properties) {
        // delete existing ETG test cases
        String rmCmd = String.format("rm %s/%s*.java", properties.getOutputPath(),
                TestCodeGenerator.getETGTestCaseNamePrefix());
        ProcessRunner.runCommand(rmCmd);
    }
}
