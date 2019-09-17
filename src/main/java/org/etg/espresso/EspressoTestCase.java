package org.etg.espresso;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.etg.espresso.codegen.TestCodeMapper;
import org.etg.mate.models.Action;
import org.etg.mate.models.WidgetTestCase;

import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class EspressoTestCase {

    private final String ESPRESSO_CUSTOM_PACKAGE = "com.google.android.apps.common.testing.ui";
    private final String ESPRESSO_STANDARD_PACKAGE = "android.support.test";

    private final String packageName;
    private final String testPackageName;
    private WidgetTestCase widgetTestCase;
    private String testCaseName;
    private final TestCodeMapper codeMapper;
    private List<String> testCodeLines;

    public EspressoTestCase(String packageName, String testPackageName,
                            WidgetTestCase widgetTestCase, String testCaseName) {
        this.packageName = packageName;
        this.testPackageName = testPackageName;
        this.widgetTestCase = widgetTestCase;
        this.testCaseName = testCaseName;

        codeMapper = new TestCodeMapper();
        testCodeLines = new ArrayList<>();
        Vector<Action> actions = widgetTestCase.getEventSequence();
        for (Action action : actions) {
            codeMapper.addTestCodeLinesForAction(action, testCodeLines);
        }
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
        String[] activityName = visitedActivities[0].toString().split("/");
        velocityContext.put("TestActivityName", packageName + activityName[1]);

        velocityContext.put("PackageName", testPackageName);
        velocityContext.put("ResourcePackageName", packageName);

        // TODO: improve test name based on TestCase's visitedActivities
        velocityContext.put("ClassName", testCaseName);
        velocityContext.put("TestMethodName", "myTestCase");

        velocityContext.put("EspressoPackageName", false ? ESPRESSO_CUSTOM_PACKAGE : ESPRESSO_STANDARD_PACKAGE);

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
        for (int i = 0; i < testCodeLines.size(); i++) {
            String statement = testCodeLines.get(i);

            if (statement.contains("onView")) {
                // not a perform
                newTestCodeLines.add(statement);
            } else {
                if (!lineNumbers.contains(performNumber)) {
                    newTestCodeLines.add(statement);
                }

                performNumber++;
            }
        }
        testCodeLines = newTestCodeLines;
    }
}
