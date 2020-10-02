package org.etg.espresso.codegen.actions;

import org.etg.ETGProperties;
import org.etg.espresso.codegen.codeMapper.StandardTestCodeMapper;
import org.etg.mate.models.Action;

import java.util.List;

public class EnterActionCodeMapper extends ActionCodeMapper {

    public EnterActionCodeMapper(ETGProperties etgProperties, Action action) {
        super(etgProperties, action);
    }

    @Override
    public String addTestCodeLines(List<String> testCodeLines, StandardTestCodeMapper testCodeMapper, int actionIndex, int actionsCount) {
        testCodeLines.add(createActionStatementOnRoot(getPressEnterKeyAction(), testCodeMapper));
        return null;
    }

    private String getPressEnterKeyAction() {
        return "pressKey(KeyEvent.KEYCODE_ENTER)";
    }
}
