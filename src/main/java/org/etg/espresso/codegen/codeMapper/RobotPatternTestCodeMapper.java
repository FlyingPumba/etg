package org.etg.espresso.codegen.codeMapper;

import org.etg.ETGProperties;
import org.etg.espresso.codegen.actions.ActionCodeMapper;
import org.etg.espresso.codegen.actions.ActionCodeMapperFactory;
import org.etg.espresso.templates.kotlin.robot.RobotTemplate;
import org.etg.espresso.templates.kotlin.robot.ScreenRobotTemplate;
import org.etg.mate.models.Action;
import org.etg.mate.models.ActionType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.etg.espresso.util.StringHelper.boxString;

public class RobotPatternTestCodeMapper extends TestCodeMapper {

    private static final Map<String, RobotTemplate> robotTemplates = new HashMap<>();
    private static final ScreenRobotTemplate screenRobotTemplate = new ScreenRobotTemplate();

    StandardTestCodeMapper standardTestCodeMapper;

    String currentRobotName = null;

    public RobotPatternTestCodeMapper(ETGProperties properties) {
        super(properties);

        neededTemplates.add(screenRobotTemplate);
        standardTestCodeMapper = new StandardTestCodeMapper(properties);
    }

    @Override
    public void addTestCodeLinesForAction(int index, List<Action> actions, List<String> testCodeLines) {
        List<String> actionTestCodeLines = new ArrayList<>();

        if (index == 0 && etgProperties.getSleepAfterLaunch() != -1) {
            Action waitAfterLaunch = new Action(ActionType.WAIT);
            waitAfterLaunch.setTimeToWait(etgProperties.getSleepAfterLaunch());
            actionTestCodeLines.addAll(standardCodeMapping(waitAfterLaunch, index , actions.size()));
            extraImports.add(String.format("%s.utils.EspressoUtils.Companion.waitFor",
                    etgProperties.getTestPackageName()));
        }

        actionTestCodeLines.addAll(mapActionToRobotCalls(index , actions));

        testCodeLines.addAll(actionTestCodeLines);
    }

    private List<String> standardCodeMapping(Action action, int actionIndex, int actionsCount) {
        ActionCodeMapper actionCodeMapper = ActionCodeMapperFactory.get(etgProperties, action);
        List<String> espressoLinesForAction = new ArrayList<>();
        actionCodeMapper.addTestCodeLines(espressoLinesForAction, standardTestCodeMapper, actionIndex, actionsCount);
        return espressoLinesForAction;
    }

    private List<String> mapActionToRobotCalls(int index, List<Action> actions) {
        Action action = actions.get(index);
        if (shouldSkipAction(action)) {
            return new ArrayList<>();
        }

        // get standard code mapping (Espresso code) for this action
        List<String> espressoLinesForAction = standardCodeMapping(action, index, actions.size());

        // Figure out which robot are we using for this action and following ones
        List<String> actionTestCodeLines = new ArrayList<>();
        String nextRobotName = getNextRobotName(index, actions, currentRobotName);
        if (currentRobotName == null) {
            // we are starting a new screen
            dumpMockResponsesForRobotScreen(index, actions, nextRobotName, actionTestCodeLines);
            actionTestCodeLines.add(String.format("\n%s {", RobotTemplate.buildRobotScreenName(nextRobotName)));
            currentRobotName = nextRobotName;
        } else if (!currentRobotName.equals(nextRobotName)) {
            // we are changing from one screen robot to another
            actionTestCodeLines.add("}\n");
            dumpMockResponsesForRobotScreen(index, actions, nextRobotName, actionTestCodeLines);
            actionTestCodeLines.add(String.format("\n%s {", RobotTemplate.buildRobotScreenName(nextRobotName)));
            currentRobotName = nextRobotName;
        }

        // send standard code mapping to the proper screen robot
        String methodName;
        if (nextRobotName == null || ScreenRobotTemplate.supportsAction(action)) {
            // this action do not belongs to any particular screen, send it to the Screen Robot
            methodName = screenRobotTemplate.addMethod(action, espressoLinesForAction);
        } else {
            RobotTemplate robotTemplate = addRobotTemplate(nextRobotName);
            methodName = robotTemplate.addMethod(action, espressoLinesForAction);
        }

        // append method call to robot
        if (action.getActionType() == ActionType.TYPE_TEXT) {
            actionTestCodeLines.add((String.format("%s(%s)", methodName, boxString(action.getExtraInfo()))));
        } else {
            actionTestCodeLines.add((String.format("%s()", methodName)));
        }

        if (index == actions.size() - 1) {
            // close chain of calls to robot
            actionTestCodeLines.add("}");
            currentRobotName = null;
        }

        return actionTestCodeLines;
    }

