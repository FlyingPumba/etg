package org.etg.espresso.codegen.actions;

import org.etg.ETGProperties;
import org.etg.espresso.codegen.codeMapper.CodeMapperType;
import org.etg.espresso.codegen.codeMapper.StandardTestCodeMapper;
import org.etg.mate.models.Action;

import java.util.List;

public class WaitActionCodeMapper extends ActionCodeMapper {

    private long sleepTime = 0;

    public WaitActionCodeMapper(ETGProperties etgProperties, Action action) {
        super(etgProperties, action);
    }

    public WaitActionCodeMapper(ETGProperties etgProperties, Action action, long sleepTime) {
        super(etgProperties, action);
        this.sleepTime = sleepTime;
    }

    @Override
    public String addTestCodeLines(List<String> testCodeLines, StandardTestCodeMapper testCodeMapper, int actionIndex, int actionsCount) {
        if (etgProperties.getCodeMapper() == CodeMapperType.Standard) {
            testCodeLines.add(createActionStatementOnRoot(getWaitForAction(), testCodeMapper) + "\n");
        } else {
            testCodeLines.add(getWaitForAction() + "\n");
        }
        testCodeMapper.waitActionAdded = true;
        return null;
    }

    private String getWaitForAction() {
        return String.format("waitFor(%d)", sleepTime);
    }
}
