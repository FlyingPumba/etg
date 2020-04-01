package org.etg.espresso.codegen.actions;

import org.etg.ETGProperties;
import org.etg.espresso.codegen.TestCodeMapper;
import org.etg.espresso.codegen.viewPicking.ViewPickingStatementGenerator;
import org.etg.espresso.templates.TemplatesFactory;
import org.etg.mate.models.Action;

import java.util.List;

public class ClickActionCodeMapper extends ActionCodeMapper {

    public ClickActionCodeMapper(ETGProperties etgProperties, Action action) {
        super(etgProperties, action);
    }

    @Override
    public String addTestCodeLines(List<String> testCodeLines, TestCodeMapper testCodeMapper) {
        ViewPickingStatementGenerator pickingStatementGenerator = new ViewPickingStatementGenerator(etgProperties, action);
        String variableName = pickingStatementGenerator.addTestCodeLines(testCodeLines, testCodeMapper);

        int recyclerViewChildPosition = action.getWidget().getRecyclerViewChildPosition();

        testCodeLines.add(createActionStatement(variableName, recyclerViewChildPosition, getClickViewAction(), testCodeMapper));
        testCodeMapper.addTemplateFor(TemplatesFactory.Template.CLICK_ACTION);
        testCodeMapper.clickActionAdded = true;

        return null;
    }

    private String getClickViewAction() {
        return "getClickAction()";
    }
}
