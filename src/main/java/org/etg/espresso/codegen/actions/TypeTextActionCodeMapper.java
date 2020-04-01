package org.etg.espresso.codegen.actions;

import org.etg.ETGProperties;
import org.etg.espresso.codegen.TestCodeMapper;
import org.etg.espresso.codegen.viewPicking.ViewPickingStatementGenerator;
import org.etg.mate.models.Action;
import org.etg.mate.models.ActionType;

import java.util.List;

import static org.etg.espresso.util.StringHelper.boxString;
import static org.etg.espresso.util.StringHelper.isNullOrEmpty;

public class TypeTextActionCodeMapper extends ActionCodeMapper {

    public TypeTextActionCodeMapper(ETGProperties etgProperties, Action action) {
        super(etgProperties, action);
    }

    @Override
    public String addTestCodeLines(List<String> testCodeLines, TestCodeMapper testCodeMapper) {
        ViewPickingStatementGenerator pickingStatementGenerator = new ViewPickingStatementGenerator(etgProperties, action);
        String variableName = pickingStatementGenerator.addTestCodeLines(testCodeLines, testCodeMapper);

        int recyclerViewChildPosition = action.getWidget().getRecyclerViewChildPosition();

        String closeSoftKeyboardAction = doesNeedStandaloneCloseSoftKeyboardAction(testCodeMapper) ? "" : (", " + getCloseSoftKeyboard());
        testCodeLines.add(createActionStatement(
                variableName, recyclerViewChildPosition, "replaceText(" + boxString(action.getExtraInfo()) + ")" + closeSoftKeyboardAction, testCodeMapper));

        return null;
    }

    private boolean doesNeedStandaloneCloseSoftKeyboardAction(TestCodeMapper testCodeMapper) {
        // Make text edit in a RecyclerView child always require a standalone close soft keyboard action since actionOnItemAtPosition
        // accepts only a single action.
        return testCodeMapper.mUseTextForElementMatching && action.getActionType() == ActionType.TYPE_TEXT
                && (!isNullOrEmpty(action.getExtraInfo()) || action.getWidget().getRecyclerViewChildPosition() != -1);
    }

    private String getCloseSoftKeyboard() {
        return "closeSoftKeyboard()";
    }
}
