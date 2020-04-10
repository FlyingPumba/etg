package org.etg.espresso.codegen.actions;

import org.etg.ETGProperties;
import org.etg.espresso.codegen.TestCodeMapper;
import org.etg.mate.models.Action;

import java.util.List;

import static org.etg.espresso.codegen.viewPicking.ViewPickingStatementGenerator.getIsRootMatcher;

public abstract class ActionCodeMapper {

    protected ETGProperties etgProperties;
    protected Action action;



    public ActionCodeMapper(ETGProperties etgProperties, Action action) {
        this.etgProperties = etgProperties;
        this.action = action;
    }

    public abstract String addTestCodeLines(List<String> testCodeLines, TestCodeMapper testCodeMapper, int actionIndex, int actionsCount);

    protected String createActionStatement(String variableName, String statement, TestCodeMapper testCodeMapper) {
        // Although tecnically correct, the following causes a lot of problems.
        // The scrollTo() action tends to fail quite often.
        // if(action.getWidget().isSonOfScrollable()) {
        //     completeAction = getScrollToAction() + ", " + completeAction;
        // }

        String performStatement = variableName + ".perform(" + statement + ")" + testCodeMapper.getStatementTerminator();

        performStatement += "\n";

        return performStatement;
    }

    protected String createActionStatementOnRoot(String statement, TestCodeMapper testCodeMapper) {
        String performStatement = "onView(" + getIsRootMatcher() + ").perform(" + statement + ")" + testCodeMapper.getStatementTerminator();

        performStatement += "\n";

        return performStatement;
    }

    protected void addCloseSoftKeyboardAction(List<String> testCodeLines, TestCodeMapper testCodeMapper) {
        testCodeLines.add(getCloseSoftKeyboardAction() + testCodeMapper.getStatementTerminator());
    }

    private String getCloseSoftKeyboardAction() {
        return "Espresso.closeSoftKeyboard()";
    }

    private String getScrollToAction() {
        return "scrollTo()";
    }
}
