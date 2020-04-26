package org.etg.espresso;

import org.etg.ETGProperties;
import org.etg.espresso.codegen.TestCodeGenerator;
import org.etg.utils.ProcessRunner;

import java.io.File;
import java.util.*;

public class EspressoTestRunner {

    private static Map<String, TestResult> cachedResults = new HashMap<>();

    private List<String> testCaseNames = new ArrayList<>();
    private ETGProperties properties;

    private final String junitRunner;
    private boolean raiseExceptionOnFailedTest = false;
    private boolean coverageEnabled = false;
    private boolean pullCoverage = false;
    private String coverageFolder;

    private EspressoTestRunner(ETGProperties properties) throws Exception {
        this.properties = properties;
        this.junitRunner = getJunitRunner();
    }

    public static EspressoTestRunner forProject(ETGProperties properties) throws Exception {
        EspressoTestRunner espressoTestRunner = new EspressoTestRunner(properties);
        espressoTestRunner.addAllTestCasesInProject();
        return espressoTestRunner;
    }

    public static EspressoTestRunner forTestCase(EspressoTestCase espressoTestCase) throws Exception {
        EspressoTestRunner espressoTestRunner = new EspressoTestRunner(espressoTestCase.getEtgProperties());
        espressoTestRunner.addTestCase(espressoTestCase);
        return espressoTestRunner;
    }

    public static EspressoTestRunner forTestCases(EspressoTestCase... espressoTestCases) throws Exception {
        EspressoTestRunner espressoTestRunner = new EspressoTestRunner(espressoTestCases[0].getEtgProperties());
        for (EspressoTestCase espressoTestCase: espressoTestCases) {
            espressoTestRunner.addTestCase(espressoTestCase);
        }
        return espressoTestRunner;
    }

    public EspressoTestRunner addTestCase(EspressoTestCase espressoTestCase) {
        addTestCase(espressoTestCase.getFullTestName());
        return this;
    }

    public EspressoTestRunner addTestCase(String testCaseName) {
        testCaseNames.add(testCaseName);
        return this;
    }

    public EspressoTestRunner addAllTestCasesInProject() throws Exception {
        // before running am instrument to find out which tests do we have, install the androidTest apk on device.
        prepareDeviceBeforeAll();

        List<String> testCases = getAllTestCases(properties, junitRunner);
        testCaseNames.addAll(testCases);
        return this;
    }

    public EspressoTestRunner withRemoteCoverage() {
        this.coverageEnabled = true;
        return this;
    }

    public EspressoTestRunner withLocalCoverage(String coverageFolder) {
        this.coverageEnabled = true;
        this.pullCoverage = true;
        this.coverageFolder = coverageFolder;
        return this;
    }

    public EspressoTestRunner raiseExceptionOnFailedTest() {
        this.raiseExceptionOnFailedTest = true;
        return this;
    }

    public TestResult run() throws Exception {
        prepareDeviceBeforeAll();

        TestResult result = new TestResult();

        prepareDeviceBeforeEach();

        String testCaseName = testCaseNames.size() == 1 ? testCaseNames.get(0) : "all";
        String coverageFlags = buildCoverageFlags(testCaseName, result);

        String instrumentCmd = buildAmInstrumentCmd(coverageFlags, testCaseNames.toArray(new String[0]));
        String output = ProcessRunner.runCommand(instrumentCmd);
        result.setOutput(output);

        // Add some time sleep after executing test to avoid clogging the emulator
        sleepAfterTest();

        result.setFailingPerforms(parseFailingPerforms());

        pullCoverageIfNeeded(result);

        return result;
    }

    public List<TestResult> runSeparately() throws Exception {
        prepareDeviceBeforeAll();

        List<TestResult> results = new ArrayList<>();
        for (String testCaseName : testCaseNames) {
            TestResult cachedResult = fetchCachedResult(testCaseName);
            TestResult result;
            if (cachedResult != null) {
                result = cachedResult;
                copyCoverageFromCachedResultIfNeeded(result);

            } else {
                result = new TestResult();

                prepareDeviceBeforeEach();

                String coverageFlags = buildCoverageFlags(testCaseName, result);

                String instrumentCmd = buildAmInstrumentCmd(coverageFlags, testCaseName);
                String output = ProcessRunner.runCommand(instrumentCmd);
                result.setOutput(output);

                if (raiseExceptionOnFailedTest && !output.contains("OK")) {
                    System.out.println("There was an error running test case: " + testCaseName);
                    System.out.println(output);
                    throw new Exception("There was an error running test case: " + testCaseName + "\n"
                            + output);
                }

                // Add some time sleep after executing test to avoid clogging the emulator
                sleepAfterTest();

                result.setFailingPerforms(parseFailingPerforms());

                pullCoverageIfNeeded(result);

                cacheResultIfNeeded(testCaseName, result);
            }

            results.add(result);
        }


        return results;
    }

