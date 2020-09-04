package org.etg.espresso;

import org.etg.ETGProperties;
import org.etg.espresso.codegen.TestCodeMapper;
import org.etg.espresso.templates.VelocityTemplate;
import org.etg.mate.models.Action;
import org.etg.mate.models.ActionType;
import org.etg.mate.models.WidgetTestCase;
import org.etg.utils.ProcessRunner;

import java.util.*;

public class EspressoTestCase {

    private ETGProperties etgProperties;
    private WidgetTestCase widgetTestCase;
    private String testCaseName;

    private TestCodeMapper codeMapper;
    private VelocityTemplate testCaseTemplate;

    private HashMap<Integer, List<String>> testCodeLinesPerWidgetActionIndex;
    private List<Action> widgetActions;
    private Set<Integer> failingWidgetActionIndexes;

    public EspressoTestCase(ETGProperties properties,
                            WidgetTestCase widgetTestCase,
                            String testCaseName,
                            VelocityTemplate testCaseTemplate) throws Exception {
        this.etgProperties = properties;
        this.widgetTestCase = widgetTestCase;
        this.testCaseName = testCaseName;

        this.testCaseTemplate = testCaseTemplate;

        this.testCodeLinesPerWidgetActionIndex = new HashMap<Integer, List<String>>();
        this.widgetActions = widgetTestCase.getEventSequence();
        this.failingWidgetActionIndexes = new HashSet<>();

        generateTestCodeLines();
    }

    private void generateTestCodeLines() throws Exception {
        this.codeMapper = new TestCodeMapper(this.etgProperties);
        testCodeLinesPerWidgetActionIndex.clear();

        for (int i = 0; i < widgetActions.size(); i++) {
            Action action = widgetActions.get(i);
            List<String> testCodeLinesForAction = new ArrayList<>();

            if (i == 0 && etgProperties.getSleepAfterLaunch() != -1) {
                Action waitAfterLaunch = new Action(ActionType.WAIT);
                waitAfterLaunch.setTimeToWait(etgProperties.getSleepAfterLaunch());
                codeMapper.addTestCodeLinesForAction(waitAfterLaunch, testCodeLinesForAction, i , widgetActions.size());
            }

            codeMapper.addTestCodeLinesForAction(action, testCodeLinesForAction, i , widgetActions.size());

            if (etgProperties.getSleepAfterActions() != -1) {
                Action waitAfterAction = new Action(ActionType.WAIT);
                waitAfterAction.setTimeToWait(etgProperties.getSleepAfterActions());
                codeMapper.addTestCodeLinesForAction(waitAfterAction, testCodeLinesForAction, i , widgetActions.size());
            }

            testCodeLinesPerWidgetActionIndex.put(i, testCodeLinesForAction);
        }
    }

    public String getTestName() {
        return testCaseName;
    }

    public Set<Integer> getFailingWidgetActionIndexes() {
        return failingWidgetActionIndexes;
    }

    public void addFailingWidgetActionIndex(int index) {
        failingWidgetActionIndexes.add(index);
    }

    public int getWidgetActionsCount(){
        return widgetActions.size();
    }

    public List<Action> getWidgetActions(){
        return widgetActions;
    }

    public int getLowestFailingWidgetActionIndex() {
        int min = widgetActions.size();
        for (int index: failingWidgetActionIndexes) {
            if (index < min) {
                min = index;
            }
        }
        return min;
    }

    public List<String> getTestCodeLinesForWidgetActionIndex(int index) {
        return testCodeLinesPerWidgetActionIndex.get(index);
    }

    public ETGProperties getEtgProperties() {
        return etgProperties;
    }

    public VelocityTemplate getTestCaseTemplate() {
        return testCaseTemplate;
    }

    public TestCodeMapper getCodeMapper() {
        return codeMapper;
    }

    public String getTestCaseResultsPath() {
        return String.format("%s/%s", etgProperties.getETGResultsPath(), getTestName());
    }

    public String getFullTestName() {
        return String.format("%s.%s",
                getEtgProperties().getTestPackageName(),
                getTestName());
    }
}
