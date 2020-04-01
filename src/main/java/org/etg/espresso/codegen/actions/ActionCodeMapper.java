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

    public abstract String addTestCodeLines(List<String> testCodeLines, TestCodeMapper testCodeMapper);

    protected String createActionStatement(String variableName, int recyclerViewChildPosition, String statement, TestCodeMapper testCodeMapper) {
        testCodeMapper.mIsRecyclerViewActionAdded = testCodeMapper.mIsRecyclerViewActionAdded || recyclerViewChildPosition != -1;
        // No need to explicitly scroll to perform an action on a RecyclerView child.
        String completeAction = statement;
        completeAction = recyclerViewChildPosition == -1
                ? completeAction
                : getActionOnItemAtPositionMethodCallPrefix(testCodeMapper) + recyclerViewChildPosition + ", " + completeAction + ")";

        if (action.getSwipe() == null) {
            // If action is a Swipe, then we don't need to preemptively close soft keyboard
            // Otherwise, this is a Click, LongClick or TypeText action, that might need closing the soft keyboard.
            completeAction += ", " + getCloseSoftKeyboard();
        }

        String performStatement = variableName + ".perform(" + completeAction + ")" + testCodeMapper.getStatementTerminator();

        performStatement += "\n";

        return performStatement;
    }

    private String getActionOnItemAtPositionMethodCallPrefix(TestCodeMapper testCodeMapper) {
        return testCodeMapper.mIsKotlinTestClass ? "actionOnItemAtPosition<ViewHolder>(" : "actionOnItemAtPosition(";
    }

    private String getCloseSoftKeyboard() {
        return "closeSoftKeyboard()";
    }
}