    private String buildAmInstrumentCmd(String coverageFlags, String... testCaseNames) {
        String cmdPrefix = "adb shell am instrument -w -r --no-window-animation -e debug false ";
        String classes = String.join(",", testCaseNames);

        return String.format("%s %s -e class %s %s/%s",
                cmdPrefix,
                coverageFlags,
                classes,
                properties.getCompiledTestPackageName(),
                junitRunner);
    }

    private String buildCoverageFlags(String testCaseName, TestResult testResult) {
        String coverageFlags = "";
        if (coverageEnabled) {
            String remoteCoverageFilePath = CoverageFetcher.getRemoteCoverageFilePathForTestName(properties, testCaseName);
            testResult.setCoverageFilePath(remoteCoverageFilePath);

            coverageFlags = String.format("-e coverage true -e coverageFile %s", remoteCoverageFilePath);
        }

        return coverageFlags;
    }

    private void prepareDeviceBeforeAll() throws Exception {
        disableAnimations();

        compileTests(properties);

        String apkTestPath = getAndroidTestApkPath(properties);
        uninstallPackage(properties.getCompiledTestPackageName());
        installApk(apkTestPath);
    }

    private void prepareDeviceBeforeEach() throws Exception {
        clearPackage(properties.getCompiledPackageName());
        clearPackage(properties.getCompiledTestPackageName());

        grantAllPermissions(properties.getCompiledPackageName());

        if (coverageEnabled) {
            ProcessRunner.runCommand(String.format("adb shell mkdir -p %s",
                    CoverageFetcher.getRemoteCoverageEcFolderPath(properties)));
        }
    }

    private void pullCoverageIfNeeded(TestResult result) throws Exception {
        if (coverageEnabled && pullCoverage) {
            if (result.getOutput().contains("Failed to generate Emma/JaCoCo coverage")) {
                // there is nothing to pull from device
                throw new Exception("Coverage information was requested but test did not generate it: " +
                        result.getOutput());
            }

            String coverageFileName = new File(result.getCoverageFilePath()).getName();
            String newLocation = String.format("%s/%s", coverageFolder, coverageFileName);

            // pull .ec file from device
            String pullCmd = String.format("adb pull %s %s",
                    result.getCoverageFilePath(),
                    newLocation);
            String pullCmdResult = ProcessRunner.runCommand(pullCmd);

            if (pullCmdResult.contains("error")) {
                throw new Exception("Unable to fetch " + result.getCoverageFilePath() + " file from device: " +
                        pullCmdResult);
            }

            result.setCoverageFilePath(newLocation);
        }
    }

    private void copyCoverageFromCachedResultIfNeeded(TestResult result) throws Exception {
        if (coverageEnabled && pullCoverage) {
            if (result.getOutput().contains("Failed to generate Emma/JaCoCo coverage")) {
                // there is nothing to pull from device
                throw new Exception("Coverage information was requested but test did not generate it: " +
                        result.getOutput());
            }

            String coverageFileName = new File(result.getCoverageFilePath()).getName();
            String newLocation = String.format("%s/%s", coverageFolder, coverageFileName);

            // copy file from one location to the other
            String cpCmd = String.format("cp %s %s",
                    result.getCoverageFilePath(),
                    newLocation);
            String cpCmdResult = ProcessRunner.runCommand(cpCmd);

            result.setCoverageFilePath(newLocation);
        }
    }

    private static void sleepAfterTest() {
        // Add some time sleep after executing test to avoid clogging the emulator
        try {
            System.out.println("Waiting 5 seconds after test");
            int seconds = 5;
            Thread.sleep(seconds * 1000);
        } catch (InterruptedException e) {
            // do nothing
        }
    }

