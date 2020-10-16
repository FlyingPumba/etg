package org.etg.espresso;

import org.etg.ETGProperties;
import org.etg.espresso.codegen.codeMapper.CodeMapperType;
import org.etg.espresso.codegen.codeMapper.RobotPatternTestCodeMapper;
import org.etg.espresso.codegen.codeMapper.StandardTestCodeMapper;
import org.etg.espresso.codegen.codeMapper.TestCodeMapper;
import org.etg.espresso.templates.VelocityTemplate;
import org.etg.espresso.templates.kotlin.CuidarSetupCodeTemplate;
import org.etg.mate.models.Action;
import org.etg.mate.models.ActionType;
import org.etg.mate.models.WidgetTestCase;

import java.util.*;

public class EspressoTestCase {

    private ETGProperties etgProperties;
    private WidgetTestCase widgetTestCase;
    private String testCaseName;

    private TestCodeMapper codeMapper;
    private VelocityTemplate testCaseTemplate;

    private HashMap<Integer, List<String>> testCodeLinesPerWidgetActionIndex;
    private Set<Integer> failingWidgetActionIndexes;

    private List<Action> testCodeWidgetActions;
    private List<Action> setupCodeWidgetActions;

    public EspressoTestCase(ETGProperties properties,
                            WidgetTestCase widgetTestCase,
                            String testCaseName,
                            VelocityTemplate testCaseTemplate) throws Exception {
        this.etgProperties = properties;
        this.widgetTestCase = widgetTestCase;
        this.testCaseName = testCaseName;
        this.testCaseTemplate = testCaseTemplate;

        this.testCodeLinesPerWidgetActionIndex = new HashMap<Integer, List<String>>();
        this.failingWidgetActionIndexes = new HashSet<>();

        parseWidgetActions();
        generateTestCodeLines();
    }

    private void parseWidgetActions() {
        Vector<Action> actions = widgetTestCase.getEventSequence();

        int restartActionIndex = -1;
        for (int i = 0, actionsSize = actions.size(); i < actionsSize; i++) {
            Action action = actions.get(i);
            if (action.getActionType() == ActionType.RESTART) {
                restartActionIndex = i;
            }
        }

        if (restartActionIndex == -1) {
            // there is not setup actions
            this.testCodeWidgetActions = actions;
            this.setupCodeWidgetActions = new ArrayList<>();
        } else {
            // the actions before and including the RESTART action are setup code
            this.testCodeWidgetActions = actions.subList(restartActionIndex + 1, actions.size());
            this.setupCodeWidgetActions = actions.subList(0, restartActionIndex + 1);
        }
    }

    private void generateTestCodeLines() throws Exception {
        if (etgProperties.getCodeMapper() == CodeMapperType.Standard) {
            this.codeMapper = new StandardTestCodeMapper(this.etgProperties);
        } else {
            this.codeMapper = new RobotPatternTestCodeMapper(this.etgProperties);
        }

        testCodeLinesPerWidgetActionIndex.clear();

        for (int i = 0; i < testCodeWidgetActions.size(); i++) {
            List<String> testCodeLinesForAction = new ArrayList<>();

            codeMapper.addTestCodeLinesForAction(i, testCodeWidgetActions, testCodeLinesForAction);

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
        return testCodeWidgetActions.size();
    }

    public List<Action> getTestCodeWidgetActions(){
        return testCodeWidgetActions;
    }

    public int getLowestFailingWidgetActionIndex() {
        int min = testCodeWidgetActions.size();
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

    public String getSetupLoggedUserCode() throws Exception {
        if (setupCodeWidgetActions.isEmpty()) {
            return "";
        }

        CuidarSetupCodeTemplate setupCodeTemplate = new CuidarSetupCodeTemplate(setupCodeWidgetActions, etgProperties);
        codeMapper.addExtraImports(setupCodeTemplate.getExtraImports());
        return setupCodeTemplate.getAsString();
    }
}
