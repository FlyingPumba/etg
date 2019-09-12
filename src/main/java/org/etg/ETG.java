package org.etg;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.etg.espresso.codegen.TestCodeGenerator;
import org.etg.mate.models.TestCase;
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
import java.util.List;

public class ETG {
    public static void main(String[] args) {
        System.out.println("ETG");

        String filePath = args[0];
        String packageName = args[1];
        String testPackageName = args[2];
        String rootProjectFolderPath = args[3];
        String outputFolderPath = args[4];

        System.out.println("Working on file with path: " + filePath + " and package name: " + packageName);

        try {
            List<TestCase> testCases = parseTestCases(filePath);

            TestCodeGenerator codeGenerator = new TestCodeGenerator(packageName, testPackageName);
            List<String> espressoTestCases = codeGenerator.getEspressoTestCases(testCases);

            writeTestCases(outputFolderPath, espressoTestCases);

            // runTestCases(packageName, testPackageName, rootProjectFolderPath, outputFolderPath, espressoTestCases);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void runTestCases(String packageName, String testPackageName, String rootProjectFolderPath,
                                     String outputFolderPath, List<String> espressoTestCases) throws Exception {
        // compile tests
        String compileCmd = String.format("%sgradlew -p %s assembleAndroidTest",
                rootProjectFolderPath, rootProjectFolderPath);
        String compileResult = ProcessRunner.runCommand(compileCmd);
        if (!compileResult.contains("BUILD SUCCESSFUL")) {
            throw new Exception("Unable to compile Espresso Tests");
        }

        // disable animations on emulator
        ProcessRunner.runCommand("adb shell settings put global window_animation_scale 0");
        ProcessRunner.runCommand("adb shell settings put global transition_animation_scale 0");
        ProcessRunner.runCommand("adb shell settings put global animator_duration_scale 0");

        // find where is androidTest apk
        String findApkCmd = String.format("find %sapp/build/outputs/apk/androidTest/ -name *androidTest.apk",
                rootProjectFolderPath);
        String[] apks = ProcessRunner.runCommand(findApkCmd).split("\n");
        if (apks.length == 0) {
            throw new Exception("Unable to find compiled Espresso Tests");
        }
        String apkTestPath = apks[0];

        // install apk test
        String installCmd = String.format("adb install %s", apkTestPath);
        ProcessRunner.runCommand(installCmd);

        // run each test case
        for (int i = 0; i < espressoTestCases.size(); i++) {
            String testCaseName = "TestCase" + i;

            String clearCmd = String.format("shell pm clear %s", packageName);
            ProcessRunner.runCommand(clearCmd);

            String instrumentCmd = String.format("adb shell am instrument -w -r -e emma true -e debug false -e class " +
                    "%s.%s %s/android.support.test.runner.AndroidJUnitRunner",
                    testPackageName, testCaseName, testPackageName);
            String testResult = ProcessRunner.runCommand(instrumentCmd);

            if (!testResult.contains("OK")) {
                System.out.println("There was an error running test case: " + testCaseName);
                System.out.println(testResult);
            }
        }
    }

    private static void writeTestCases(String outputFolderPath, List<String> espressoTestCases) throws FileNotFoundException {
        for (int i = 0; i < espressoTestCases.size(); i++) {
            String testContent = espressoTestCases.get(i);
            String outputFilePath = outputFolderPath + "TestCase" + i + ".java";

            PrintWriter out = new PrintWriter(new FileOutputStream(outputFilePath), true);
            out.print(testContent);
            out.close();
        }
    }

    private static List<TestCase> parseTestCases(String filePath) throws IOException {
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
