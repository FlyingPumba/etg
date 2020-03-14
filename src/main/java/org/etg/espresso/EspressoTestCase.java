package org.etg.espresso;

import org.apache.velocity.VelocityContext;
import org.etg.ETGProperties;
import org.etg.espresso.codegen.TestCodeMapper;
import org.etg.espresso.templates.VelocityTemplate;
import org.etg.espresso.templates.VelocityTemplateConverter;
import org.etg.mate.models.Action;
import org.etg.mate.models.WidgetTestCase;
import org.etg.utils.ProcessRunner;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class EspressoTestCase {

    private final String packageName;
    private final String testPackageName;
    private ETGProperties etgProperties;
    private WidgetTestCase widgetTestCase;
    private String testCaseName;
    private String espressoPackageName;

    private TestCodeMapper codeMapper;
    private VelocityTemplate testCaseTemplate;

    private HashMap<Integer, List<String>> testCodeLinesPerWidgetActionIndex;
    private HashMap<Integer, Integer> widgetActionIndexPerTryCatchNumber;
    private List<Action> widgetActions;
    private int lowestFailingWidgetActionIndex;


    public EspressoTestCase(ETGProperties properties,
                            WidgetTestCase widgetTestCase,
                            String testCaseName,
                            VelocityTemplate testCaseTemplate) throws Exception {
        this.etgProperties = properties;
        this.packageName = properties.getPackageName();
        this.testPackageName = properties.getTestPackageName();
        this.widgetTestCase = widgetTestCase;
        this.testCaseName = testCaseName;
        this.espressoPackageName = properties.getEspressoPackageName();

        this.testCaseTemplate = testCaseTemplate;

        this.testCodeLinesPerWidgetActionIndex = new HashMap<Integer, List<String>>();
        this.widgetActionIndexPerTryCatchNumber = new HashMap<Integer, Integer>();
        this.widgetActions = widgetTestCase.getEventSequence();
        this.lowestFailingWidgetActionIndex = this.widgetActions.size();

        generateTestCodeLines(true);
    }

    /**
     * Perform fixed-point removal of failing performs in the test case
     *
     * @param properties
     * @throws Exception
     */
    public void pruneFailingPerforms(ETGProperties properties) throws Exception {
        List<Integer> failingPerformLines;
        List<Integer> newFailingPerformLines = new ArrayList<>();
        do {
            failingPerformLines = new ArrayList<>(newFailingPerformLines);
            addToProject(properties, false);
            newFailingPerformLines = EspressoTestRunner.runTestCase(properties, this, false);

            if (newFailingPerformLines.size() > 0) {
                Integer tryCatchIndex = newFailingPerformLines.get(0);
                this.lowestFailingWidgetActionIndex = widgetActionIndexPerTryCatchNumber.get(tryCatchIndex);

                System.out.println(String.format("Lowest failing widget action index is %d (out of %d) TEST %s",
                        lowestFailingWidgetActionIndex, widgetActions.size(), getTestName()));
            }
        } while (!failingPerformLines.equals(newFailingPerformLines) && newFailingPerformLines.size() > 0);
    }

    public void addToProject(ETGProperties properties, boolean prettify) throws Exception {
        writeToFolder(properties.getOutputPath(), prettify);
        writeCustomUsedClasses(properties.getOutputPath());
    }

    private void writeToFolder(String outputFolderPath, boolean prettify) throws Exception {
        VelocityTemplateConverter templateConverter = new VelocityTemplateConverter(createVelocityContext());
        String testContent = templateConverter.applyContextToTemplate(testCaseTemplate);
        String outputFilePath = outputFolderPath + getTestName() + ".java";

        PrintWriter out = new PrintWriter(new FileOutputStream(outputFilePath), true);
        out.print(testContent);
        out.close();

        if (prettify) {
            // TODO: this is a hack, there has to be a better way
            String workingFolder = System.getProperty("user.dir");
            String googleBinPath = "bin/google-java-format-1.7-all-deps.jar";
            if (!workingFolder.endsWith("etg")) {
                googleBinPath = "etg/" + googleBinPath;
            }

            // run Google Java formatter on the output file
            String formatCmd = String.format("java -jar %s -i %s", googleBinPath, outputFilePath);
            ProcessRunner.runCommand(formatCmd);
        }
    }

    private void writeCustomUsedClasses(String outputFolderPath) throws Exception {
        VelocityTemplateConverter templateConverter = new VelocityTemplateConverter(createVelocityContext());
        for (VelocityTemplate vTemplate : codeMapper.getNeededTemplates()) {
            String classContent = templateConverter.applyContextToTemplate(vTemplate);
            String outputFilePath = outputFolderPath + vTemplate.getName();

            PrintWriter out = new PrintWriter(new FileOutputStream(outputFilePath), true);
            out.print(classContent);
            out.close();
        }
    }

    private VelocityContext createVelocityContext() throws Exception {
        VelocityContext velocityContext = new VelocityContext();

        Object[] visitedActivities = widgetTestCase.getVisitedActivities().toArray();
        if (visitedActivities.length == 0) {
            throw new Exception("No valid activities found in widget test case");
        }
        if ("unknown".equals(visitedActivities[0].toString())|| !visitedActivities[0].toString().contains("/")) {
            throw new Exception(String.format("No valid activity found in widget test case: %s", visitedActivities[0].toString()));
        }

        String activityName = visitedActivities[0].toString().split("/")[1];
        if (activityName.startsWith(packageName)) {
            velocityContext.put("TestActivityName", activityName);
        } else {
            velocityContext.put("TestActivityName", packageName + activityName);
        }

        velocityContext.put("PackageName", testPackageName);
        velocityContext.put("ResourcePackageName", packageName);

        // TODO: improve test name based on TestCase's visitedActivities
        velocityContext.put("ClassName", testCaseName);
        velocityContext.put("TestMethodName", "myTestCase");

        velocityContext.put("EspressoPackageName", espressoPackageName);

        velocityContext.put("AddContribImport", codeMapper.isRecyclerViewActionAdded());
        velocityContext.put("AddChildAtPositionMethod", codeMapper.isChildAtPositionAdded());
        velocityContext.put("AddClassOrSuperClassesNameMethod", codeMapper.isClassOrSuperClassesNameAdded());
        velocityContext.put("AddTryCatchImport", codeMapper.isTryCatchAdded());

        velocityContext.put("TestCode", getTestCodeLines());

        velocityContext.put("longClickActionAdded", codeMapper.isLongClickActionAdded());
        velocityContext.put("clickActionAdded", codeMapper.isClickActionAdded());
        velocityContext.put("swipeActionAdded", codeMapper.isSwipeActionAdded());


        return velocityContext;
    }

    private void generateTestCodeLines(boolean addTryCatchs) throws Exception {
        this.codeMapper = new TestCodeMapper(this.etgProperties);
        codeMapper.setSurroundPerformsWithTryCatch(addTryCatchs);
        testCodeLinesPerWidgetActionIndex.clear();

        for (int i = 0; i < widgetActions.size(); i++) {
            Action action = widgetActions.get(i);
            List<String> testCodeLinesForAction = new ArrayList<>();

            int tryCatchIndex = codeMapper.addTestCodeLinesForAction(action, testCodeLinesForAction) - 1;

            testCodeLinesPerWidgetActionIndex.put(i, testCodeLinesForAction);

            if (!widgetActionIndexPerTryCatchNumber.containsKey(tryCatchIndex)) {
                widgetActionIndexPerTryCatchNumber.put(tryCatchIndex, i);
            }
        }
    }

    public List<String> getTestCodeLines() {
        List<String> testCodeLines = new ArrayList<>();
        for (int i = 0; i < widgetActions.size(); i++) {
            List<String> testCodeLinesForAction = testCodeLinesPerWidgetActionIndex.get(i);

            if (i >= lowestFailingWidgetActionIndex) {
                // comment out failing lines
                for (int j = 0; j < testCodeLinesForAction.size(); j++) {
                    String testCodeLine = testCodeLinesForAction.get(j);
                    String[] lines = testCodeLine.split("\n");
                    for (String line: lines) {
                        testCodeLines.add("// " + line);
                    }
                }
            } else {
                testCodeLines.addAll(testCodeLinesForAction);
            }
        }

        return testCodeLines;
    }

    public String getTestName() {
        return testCaseName;
    }

    public double getCoverage(ETGProperties properties) throws Exception {
        // get root permissions for adb
        ProcessRunner.runCommand("adb root");

        // delete coverage.ec file remotely
        String rmCmd = String.format("adb shell rm %s", Coverage.getRemoteCoverageEcPath(properties));
        String rmCmdResult = ProcessRunner.runCommand(rmCmd);

        // run test case with coverage enabled to create coverage.ec file
        EspressoTestRunner.runTestCase(properties, this, true);

        // fetch and process the newly created file
        return Coverage.getTestCoverage(properties, this);
    }
}