    private static List<String> getAllTestCases(ETGProperties properties, String junitRunner) {
        String dryRunCmd = String.format("adb shell am instrument -w -r --no-window-animation -e log true -e debug false -e package " +
                        "%s %s/%s | grep -e \"test=\" -e \"class=\"",
                properties.getPackageName(),
                properties.getCompiledTestPackageName(),
                junitRunner);
        String output = ProcessRunner.runCommand(dryRunCmd);
        String[] lines = output.split("\n");

        Set<String> tests = new HashSet<>();
        for (int i = 0; i < lines.length; i = i + 2) {
            String clazz = lines[i].split("INSTRUMENTATION_STATUS: class=")[1];
            String test = lines[i+1].split("INSTRUMENTATION_STATUS: test=")[1];
            if ("null".equals(test)) {
                // This class has tests but was marked as @Ignore: skip it
            } else if (test.contains("Parameter(") || test.contains("[") || test.contains(" ")) {
                // This is a parameterized test, it suffices to use the class name to run them all together,
                // since there is no way to run them one by one.
                tests.add(clazz);
            } else {
                String fullTestName = String.format("%s#%s", clazz, test);
                tests.add(fullTestName);
            }
        }

        return new ArrayList<>(tests);
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

    private String getJunitRunner() throws Exception {
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

    private void clearPackage(String packageName) {
        String clearCmd = String.format("adb shell pm clear %s", packageName);
        ProcessRunner.runCommand(clearCmd);
    }

    private void grantAllPermissions(String packageName) {
        String permissionsCmd = "adb shell pm list permissions -d -g | grep permission: | cut -d':' -f2";
        String output = ProcessRunner.runCommand(permissionsCmd);
        for (String permission: output.split("\n")) {
            grantPermission(packageName, permission);
        }
    }

    private void grantPermission(String packageName, String permission) {
        String grantCmd = String.format("adb shell pm grant %s %s >/dev/null 2>&1", packageName, permission);
        ProcessRunner.runCommand(grantCmd);
    }

    private void installApk(String apkTestPath) {
        String installCmd = String.format("adb install %s", apkTestPath);
        ProcessRunner.runCommand(installCmd);
    }

    private void uninstallPackage(String packageName) {
        String uninstallCmd = String.format("adb uninstall %s", packageName);
        ProcessRunner.runCommand(uninstallCmd);
    }

    private String getAndroidTestApkPath(ETGProperties properties) throws Exception {
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

    private void compileTests(ETGProperties properties) throws Exception {
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

    private void disableAnimations() {
        // disable animations on emulator
        ProcessRunner.runCommand("adb shell settings put global window_animation_scale 0");
        ProcessRunner.runCommand("adb shell settings put global transition_animation_scale 0");
        ProcessRunner.runCommand("adb shell settings put global animator_duration_scale 0");
    }

    private void cacheResultIfNeeded(String testCaseName, TestResult result) {
        // Only cache results of project tests, since we are not modifying them
        if (!testCaseName.contains(TestCodeGenerator.getETGTestCaseNamePrefix()) &&
                !cachedResults.containsKey(testCaseName)) {
            cachedResults.put(testCaseName, result);
        }
    }

    private TestResult fetchCachedResult(String testCaseName) {
        if (cachedResults.containsKey(testCaseName)) {
            return cachedResults.get(testCaseName);
        }
        return null;
    }

    public class TestResult {

        private String output;
        private String coverageFilePath;
        private List<Integer> parseFailingPerforms;

        public TestResult(String output, String coverageFilePath) {
            this.output = output;
            this.coverageFilePath = coverageFilePath;
            this.parseFailingPerforms = new ArrayList<>();
        }

        public TestResult(String output) {
            this(output, null);
        }

        public TestResult() {
            this("", null);
        }

        public String getOutput() {
            return output;
        }

        public void setOutput(String output) {
            this.output = output;
        }

        public String getCoverageFilePath() {
            return coverageFilePath;
        }

        public void setCoverageFilePath(String coverageFilePath) {
            this.coverageFilePath = coverageFilePath;
        }

        public List<Integer> getParseFailingPerforms() {
            return parseFailingPerforms;
        }

        public void setFailingPerforms(List<Integer> parseFailingPerforms) {
            this.parseFailingPerforms = parseFailingPerforms;
        }
    }
}
