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

        testCodeLines.add(createActionStatement(variableName, recyclerViewChildPosition,
                "replaceText(" + boxString(action.getExtraInfo()) + ")", testCodeMapper));
        addCloseSoftKeyboardAction(testCodeLines, testCodeMapper);

        return null;
    }
}
