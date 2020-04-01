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
import org.etg.espresso.templates.VelocityTemplate;
import org.etg.espresso.templates.TemplatesFactory;
import org.etg.mate.models.Action;
import org.etg.mate.models.Swipe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TestCodeMapper {

    public boolean mIsChildAtPositionAdded = false;
    public boolean mIsRecyclerViewActionAdded = false;
    public boolean mIsclassOrSuperClassesNameAdded = false;
    public boolean mIsKotlinTestClass = false;
    public boolean mUseTextForElementMatching = true;
    public boolean mSurroundPerformsWithTryCatch = true;
    public boolean swipeActionAdded = false;
    public boolean longClickActionAdded = false;
    public boolean clickActionAdded = false;

    /**
     * Needed templates, every extra templata that we need must be listed here
     * **/
    private Set<VelocityTemplate> neededTemplates = new HashSet<>();
    private TemplatesFactory templatesFactory = new TemplatesFactory();

    /**
     * Map of variable_name -> first_unused_index. This map is used to ensure that variable names are unique.
     */
    public Map<String, Integer> mVariableNameIndexes = new HashMap<>();
    private int performCount = 0;
    private ETGProperties etgProperties;

    public TestCodeMapper(ETGProperties properties) {
        etgProperties = properties;
    }

    public void addTemplateFor(TemplatesFactory.Template action) {
        neededTemplates.add(templatesFactory.createFor(action));
    }

    public Set<VelocityTemplate> getNeededTemplates() {
        return neededTemplates;
    }

    public int addTestCodeLinesForAction(Action action, List<String> testCodeLines) {
        ActionCodeMapper actionCodeMapper = ActionCodeMapperFactory.get(etgProperties, action);

        List<String> actionTestCodeLines = new ArrayList<>();
        actionCodeMapper.addTestCodeLines(actionTestCodeLines, this);

        if (mSurroundPerformsWithTryCatch) {
            // surround lines with try-catch
            surroundLinesWithTryCatch(actionTestCodeLines);
        }

        testCodeLines.addAll(actionTestCodeLines);

        return performCount-1;
    }

    private void surroundLinesWithTryCatch(List<String> actionTestCodeLines) {
        String tryStr = "\ntry {\n";
        String catchStr = "\n" +
                "} catch (Exception e) {\n" +
                "System.out.println(buildPerformExceptionMessage(e, " + performCount + "))" + getStatementTerminator() + "\n" +
                "}";

        actionTestCodeLines.add(0, tryStr);
        actionTestCodeLines.add(catchStr);

        performCount++;
    }

    public void setSurroundPerformsWithTryCatch(boolean mSurroundPerformsWithTryCatch) {
        this.mSurroundPerformsWithTryCatch = mSurroundPerformsWithTryCatch;
        this.performCount = 0;
        this.mVariableNameIndexes = new HashMap<>();
    }

    public String getStatementTerminator() {
        return mIsKotlinTestClass ? "" : ";";
    }
}
