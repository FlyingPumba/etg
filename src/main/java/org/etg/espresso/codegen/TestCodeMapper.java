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
package org.etg.espresso.codegen;

import org.etg.ETGProperties;
import org.etg.espresso.codegen.actions.ActionCodeMapper;
import org.etg.espresso.codegen.actions.ActionCodeMapperFactory;
import org.etg.espresso.templates.java.IsEqualTrimmingAndIgnoringCaseJavaTemplate;
import org.etg.espresso.templates.TemplatesFactory;
import org.etg.espresso.templates.VelocityTemplate;
import org.etg.espresso.templates.java.VisibleViewMatcherJavaTemplate;
import org.etg.espresso.templates.kotlin.EspressoUtils;
import org.etg.espresso.templates.kotlin.IsEqualTrimmingAndIgnoringCaseKotlinTemplate;
import org.etg.espresso.templates.kotlin.MockedServerTest;
import org.etg.espresso.templates.kotlin.VisibleViewMatcherKotlinTemplate;
import org.etg.mate.models.Action;
import org.etg.mate.models.ActionType;

import java.util.*;

public class TestCodeMapper {

    public boolean mIsclassOrSuperClassesNameAdded = false;
    public boolean mUseTextForElementMatching = true;
    public boolean swipeActionAdded = false;
    public boolean longClickActionAdded = false;
    public boolean clickActionAdded = false;
    public boolean waitActionAdded = false;

    /**
     * Needed templates, every extra template that we need must be listed here
     * **/
    private Set<VelocityTemplate> neededTemplates = new HashSet<>();
    private TemplatesFactory templatesFactory = new TemplatesFactory();

    /**
     * Map of variable_name -> first_unused_index. This map is used to ensure that variable names are unique.
     */
    public Map<String, Integer> mVariableNameIndexes = new HashMap<>();
    private ETGProperties etgProperties;

    public TestCodeMapper(ETGProperties properties) {
        etgProperties = properties;
        if (etgProperties.useKotlinFormat()) {
            neededTemplates.add(new VisibleViewMatcherKotlinTemplate());
            neededTemplates.add(new IsEqualTrimmingAndIgnoringCaseKotlinTemplate());
            neededTemplates.add(new MockedServerTest());
            neededTemplates.add(new EspressoUtils());
        } else {
            neededTemplates.add(new VisibleViewMatcherJavaTemplate());
            neededTemplates.add(new IsEqualTrimmingAndIgnoringCaseJavaTemplate());
        }
    }

    public void addTemplateFor(TemplatesFactory.Template action) {
        neededTemplates.add(templatesFactory.createFor(action, etgProperties));
    }

    public Set<VelocityTemplate> getNeededTemplates() {
        return neededTemplates;
    }

    public void addTestCodeLinesForAction(Action action, List<String> testCodeLines, int actionIndex, int actionsCount) {
        List<String> actionTestCodeLines = new ArrayList<>();

        if (actionIndex == 0 && etgProperties.getSleepAfterLaunch() != -1) {
            Action waitAfterLaunch = new Action(ActionType.WAIT);
            waitAfterLaunch.setTimeToWait(etgProperties.getSleepAfterLaunch());
            actionTestCodeLines.addAll(mapActionToTestCodeLiens(waitAfterLaunch, actionIndex , actionsCount));
        }

        if (action.getNetworkingInfo().size() > 0) {
            Action mockServerResponse = new Action(ActionType.MOCK_SERVER_RESPONSE);
            mockServerResponse.setNetworkingInfo(action.getNetworkingInfo());
            actionTestCodeLines.addAll(mapActionToTestCodeLiens(mockServerResponse, actionIndex , actionsCount));
        }

        actionTestCodeLines.addAll(mapActionToTestCodeLiens(action, actionIndex , actionsCount));

        if (actionIndex != actionsCount && etgProperties.getSleepAfterActions() != -1) {
            Action waitAfterAction = new Action(ActionType.WAIT);
            waitAfterAction.setTimeToWait(etgProperties.getSleepAfterActions());
            actionTestCodeLines.addAll(mapActionToTestCodeLiens(waitAfterAction, actionIndex , actionsCount));
        }

        testCodeLines.addAll(actionTestCodeLines);
    }

    private List<String> mapActionToTestCodeLiens(Action action, int actionIndex, int actionsCount) {
        ActionCodeMapper actionCodeMapper = ActionCodeMapperFactory.get(etgProperties, action);

        List<String> actionTestCodeLines = new ArrayList<>();
        actionCodeMapper.addTestCodeLines(actionTestCodeLines, this, actionIndex, actionsCount);

        return actionTestCodeLines;
    }

    public static String getStatementTerminator() {
        return ";";
    }
}
