package org.etg;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.etg.espresso.EspressoTestCase;
import org.etg.espresso.codegen.TestCodeGenerator;
import org.etg.mate.models.WidgetTestCase;
import org.etg.mate.parser.TestCaseParser;
import org.etg.utils.ProcessRunner;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ETG {
    public static void main(String[] args) {
        System.out.println("ETG");

        try {
            ETGProperties properties = ETGProperties.loadProperties(args[0]);
            System.out.println("Working on file with path: " + properties.getJsonPath() + " and package name: " + properties.getPackageName());

            List<WidgetTestCase> widgetTestCases = parseTestCases(properties.getJsonPath());

            TestCodeGenerator codeGenerator = new TestCodeGenerator(properties);
            List<EspressoTestCase> espressoTestCases = codeGenerator.getEspressoTestCases(widgetTestCases);

            // prune failing lines from each test case
            writeTestCases(properties.getOutputPath(), espressoTestCases);
            prepareTestRun();

            for (int i = 0; i < espressoTestCases.size(); i++) {
                pruneFailingLines(properties, espressoTestCases.get(i));
            }

            // write pruned test cases
            writeTestCases(properties.getOutputPath(), espressoTestCases);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static EspressoTestCase pruneFailingLines(ETGProperties properties, EspressoTestCase espressoTestCase) throws Exception {
        // Preform fixed-point removal of failing performs in the test case

        ArrayList<Integer> failingPerformLines;
        ArrayList<Integer> newFailingPerformLines = new ArrayList<>();
        do {
            failingPerformLines = new ArrayList<>(newFailingPerformLines);
            newFailingPerformLines = runTestCase(properties, espressoTestCase);

            if (newFailingPerformLines.size() > 0) {
                espressoTestCase.removePerformsByNumber(newFailingPerformLines);
            }

            writeTestCase(properties.getOutputPath(), espressoTestCase);

        } while (!failingPerformLines.equals(newFailingPerformLines) && newFailingPerformLines.size() > 0);

        return espressoTestCase;
    }

    private static ArrayList<Integer> runTestCase(ETGProperties properties, EspressoTestCase espressoTestCase) throws Exception {
        ArrayList<Integer> failingPerforms = new ArrayList<>();

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

        // find where is androidTest apk
        String findApkCmd = String.format("find %s -name *androidTest.apk", properties.getApplicationFolderPath());
        String findApkResult = ProcessRunner.runCommand(findApkCmd);
        if (findApkResult.contains("No such file or directory")) {
            throw new Exception("Unable to find compiled Espresso Tests");
        }

        String[] apks = findApkResult.split("\n");
        List<String> filteredApks = new ArrayList<>();
        for (String apk : apks) {
            if (apk.contains(properties.getBuildVariant())) {
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

        String clearCmd = String.format("adb shell pm clear %s", properties.getPackageName());
        ProcessRunner.runCommand(clearCmd);

        String junitRunner = "";
        if (properties.getEspressoPackageName().contains("androidx")) {
            junitRunner = "androidx.test.runner.AndroidJUnitRunner";
        } else {
            junitRunner = "android.support.test.runner.AndroidJUnitRunner";
        }

        String instrumentCmd = String.format("adb shell am instrument -w -r -e emma true -e debug false -e class " +
                        "%s.%s %s.test/%s",
                properties.getTestPackageName(), espressoTestCase.getTestName(), properties.getPackageName(), junitRunner);
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

    private static void prepareTestRun() {
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
