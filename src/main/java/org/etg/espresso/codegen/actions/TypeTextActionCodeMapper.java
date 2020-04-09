package org.etg.espresso.codegen.actions;

import org.etg.ETGProperties;
import org.etg.espresso.codegen.TestCodeMapper;
import org.etg.espresso.codegen.viewPicking.ViewPickingStatementGenerator;
import org.etg.mate.models.Action;

import java.util.List;

import static org.etg.espresso.util.StringHelper.boxString;

public class TypeTextActionCodeMapper extends ActionCodeMapper {

    public TypeTextActionCodeMapper(ETGProperties etgProperties, Action action) {
        super(etgProperties, action);
    }

    @Override
    public String addTestCodeLines(List<String> testCodeLines, TestCodeMapper testCodeMapper, int actionIndex, int actionsCount) {
        ViewPickingStatementGenerator pickingStatementGenerator = new ViewPickingStatementGenerator(etgProperties, action);
        String variableName = pickingStatementGenerator.addTestCodeLines(testCodeLines, testCodeMapper, actionIndex, actionsCount);

        testCodeLines.add(createActionStatement(variableName,
                "replaceText(" + boxString(action.getExtraInfo()) + ")", testCodeMapper));

        if (actionIndex != actionsCount-1) {
            addCloseSoftKeyboardAction(testCodeLines, testCodeMapper);
        }

        return null;
    }
}
