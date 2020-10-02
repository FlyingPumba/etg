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
    public void addTestCodeLinesForAction(Action action, List<String> testCodeLines, int actionIndex, int actionsCount) {
        List<String> actionTestCodeLines = new ArrayList<>();

        if (actionIndex == 0 && etgProperties.getSleepAfterLaunch() != -1) {
            Action waitAfterLaunch = new Action(ActionType.WAIT);
            waitAfterLaunch.setTimeToWait(etgProperties.getSleepAfterLaunch());
            actionTestCodeLines.addAll(mapActionToTestCodeLines(waitAfterLaunch, actionIndex , actionsCount));
        }

        if (action.getNetworkingInfo().size() > 0) {
            Action mockServerResponse = new Action(ActionType.MOCK_SERVER_RESPONSE);
            mockServerResponse.setNetworkingInfo(action.getNetworkingInfo());
            actionTestCodeLines.addAll(mapActionToTestCodeLines(mockServerResponse, actionIndex , actionsCount));
        }

        actionTestCodeLines.addAll(mapActionToTestCodeLines(action, actionIndex , actionsCount));

        testCodeLines.addAll(actionTestCodeLines);
    }

    private List<String> mapActionToTestCodeLines(Action action, int actionIndex, int actionsCount) {
        // get standard code mapping for action
        ActionCodeMapper actionCodeMapper = ActionCodeMapperFactory.get(etgProperties, action);
        List<String> espressoLinesForAction = new ArrayList<>();
        actionCodeMapper.addTestCodeLines(espressoLinesForAction, standardTestCodeMapper, actionIndex, actionsCount);
        if (action.getActionType() == ActionType.WAIT || action.getActionType() == ActionType.MOCK_SERVER_RESPONSE) {
            extraImports.add(String.format("%s.utils.EspressoUtils.Companion.waitFor",
                    etgProperties.getTestPackageName()));
            return espressoLinesForAction;
        }

        // send standard code mapping to the proper screen robot
        String idByActivity = action.getWidget().getIdByActivity();
        String methodName;
        RobotTemplate robotTemplate = null;
        if (idByActivity.isEmpty()) {
            // this action do not belongs to any particular screen, send it to the Screen Robot
            methodName = screenRobotTemplate.addMethod(action, espressoLinesForAction);
            // FIX: this breaks if a method from screen robot is called while outside a robot call chain
        } else {
            String robotName = buildScreenRobotName(idByActivity);
            robotTemplate = addRobotTemplate(robotName);
            methodName = robotTemplate.addMethod(action, espressoLinesForAction);
        }

        // Now, build robot calls for actual test
        List<String> actionTestCodeLines = new ArrayList<>();
        if (currentRobotName != null && robotTemplate != null &&
                !currentRobotName.equals(robotTemplate.getRobotName())) {
            // we are changing from one screen robot to another
            actionTestCodeLines.add("}");
            currentRobotName = null;
        }

        if (currentRobotName == null && robotTemplate != null) {
            // we are starting a new screen
            actionTestCodeLines.add(String.format("\n%s {", robotTemplate.getScreenName()));
            currentRobotName = robotTemplate.getRobotName();
        }

        // append method call to robot
        if (action.getActionType() == ActionType.TYPE_TEXT) {
            actionTestCodeLines.add((String.format("%s(%s)", methodName, boxString(action.getExtraInfo()))));
        } else {
            actionTestCodeLines.add((String.format("%s()", methodName)));
        }

        if (actionIndex == actionsCount - 1) {
            // close chain of calls to robot
            actionTestCodeLines.add("}");
            currentRobotName = null;
        }

        return actionTestCodeLines;
    }

    private RobotTemplate addRobotTemplate(String robotName) {
        if (!robotTemplates.containsKey(robotName)) {
            RobotTemplate newTemplate = new RobotTemplate(robotName);
            robotTemplates.put(robotName, newTemplate);

            neededTemplates.add(newTemplate);
            extraImports.add(String.format("%s.robot.%s",
                    etgProperties.getTestPackageName(), newTemplate.getScreenName()));
        }

        return robotTemplates.get(robotName);
    }

    private String buildScreenRobotName(String idByActivity) {
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
