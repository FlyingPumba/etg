/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

    private static Map<String, RobotTemplate> robotTemplates = new HashMap<>();
    private static ScreenRobotTemplate screenRobotTemplate = new ScreenRobotTemplate();

    StandardTestCodeMapper standardTestCodeMapper;

    String currentRobotName = null;

    public RobotPatternTestCodeMapper(ETGProperties properties) {
        super(properties);

        neededTemplates.add(screenRobotTemplate);
        standardTestCodeMapper = new StandardTestCodeMapper(properties);
    }

    @Override
    public void addTestCodeLinesForAction(int index, List<Action> actions, List<String> testCodeLines) {
        Action action = actions.get(index);
        List<String> actionTestCodeLines = new ArrayList<>();

        if (index == 0 && etgProperties.getSleepAfterLaunch() != -1) {
            Action waitAfterLaunch = new Action(ActionType.WAIT);
            waitAfterLaunch.setTimeToWait(etgProperties.getSleepAfterLaunch());
            actionTestCodeLines.addAll(standardCodeMapping(waitAfterLaunch, index , actions.size()));
            extraImports.add(String.format("%s.utils.EspressoUtils.Companion.waitFor",
                    etgProperties.getTestPackageName()));
        }

        if (action.getNetworkingInfo().size() > 0) {
            Action mockServerResponse = new Action(ActionType.MOCK_SERVER_RESPONSE);
            mockServerResponse.setNetworkingInfo(action.getNetworkingInfo());
            actionTestCodeLines.addAll(standardCodeMapping(mockServerResponse, index , actions.size()));
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
        // get standard code mapping (Espresso code) for this action
        Action action = actions.get(index);
        List<String> espressoLinesForAction = standardCodeMapping(action, index, actions.size());

        // Figure out which robot are we using for this action and following ones
        List<String> actionTestCodeLines = new ArrayList<>();
        String nextRobotName = getNextRobotName(index, actions);
        if (currentRobotName == null) {
            // we are starting a new screen
            actionTestCodeLines.add(String.format("\n%s {", RobotTemplate.buildRobotScreenName(nextRobotName)));
            currentRobotName = nextRobotName;
        } else if (!currentRobotName.equals(nextRobotName)) {
            // we are changing from one screen robot to another
            actionTestCodeLines.add(String.format("}\n\n%s {", RobotTemplate.buildRobotScreenName(nextRobotName)));
            currentRobotName = nextRobotName;
        }

        // send standard code mapping to the proper screen robot
        String methodName;
        if (nextRobotName == null || ScreenRobotTemplate.supportsAction(action)) {
            // this action do not belongs to any particular screen, send it to the Screen Robot
            methodName = screenRobotTemplate.addMethod(action, espressoLinesForAction);
            // FIX: this breaks if a method from screen robot is called while outside a robot call chain
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

    private String getNextRobotName(int index, List<Action> actions) {
        Action currentAction = actions.get(index);
        String robotNameForCurrentAction = getRobotNameForAction(currentAction);

        if (robotNameForCurrentAction != null) {
            // This action has a specific robot associated
            return robotNameForCurrentAction;
        }

        // is this action a part of generic actions before a change of robot name?
        // In that case, we count it as part of the next robot screen.
        for (int i = index; i < actions.size(); i++) {
            Action action = actions.get(i);
            String robotName = getRobotNameForAction(action);
            boolean genericAction = robotName == null;

            if (!genericAction) {
                boolean robotNameChanged = currentRobotName != null && !currentRobotName.equals(robotName);
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

        // this action does not have a specific robot name, return the current one.
        return currentRobotName;
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
        String[] parts = activityFullQualifiedName.split("\\.");
        String simpleName = parts[parts.length - 1];

        if (simpleName.endsWith("Activity")) {
            return simpleName.split("Activity")[0];
        } else {
            return simpleName;
        }
    }
}
