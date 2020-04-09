package org.etg.espresso.codegen.actions;

import org.etg.ETGProperties;
import org.etg.espresso.codegen.TestCodeMapper;
import org.etg.espresso.codegen.viewPicking.ViewPickingStatementGenerator;
import org.etg.espresso.templates.TemplatesFactory;
import org.etg.mate.models.Action;

import java.util.List;

public class LongClickActionCodeMapper extends ActionCodeMapper {

    public LongClickActionCodeMapper(ETGProperties etgProperties, Action action) {
        super(etgProperties, action);
    }

    @Override
    public String addTestCodeLines(List<String> testCodeLines, TestCodeMapper testCodeMapper, int actionIndex, int actionsCount) {
        ViewPickingStatementGenerator pickingStatementGenerator = new ViewPickingStatementGenerator(etgProperties, action);
        String variableName = pickingStatementGenerator.addTestCodeLines(testCodeLines, testCodeMapper, actionIndex, actionsCount);

        testCodeLines.add(createActionStatement(variableName, getLongClickAction(), testCodeMapper));

        if (actionIndex != actionsCount-1) {
            addCloseSoftKeyboardAction(testCodeLines, testCodeMapper);
        }

        testCodeMapper.addTemplateFor(TemplatesFactory.Template.LONG_CLICK_ACTION);
        testCodeMapper.longClickActionAdded = true;

        return null;
    }

    private String getLongClickAction() {
        return "getLongClickAction()";
    }
}
