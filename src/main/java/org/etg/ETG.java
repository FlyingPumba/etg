package org.etg;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.etg.espresso.EspressoTestCase;
import org.etg.espresso.codegen.TestCodeGenerator;
import org.etg.mate.models.WidgetTestCase;
import org.etg.mate.parser.TestCaseParser;
import org.etg.utils.ProcessRunner;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ETG {
    public static void main(String[] args) {
        System.out.println("ETG");

        String filePath = args[0];
        String packageName = args[1];
        String testPackageName = args[2];
        String buildVariant = args[3];
        String rootProjectFolderPath = args[4];
        String outputFolderPath = args[5];

        System.out.println("Working on file with path: " + filePath + " and package name: " + packageName);

        try {
            String applicationFolderPath = getApplicationFolderPath(rootProjectFolderPath);
            String espressoPackageName = getEspressoPackageName(rootProjectFolderPath);

            List<WidgetTestCase> widgetTestCases = parseTestCases(filePath);

            TestCodeGenerator codeGenerator = new TestCodeGenerator(packageName, testPackageName, espressoPackageName);
            List<EspressoTestCase> espressoTestCases = codeGenerator.getEspressoTestCases(widgetTestCases);

            // prune failing lines from each test case
            writeTestCases(outputFolderPath, espressoTestCases);
            prepareTestRun(rootProjectFolderPath);

            for (int i = 0; i < espressoTestCases.size(); i++) {
                pruneFailingLines(packageName, testPackageName, espressoPackageName,
                        rootProjectFolderPath, applicationFolderPath, buildVariant, outputFolderPath,
                        espressoTestCases.get(i));
            }

            // write pruned test cases
            writeTestCases(outputFolderPath, espressoTestCases);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String getApplicationFolderPath(String rootProjectFolderPath) throws Exception {
        String grepCmd = String.format("grep -l -R \"apply plugin: 'com.android.application'\" %s", rootProjectFolderPath);
        String[] grepResult = ProcessRunner.runCommand(grepCmd).split("\n");
        if (grepResult.length != 1) {
            throw new Exception("Unable to find application path inside project.");
        }
        return new File(grepResult[0]).getParent() + File.separator;
    }

    private static String getEspressoPackageName(String rootProjectFolderPath) throws Exception {
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

    private static EspressoTestCase pruneFailingLines(String packageName, String testPackageName,
                                                      String espressoPackageName, String rootProjectFolderPath,
                                                      String applicationFolderPath, String buildVariant,
                                                      String outputFolderPath, EspressoTestCase espressoTestCase) throws Exception {
        // Preform fixed-point removal of failing performs in the test case

        ArrayList<Integer> failingPerformLines;
        ArrayList<Integer> newFailingPerformLines = new ArrayList<>();
        do {
            failingPerformLines = new ArrayList<>(newFailingPerformLines);
            newFailingPerformLines = runTestCase(packageName, testPackageName, espressoPackageName,
                    rootProjectFolderPath, applicationFolderPath, buildVariant, outputFolderPath, espressoTestCase);

            if (newFailingPerformLines.size() > 0) {
                espressoTestCase.removePerformsByNumber(newFailingPerformLines);
            }

            writeTestCase(outputFolderPath, espressoTestCase);

        } while (!failingPerformLines.equals(newFailingPerformLines) && newFailingPerformLines.size() > 0);

        return espressoTestCase;
    }

    private static ArrayList<Integer> runTestCase(String packageName, String testPackageName,
                                                  String espressoPackageName, String rootProjectFolderPath,
                                                  String applicationFolderPath, String buildVariant,
                                                  String outputFolderPath, EspressoTestCase espressoTestCase) throws Exception {
        ArrayList<Integer> failingPerforms = new ArrayList<>();

        // delete previously built APKs
        String rmCmd = String.format("find %s -name *.apk -delete", applicationFolderPath);
        ProcessRunner.runCommand(rmCmd);

        // compile tests
        String compileCmd = String.format("%sgradlew -p %s assembleAndroidTest",
                rootProjectFolderPath, rootProjectFolderPath);
        String compileResult = ProcessRunner.runCommand(compileCmd);
        if (!compileResult.contains("BUILD SUCCESSFUL")) {
            throw new Exception("Unable to compile Espresso Tests:\n" + compileResult);
        }

        // find where is androidTest apk
        String findApkCmd = String.format("find %s -name *androidTest.apk", applicationFolderPath);
        String findApkResult = ProcessRunner.runCommand(findApkCmd);
        if (findApkResult.contains("No such file or directory")) {
            throw new Exception("Unable to find compiled Espresso Tests");
        }

        String[] apks = findApkResult.split("\n");
        List<String> filteredApks = new ArrayList<>();
        for (String apk : apks) {
            if (apk.contains(buildVariant)) {
                filteredApks.add(apk);
            }
        }

        if (filteredApks.size() != 1) {
            throw new Exception("Unable to find compiled Espresso Tests");
        }
        String apkTestPath = filteredApks.get(0);

        // install apk test
        String installCmd = String.format("adb install %s", apkTestPath);
        ProcessRunner.runCommand(installCmd);

        String clearCmd = String.format("adb shell pm clear %s", packageName);
        ProcessRunner.runCommand(clearCmd);

        String junitRunner = "";
        if (espressoPackageName.contains("androidx")) {
            junitRunner = "androidx.test.runner.AndroidJUnitRunner";
        } else {
            junitRunner = "android.support.test.runner.AndroidJUnitRunner";
        }

        String instrumentCmd = String.format("adb shell am instrument -w -r -e emma true -e debug false -e class " +
                        "%s.%s %s.test/%s",
                testPackageName, espressoTestCase.getTestName(), packageName, junitRunner);
        String testResult = ProcessRunner.runCommand(instrumentCmd);

        if (!testResult.contains("OK")) {
            System.out.println("There was an error running test case: " + espressoTestCase.getTestName());
            System.out.println(testResult);
            return failingPerforms;
        }

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

        return failingPerforms;
    }

    private static void prepareTestRun(String rootProjectFolderPath) throws Exception {
        // disable animations on emulator
        ProcessRunner.runCommand("adb shell settings put global window_animation_scale 0");
        ProcessRunner.runCommand("adb shell settings put global transition_animation_scale 0");
        ProcessRunner.runCommand("adb shell settings put global animator_duration_scale 0");
    }

    private static void writeTestCases(String outputFolderPath, List<EspressoTestCase> espressoTestCases) throws FileNotFoundException {
        for (int i = 0; i < espressoTestCases.size(); i++) {
            writeTestCase(outputFolderPath, espressoTestCases.get(i));
        }
    }

    private static void writeTestCase(String outputFolderPath, EspressoTestCase espressoTestCase) throws FileNotFoundException {
        String testContent = espressoTestCase.toString();
        String outputFilePath = outputFolderPath + espressoTestCase.getTestName() + ".java";

        PrintWriter out = new PrintWriter(new FileOutputStream(outputFilePath), true);
        out.print(testContent);
        out.close();
    }

    private static List<WidgetTestCase> parseTestCases(String filePath) throws IOException {
        String content = readFile(filePath, StandardCharsets.UTF_8);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(content);

        return TestCaseParser.parseList(mapper, jsonNode);
    }

    private static String readFile(String path, Charset encoding) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }
}
