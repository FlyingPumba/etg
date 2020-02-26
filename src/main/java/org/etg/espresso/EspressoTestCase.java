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
import java.util.List;
import java.util.Vector;

public class EspressoTestCase {

    private final String packageName;
    private final String testPackageName;
    private WidgetTestCase widgetTestCase;
    private String testCaseName;
    private String espressoPackageName;
    private final TestCodeMapper codeMapper;
    private List<String> testCodeLines;
    private VelocityTemplate testCaseTemplate;


    public EspressoTestCase(ETGProperties properties,
                            WidgetTestCase widgetTestCase,
                            String testCaseName,
                            VelocityTemplate testCaseTemplate) throws Exception {
        this.packageName = properties.getPackageName();
        this.testPackageName = properties.getTestPackageName();
        this.widgetTestCase = widgetTestCase;
        this.testCaseName = testCaseName;
        this.espressoPackageName = properties.getEspressoPackageName();
        this.testCaseTemplate = testCaseTemplate;
        codeMapper = new TestCodeMapper(properties);
        testCodeLines = new ArrayList<>();

        generateTestCodeLines(true);
    }

    /**
     * Perform fixed-point removal of failing performs in the test case
     *
     * @param properties
     * @throws Exception
     */
    public void pruneFailingPerforms(ETGProperties properties) throws Exception {
        ArrayList<Integer> failingPerformLines;
        ArrayList<Integer> newFailingPerformLines = new ArrayList<>();
        do {
            failingPerformLines = new ArrayList<>(newFailingPerformLines);
            addToProject(properties, false);
            newFailingPerformLines = EspressoTestRunner.runTestCase(properties, this);

            if (newFailingPerformLines.size() > 0) {
                System.out.println(String.format("Removing %d/%d failing lines from TEST %s",
                        newFailingPerformLines.size(), testCodeLines.size(), getTestName()));
                removePerformsByNumber(newFailingPerformLines);
            }
        } while (!failingPerformLines.equals(newFailingPerformLines) && newFailingPerformLines.size() > 0);
    }

    private void generateTestCodeLines(boolean addTryCatchs) {
        codeMapper.setSurroundPerformsWithTryCatch(addTryCatchs);

        testCodeLines.clear();
        Vector<Action> actions = widgetTestCase.getEventSequence();
        for (Action action : actions) {
            codeMapper.addTestCodeLinesForAction(action, testCodeLines);
        }
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

        velocityContext.put("TestCode", testCodeLines);

        velocityContext.put("longClickActionAdded", codeMapper.isLongClickActionAdded());
        velocityContext.put("clickActionAdded", codeMapper.isClickActionAdded());
        velocityContext.put("swipeActionAdded", codeMapper.isSwipeActionAdded());


        return velocityContext;
    }

    public List<String> getTestCodeLines() {
        return testCodeLines;
    }

    public String getTestName() {
        return testCaseName;
    }

    public void removePerformsByNumber(ArrayList<Integer> lineNumbers) {
        List<String> newTestCodeLines = new ArrayList<>();
        int performNumber = 0;
        for (int i = 0; i < testCodeLines.size();) {
            String statement = testCodeLines.get(i);

            if (statement.contains("onView")) {
                // not a perform, but check if it's related to next one
                if (i+1 < testCodeLines.size()) {

                    String variableName = statement.split("ViewInteraction ")[1].split(" = ")[0];
                    String nextStatement = testCodeLines.get(i + 1);

                    if (!nextStatement.contains("onView") && nextStatement.contains(variableName)
                            && lineNumbers.contains(performNumber)) {
                        // both this and next statement are related, and the next statement is a perform that should be removed
                        // skip both and increase perform number
                        i++;
                        performNumber++;

                    } else {
                        newTestCodeLines.add(statement);
                    }

                } else {
                    newTestCodeLines.add(statement);
                }

            } else {
                if (!lineNumbers.contains(performNumber)) {
                    newTestCodeLines.add(statement);
                }

                performNumber++;
            }

            i++;
        }
        testCodeLines = newTestCodeLines;
    }

    public double getCoverage(ETGProperties properties) throws Exception {
        return Coverage.getTestCoverage(properties, this);
    }
}
