package org.etg.espresso;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.etg.ETGProperties;
import org.etg.espresso.codegen.TestCodeMapper;
import org.etg.mate.models.Action;
import org.etg.mate.models.WidgetTestCase;

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

    public EspressoTestCase(String packageName, String testPackageName, String espressoPackageName,
                            WidgetTestCase widgetTestCase, String testCaseName) {
        this.packageName = packageName;
        this.testPackageName = testPackageName;
        this.widgetTestCase = widgetTestCase;
        this.testCaseName = testCaseName;
        this.espressoPackageName = espressoPackageName;

        codeMapper = new TestCodeMapper();
        testCodeLines = new ArrayList<>();
        Vector<Action> actions = widgetTestCase.getEventSequence();
        for (Action action : actions) {
            codeMapper.addTestCodeLinesForAction(action, testCodeLines);
        }
    }

    public void pruneFailingPerforms(ETGProperties properties) throws Exception {
        // Perform fixed-point removal of failing performs in the test case

        ArrayList<Integer> failingPerformLines;
        ArrayList<Integer> newFailingPerformLines = new ArrayList<>();
        do {
            failingPerformLines = new ArrayList<>(newFailingPerformLines);

            writeToFolder(properties.getOutputPath());
            newFailingPerformLines = EspressoTestRunner.runTestCase(properties, this);

            if (newFailingPerformLines.size() > 0) {
                removePerformsByNumber(newFailingPerformLines);
            }
        } while (!failingPerformLines.equals(newFailingPerformLines) && newFailingPerformLines.size() > 0);
    }

    private void writeToFolder(String outputFolderPath) throws FileNotFoundException {
        String testContent = toString();
        String outputFilePath = outputFolderPath + getTestName() + ".java";

        PrintWriter out = new PrintWriter(new FileOutputStream(outputFilePath), true);
        out.print(testContent);
        out.close();
    }

    @Override
    public String toString() {
        Writer writer = null;
        try {
            writer = new StringWriter();

            VelocityEngine velocityEngine = new VelocityEngine();
            // Suppress creation of velocity.log file.
            velocityEngine.setProperty(RuntimeConstants.RUNTIME_LOG_LOGSYSTEM_CLASS, "org.apache.velocity.runtime.log.NullLogChute");
            velocityEngine.init();
            VelocityContext velocityContext = createVelocityContext();
            velocityEngine.evaluate(velocityContext, writer, "mystring", TestCodeTemplate.getTemplate());
            writer.flush();

            return writer.toString();

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate test class file: ", e);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (Exception e) {
                    // ignore
                }
            }
        }
    }

    private VelocityContext createVelocityContext() {
        VelocityContext velocityContext = new VelocityContext();

        Object[] visitedActivities = widgetTestCase.getVisitedActivities().toArray();
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
}
