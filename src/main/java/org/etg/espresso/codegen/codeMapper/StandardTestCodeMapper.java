package org.etg.espresso.codegen.codeMapper;

import org.etg.ETGProperties;
import org.etg.espresso.codegen.actions.ActionCodeMapper;
import org.etg.espresso.codegen.actions.ActionCodeMapperFactory;
import org.etg.mate.models.Action;
import org.etg.mate.models.ActionType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StandardTestCodeMapper extends TestCodeMapper {

    /**
     * Map of variable_name -> first_unused_index. This map is used to ensure that variable names are unique.
     */
    public Map<String, Integer> mVariableNameIndexes = new HashMap<>();

    public StandardTestCodeMapper(ETGProperties properties) {
        super(properties);
    }

    @Override
    public void addTestCodeLinesForAction(int index, List<Action> actions, List<String> testCodeLines) {
        Action action = actions.get(index);
        List<String> actionTestCodeLines = new ArrayList<>();

        if (index == 0 && etgProperties.getSleepAfterLaunch() != -1) {
            Action waitAfterLaunch = new Action(ActionType.WAIT);
            waitAfterLaunch.setTimeToWait(etgProperties.getSleepAfterLaunch());
            actionTestCodeLines.addAll(mapActionToTestCodeLiens(waitAfterLaunch, index , actions.size()));
        }

        if (action.getNetworkingInfo().size() > 0) {
            Action mockServerResponse = new Action(ActionType.MOCK_SERVER_RESPONSE);
            mockServerResponse.setNetworkingInfo(action.getNetworkingInfo());
            actionTestCodeLines.addAll(mapActionToTestCodeLiens(mockServerResponse, index , actions.size()));
        }

        actionTestCodeLines.addAll(mapActionToTestCodeLiens(action, index , actions.size()));

        if (index != actions.size() && etgProperties.getSleepAfterActions() != -1) {
            Action waitAfterAction = new Action(ActionType.WAIT);
            waitAfterAction.setTimeToWait(etgProperties.getSleepAfterActions());
            actionTestCodeLines.addAll(mapActionToTestCodeLiens(waitAfterAction, index , actions.size()));
        }

        testCodeLines.addAll(actionTestCodeLines);
    }

    private List<String> mapActionToTestCodeLiens(Action action, int actionIndex, int actionsCount) {
        ActionCodeMapper actionCodeMapper = ActionCodeMapperFactory.get(etgProperties, action);

        List<String> actionTestCodeLines = new ArrayList<>();
        actionCodeMapper.addTestCodeLines(actionTestCodeLines, this, actionIndex, actionsCount);

        return actionTestCodeLines;
    }
}
