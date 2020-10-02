package org.etg.espresso;

import org.etg.ETGProperties;
import org.etg.espresso.codegen.codeMapper.CodeMapperType;
import org.etg.espresso.codegen.codeMapper.RobotPatternTestCodeMapper;
import org.etg.espresso.codegen.codeMapper.StandardTestCodeMapper;
import org.etg.espresso.codegen.codeMapper.TestCodeMapper;
import org.etg.espresso.templates.VelocityTemplate;
import org.etg.mate.models.Action;
import org.etg.mate.models.WidgetTestCase;

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
        if (etgProperties.getCodeMapper() == CodeMapperType.Standard) {
            this.codeMapper = new StandardTestCodeMapper(this.etgProperties);
        } else {
            this.codeMapper = new RobotPatternTestCodeMapper(this.etgProperties);
        }

        testCodeLinesPerWidgetActionIndex.clear();

        for (int i = 0; i < widgetActions.size(); i++) {
            List<String> testCodeLinesForAction = new ArrayList<>();

            codeMapper.addTestCodeLinesForAction(i, widgetActions, testCodeLinesForAction);

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
