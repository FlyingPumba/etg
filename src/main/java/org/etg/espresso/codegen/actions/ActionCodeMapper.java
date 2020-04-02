package org.etg.espresso.codegen.actions;

import org.etg.ETGProperties;
import org.etg.espresso.codegen.TestCodeMapper;
import org.etg.mate.models.Action;

import java.util.List;

public abstract class ActionCodeMapper {

    protected ETGProperties etgProperties;
    protected Action action;



    public ActionCodeMapper(ETGProperties etgProperties, Action action) {
        this.etgProperties = etgProperties;
        this.action = action;
    }

    public abstract String addTestCodeLines(List<String> testCodeLines, TestCodeMapper testCodeMapper, int actionIndex, int actionsCount);

    protected String createActionStatement(String variableName, int recyclerViewChildPosition, String statement, TestCodeMapper testCodeMapper) {
        testCodeMapper.mIsRecyclerViewActionAdded = testCodeMapper.mIsRecyclerViewActionAdded || recyclerViewChildPosition != -1;
        // No need to explicitly scroll to perform an action on a RecyclerView child.
        String completeAction = statement;
        completeAction = recyclerViewChildPosition == -1
                ? completeAction
                : getActionOnItemAtPositionMethodCallPrefix(testCodeMapper) + recyclerViewChildPosition + ", " + completeAction + ")";

        if(action.getWidget().isSonOfScrollable()) {
            completeAction = getScrollToAction() + ", " + completeAction;
        }

        String performStatement = variableName + ".perform(" + completeAction + ")" + testCodeMapper.getStatementTerminator();

        performStatement += "\n";

        return performStatement;
    }

    private String getActionOnItemAtPositionMethodCallPrefix(TestCodeMapper testCodeMapper) {
        return testCodeMapper.mIsKotlinTestClass ? "actionOnItemAtPosition<ViewHolder>(" : "actionOnItemAtPosition(";
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