    private boolean shouldSkipAction(Action action) {
        if (action.getWidget().getClazz().contains("Spinner")) {
            // avoid clicking spinners, it only causes problems due to flakiness.
            return true;
        }

        return false;
    }

    private void dumpMockResponsesForRobotScreen(int index, List<Action> actions, String currentRobotName, List<String> actionTestCodeLines) {
        for (int i = index; i < actions.size(); i++) {
            Action action = actions.get(i);
            String nextRobotName = getNextRobotName(i, actions, currentRobotName);
            if (!currentRobotName.equals(nextRobotName)) {
                return;
            }

            List<String> networkingInfo = action.getNetworkingInfo();
            if (!networkingInfo.isEmpty()) {
                Action mockServerResponse = new Action(ActionType.MOCK_SERVER_RESPONSE);
                mockServerResponse.setNetworkingInfo(networkingInfo);
                actionTestCodeLines.addAll(standardCodeMapping(mockServerResponse, 0, 0));
            }
        }
    }

    private String getNextRobotName(int index, List<Action> actions, String currentRobotName) {
        Action currentAction = actions.get(index);
        String robotNameForCurrentAction = getRobotNameForAction(currentAction);

        if (robotNameForCurrentAction != null) {
            // This action has a specific robot associated
            return robotNameForCurrentAction;
        }

        // is this action a part of generic actions before a change of robot name?
        // In that case, we count it as part of the next robot screen.
        for (int i = index + 1; i < actions.size(); i++) {
            Action action = actions.get(i);
            String robotName = getRobotNameForAction(action);
            boolean genericAction = robotName == null;

            if (!genericAction) {
                if (currentRobotName == null) {
                    // currently, we are not in any robot screen. Take the robot name of the next robot screen.
                    return robotName;
                }

                boolean robotNameChanged = !currentRobotName.equals(robotName);
                if (robotNameChanged) {
                    if (currentAction.getActionType() == ActionType.BACK) {
                        // if name changes but we are deciding on a Back action, assume it is from the currrent robot
                        return currentRobotName;
                    } else {
                        // otherwise, the action belongs to a new robot
                        return robotName;
                    }
                } else {
                    // the action after scroll(s) has the same robot name as the current one.
                    return currentRobotName;
                }
            }
        }

        // All actions are generic until the end of the test.
        // Do we have a screen name right now? If not, use the generic screen robot
        if (currentRobotName != null) {
             return currentRobotName;
        } else {
            return "generic";
        }
    }

    private RobotTemplate addRobotTemplate(String robotName) {
        if (!robotTemplates.containsKey(robotName)) {
            RobotTemplate newTemplate = new RobotTemplate(robotName);
            robotTemplates.put(robotName, newTemplate);

            neededTemplates.add(newTemplate);
            extraImports.add(String.format("%s.robot.%s",
                    etgProperties.getTestPackageName(), newTemplate.getRobotScreenName()));
        }

        return robotTemplates.get(robotName);
    }

    private String getRobotNameForAction(Action action) {
        String idByActivity = action.getWidget().getIdByActivity();
        if (idByActivity == null || idByActivity.isEmpty()) {
            return null;
        }

        String activityFullQualifiedName = idByActivity.split("_")[0].split("/")[1];
        boolean outsideActivity = !activityFullQualifiedName.startsWith(etgProperties.getPackageName());
        if (outsideActivity) {
            // this is an activity provided by another package (e.g., barcode reader)
            return null;
        }

        String[] parts = activityFullQualifiedName.split("\\.");
        String simpleName = parts[parts.length - 1];

        if (simpleName.endsWith("Activity")) {
            return simpleName.split("Activity")[0];
        } else {
            return simpleName;
        }
    }
}
