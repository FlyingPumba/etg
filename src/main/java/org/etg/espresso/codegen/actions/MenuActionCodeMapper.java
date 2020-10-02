package org.etg.espresso.codegen.actions;

import org.etg.ETGProperties;
import org.etg.espresso.codegen.codeMapper.StandardTestCodeMapper;
import org.etg.mate.models.Action;

import java.util.List;

public class MenuActionCodeMapper extends ActionCodeMapper {

    public MenuActionCodeMapper(ETGProperties etgProperties, Action action) {
        super(etgProperties, action);
    }

    @Override
    public String addTestCodeLines(List<String> testCodeLines, StandardTestCodeMapper testCodeMapper, int actionIndex, int actionsCount) {
        testCodeLines.add(createActionStatementOnRoot(getPressMenuKeyAction(), testCodeMapper));

        return null;
    }

    private String getPressMenuKeyAction() {
        return "pressMenuKey()";
    }
}
