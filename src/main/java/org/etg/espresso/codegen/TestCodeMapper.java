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
import org.etg.espresso.templates.VelocityTemplate;
import org.etg.espresso.templates.TemplatesFactory;
import org.etg.mate.models.Action;
import org.etg.mate.models.ActionType;
import org.etg.mate.models.Swipe;
import org.etg.mate.models.Widget;
import org.etg.utils.Randomness;
import org.etg.utils.Tuple;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.etg.espresso.codegen.MatcherBuilder.Kind.*;
import static org.etg.espresso.util.StringHelper.*;

public class TestCodeMapper {

    private static final int MAX_HIERARCHY_VIEW_LEVEL = 2;
    private static final String VIEW_VARIABLE_CLASS_NAME = "ViewInteraction";
    private static final String DATA_VARIABLE_CLASS_NAME = "DataInteraction";
    private static final String CLASS_VIEW_PAGER = "android.support.v4.view.ViewPager";

    private boolean mIsChildAtPositionAdded = false;
    private boolean mIsRecyclerViewActionAdded = false;
    private boolean mIsclassOrSuperClassesNameAdded = false;
    private boolean mIsKotlinTestClass = false;
    private boolean mUseTextForElementMatching = true;
    private boolean mSurroundPerformsWithTryCatch = true;
    private boolean swipeActionAdded = false;
    private boolean longClickActionAdded = false;
    private boolean clickActionAdded = false;



    /**
     * Needed templates, every extra templata that we need must be listed here
     * **/
    private Set<VelocityTemplate> neededTemplates = new HashSet<>();
    private TemplatesFactory templatesFactory = new TemplatesFactory();

    private String mPressBackCmd = "";

    /**
     * Map of variable_name -> first_unused_index. This map is used to ensure that variable names are unique.
     */
    private final Map<String, Integer> mVariableNameIndexes = new HashMap<>();
    private int performCount = 0;

    public TestCodeMapper(ETGProperties properties) throws Exception {
        if (properties.getEspressoVersion().startsWith("3")) {
            mPressBackCmd = "pressBackUnconditionally";
        } else {
            mPressBackCmd = "pressBack";
        }
    }

    private void addTemplateFor(TemplatesFactory.Template action) {
        neededTemplates.add(templatesFactory.createFor(action));
    }

    public Set<VelocityTemplate> getNeededTemplates() {
        return neededTemplates;
    }

    public void addTestCodeLinesForAction(Action action, List<String> testCodeLines) {
        String lastStatement = null;
        if (testCodeLines.size() > 0) {
            lastStatement = testCodeLines.get(testCodeLines.size() - 1);
        }

        if (action.getActionType() == ActionType.BACK) {
            // the following statement is identically to
            // onView(isRoot()).perform(ViewActions.pressBackUnconditionally());
            // choosing one or the other is just a matter of taste
            String statement = String.format("Espresso.%s()%s", mPressBackCmd, getStatementTerminator());

            if (lastStatement != null && lastStatement.contains("pressMenuKey")) {
                // add hoc heuristic:
                // In the cases where pressMenuKey was just fired, it seems to work better if we don't specifiy the root view
                // as the target of the pressBackUnconditionally action.
                statement = String.format("ViewActions.%s()%s", mPressBackCmd, getStatementTerminator());
            }

            if (mSurroundPerformsWithTryCatch) {
                statement = surroundPerformWithTryCatch(statement);
            }
            testCodeLines.add(statement);
            return;

        } else if (action.getActionType() == ActionType.MENU) {
            String statement = getPressMenuKeyAction() + getStatementTerminator();
            if (mSurroundPerformsWithTryCatch) {
                statement = surroundPerformWithTryCatch(statement);
            }
            testCodeLines.add(statement);
            return;
        }

//        if (event.isDelayedMessagePost()) {
//            testCodeLines.add(createSleepStatement(event.getDelayTime()));
//            return testCodeLines;
//        }

        String variableName = addPickingStatement(action, testCodeLines);
        int recyclerViewChildPosition = action.getWidget().getRecyclerViewChildPosition();


        if (action.getSwipe() != null){//if swipe
            Swipe swipe = action.getSwipe();

            String methdCall = getSwipeAction(swipe);

            testCodeLines.add(createActionStatement(variableName, recyclerViewChildPosition, methdCall, action.getWidget().isSonOfScrollable()));
            testCodeLines.add(getWaitToScrollEndStatement() + getStatementTerminator() + "\n");
            swipeActionAdded = true;
        }

//        else if (event.isPressEditorAction()) {
//            // TODO: If this is the same element that was just edited, consider reusing the same view interaction (i.e., variable name).
//            testCodeLines.add(createActionStatement(variableName, recyclerViewChildPosition, "pressImeActionButton()", false));
//        } else

        else if (action.getActionType() == ActionType.CLICK) {
            testCodeLines.add(createActionStatement(variableName, recyclerViewChildPosition, getClickViewAction(), action.getWidget().isSonOfScrollable()));
            addTemplateFor(TemplatesFactory.Template.CLICK_ACTION);
            clickActionAdded = true;
        } else if (action.getActionType() == ActionType.LONG_CLICK) {
            testCodeLines.add(createActionStatement(variableName, recyclerViewChildPosition, getLongClickAction(), action.getWidget().isSonOfScrollable()));
            addTemplateFor(TemplatesFactory.Template.LONG_CLICK_ACTION);
            longClickActionAdded = true;
        } else if (action.getActionType() == ActionType.TYPE_TEXT) {
            String closeSoftKeyboardAction = doesNeedStandaloneCloseSoftKeyboardAction(action) ? "" : (", " + getCloseSoftKeyboard());
            testCodeLines.add(createActionStatement(
                    variableName, recyclerViewChildPosition, "replaceText(" + boxString(action.getExtraInfo()) + ")" + closeSoftKeyboardAction, action.getWidget().isSonOfScrollable()));
        } else if (action.getActionType() == ActionType.ENTER) {
            // do nothing, since this is handled solely by the Espresso "replaceText" command.
        } else {
            throw new RuntimeException("Unsupported event type: " + action.getActionType());
        }

        if (doesNeedStandaloneCloseSoftKeyboardAction(action)) {
            addStandaloneCloseSoftKeyboardAction(action, testCodeLines);
        }
    }

    private void addStandaloneCloseSoftKeyboardAction(Action action, List<String> testCodeLines) {
        // Simulate an artificial close soft keyboard event.
        Action closeSoftKeyboardAction = new Action(action.getWidget(), action.getActionType());

        testCodeLines.add("");
        String variableName = addPickingStatement(closeSoftKeyboardAction, testCodeLines);
        testCodeLines.add(createActionStatement(variableName, closeSoftKeyboardAction.getWidget().getRecyclerViewChildPosition(), getCloseSoftKeyboard(), false));
    }

    private boolean doesNeedStandaloneCloseSoftKeyboardAction(Action action) {
        // Make text edit in a RecyclerView child always require a standalone close soft keyboard action since actionOnItemAtPosition
        // accepts only a single action.
        return mUseTextForElementMatching && action.getActionType() == ActionType.TYPE_TEXT
                && (!isNullOrEmpty(action.getExtraInfo()) || action.getWidget().getRecyclerViewChildPosition() != -1);
    }

    private boolean isSwipeAction(Action action){
        return action.getSwipe()!=null;
    }

    private String getRootPickingStatement(Action action, List<String> testCodeLines){
        String variableName = generateVariableNameFromElementClassName("root", VIEW_VARIABLE_CLASS_NAME);
        testCodeLines.add("ViewInteraction " + variableName +  " = onView("+ getIsRootMatcher() +")" + getStatementTerminator());
        return variableName;
    }

    private String addPickingStatement(Action action, List<String> testCodeLines) {
        if (isSwipeAction(action)){
            return getRootPickingStatement(action, testCodeLines);
        }
        else if (isAdapterViewAction(action)) {
            return addDataPickingStatement(action, testCodeLines);
        }


        //1- refine action according receiver of action according to coordenates
        refineReceiverOfAction(action);

        String variableName = addViewPickingStatement(action, testCodeLines);
        String statement = testCodeLines.get(testCodeLines.size() - 1);

        //2- check children to be more specific
        /**
         * Search for a childs with any? of the following
         *  - id
         *  - text
         *  - content description
         *
         * For every (wich? all? how to tell when to stop and wich is better?) child found, add it to last picking statement
         * **/


        //3- check parent to be more specific
        /**
         * Search for parents
         *  - id
         *  - text
         *  - content description
         *
         * Use this info, or if any of the parents has specific position on a view use that info
         * **/


        ActionType actionType = action.getActionType();
        Widget target = action.getWidget();

        // check if this view pick statement is too unspecific.
        // If so, try to start the creation of the statement with a more specific children
        Widget childrenWithSomeText = target.getChildrenWithContentDescriptionOrText();
        Widget childrenWithRId = target.getChildrenWithRId();

        if (childrenWithSomeText != null &&
                !statement.contains("withContentDescription") &&
                !statement.contains("withText")) {
            // there as a child with some text to make the statement more specific

            testCodeLines.remove(testCodeLines.size() - 1);
            variableName = addViewPickingStatement(new Action(childrenWithSomeText, actionType), testCodeLines);

        } else if (childrenWithRId != null &&
                !statement.contains("R.id")) {
            // there is a child with R.id to make the statement more specific

            testCodeLines.remove(testCodeLines.size() - 1);
            variableName = addViewPickingStatement(new Action(childrenWithRId, actionType), testCodeLines);

        } else if (!target.getChildren().isEmpty() &&
                !statement.contains("withContentDescription") &&
                !statement.contains("withText") &&
                !statement.contains("R.id")) {
            // there is a child that might make this statement more specific

            while (!target.getChildren().isEmpty()) {
                target = Randomness.randomElement(target.getChildren());
            }

            testCodeLines.remove(testCodeLines.size() - 1);
            variableName = addViewPickingStatement(new Action(target, actionType), testCodeLines);
        }

        // System.out.println("Statement post rewrite: " + testCodeLines.get(0));

        return variableName;
    }

    private void refineReceiverOfAction(Action action) {
        Widget receiverOfAction = action.getWidget().getReceiverOfClickInCoordinates(action.getWidget().getX(), action.getWidget().getY());
        if (receiverOfAction == null) {
            throw new RuntimeException("there is no receiver of click action for widget");
        }
        action.setWidget(receiverOfAction);
    }

    private String addDataPickingStatement(Action action, List<String> testCodeLines) {
        String variableName = generateVariableNameFromElementClassName(action.getWidget().getClazz(), DATA_VARIABLE_CLASS_NAME);
        // TODO: Add '.onChildView(...)' when we support AdapterView beyond the immediate parent of the affected element.
        testCodeLines.add(getVariableTypeDeclaration(false) + " " + variableName + " = onData(anything())\n.inAdapterView(" +
                generateElementHierarchyConditions(action, 1) + ")\n.atPosition(" + action.getWidget().getAdapterViewChildPosition() +
                ")" + getStatementTerminator());
        return variableName;
    }

    private String getVariableTypeDeclaration(boolean isOnViewInteraction) {
        if (mIsKotlinTestClass) {
            return "val";
        }
        return isOnViewInteraction ? VIEW_VARIABLE_CLASS_NAME : DATA_VARIABLE_CLASS_NAME;
    }

    // TODO: This will not detect an adapter view action if the affected element's immediate parent is not an AdapterView
    // (e.g., clicking on a button, whose parent's parent is AdapterView will not be detected as an AdapterView action).
    private static boolean isAdapterViewAction(Action action) {
        return action.getWidget().getAdapterViewChildPosition() != -1 && action.getWidget().getParent() != null;
    }

    private String addViewPickingStatement(Action action, List<String> testCodeLines) {
        // Skip a level for RecyclerView children as they will be identified through their position.
        int startIndex = action.getWidget().getRecyclerViewChildPosition() != -1 && action.getWidget().getParent() != null ? 1 : 0;

        String variableClassName = startIndex == 0 ? action.getWidget().getClazz() : action.getWidget().getParent().getClazz();
        String variableName = generateVariableNameFromElementClassName(variableClassName, VIEW_VARIABLE_CLASS_NAME);
        String viewMatchers = generateElementHierarchyConditions(action, startIndex);

        if (getIsDisplayedMatcher().equals(viewMatchers)) {
            // this means that the action has an empty widget as a target
            viewMatchers = getIsRootMatcher();
        }

        testCodeLines.add(getVariableTypeDeclaration(true) + " " + variableName + " = onView(" +
                viewMatchers + ")" + getStatementTerminator());

        return variableName;
    }

    private String generateVariableNameFromElementClassName(String elementClassName, String defaultClassName) {
        if (isNullOrEmpty(elementClassName)) {
            return generateVariableNameFromTemplate(defaultClassName);
        }
        return generateVariableNameFromTemplate(elementClassName);
    }

    private String generateVariableNameFromTemplate(String template) {
        template = template.replace(".", "_");
        String variableName = Character.toLowerCase(template.charAt(0)) + template.substring(1);
//        if (JavaLexer.isKeyword(variableName, LanguageLevel.HIGHEST)) {
//            variableName += "_";
//        }

        Integer unusedIndex = mVariableNameIndexes.get(variableName);
        if (unusedIndex == null) {
            mVariableNameIndexes.put(variableName, 2);
            return variableName;
        }

        mVariableNameIndexes.put(variableName, unusedIndex + 1);
        return variableName + unusedIndex;
    }

    private String generateElementHierarchyConditions(Action action, int startIndex) {
        // remove widgets in the hierarchy until we reach the desired index
        Widget widget = action.getWidget();
        while (startIndex > 0) {
            widget = widget.getParent();
            startIndex--;

            // the widget hierarchy is not as deep as the desired startIndex
            if (widget == null) {
                return "UNKNOWN";
            }
        }
        return generateElementHierarchyConditionsRecursively(widget, !widget.isSonOfScrollable(), startIndex);
    }

    private String generateElementHierarchyConditionsRecursively(Widget widget, boolean checkIsDisplayed, int index) {
        // Add isDisplayed() only to the innermost element.
        boolean addIsDisplayed = checkIsDisplayed && index == 0;
        MatcherBuilder matcherBuilder = new MatcherBuilder();

        //que significa que sea vacio un widget?
        if (isEmpty(widget)
                // Cannot use child position for the last element, since no parent descriptor available.
                || widget.getParent() == null && isEmptyIgnoringChildPosition(widget)
                || index == 0 && isLoginRadioButton(widget)) {
            matcherBuilder.addMatcher(ClassName, widget.getClazz(), true, false);
            mIsclassOrSuperClassesNameAdded = true;
        } else {
            // Do not use android framework ids that are not visible to the compiler.
            String resourceId = widget.getResourceID();
            if (isAndroidFrameworkPrivateId(resourceId)) {
                matcherBuilder.addMatcher(ClassName, widget.getClazz(), true, false);
                mIsclassOrSuperClassesNameAdded = true;
            } else {
                matcherBuilder.addMatcher(Id, convertIdToTestCodeFormat(resourceId), false, false);
            }

            if (mUseTextForElementMatching) {
                matcherBuilder.addMatcher(Text, widget.getText(), true, false);
            }

            matcherBuilder.addMatcher(ContentDescription, widget.getContentDesc(), true, false);
        }

        // TODO: Consider minimizing the generated statement to improve test's readability and maintainability (e.g., by capping parent hierarchy).

        // The last element has no parent.
        if (widget.getParent() == null || index > MAX_HIERARCHY_VIEW_LEVEL) {
            if (matcherBuilder.getMatcherCount() > 1 || addIsDisplayed) {
                String matchers = matcherBuilder.getMatchers();
                if (!matchers.isEmpty()) {
                    return "allOf(" + matchers + (addIsDisplayed ? ", " + getIsDisplayedMatcher() : "") + ")";
                } else {
                    return addIsDisplayed ? getIsDisplayedMatcher() : "";
                }
            }
            return matcherBuilder.getMatchers();
        }

        boolean addAllOf = matcherBuilder.getMatcherCount() > 0 || addIsDisplayed;
        int groupViewChildPosition = widget.getGroupViewChildPosition();

        // Do not use child position for ViewPager children as it changes dynamically and non-deterministically.
        if (CLASS_VIEW_PAGER.equals(widget.getParent().getClazz())) {
            groupViewChildPosition = -1;
        }

        mIsChildAtPositionAdded = mIsChildAtPositionAdded || groupViewChildPosition != -1;

        return (addAllOf ? "allOf(" : "") + matcherBuilder.getMatchers() + (matcherBuilder.getMatcherCount() > 0 ? "," : "")
                + (groupViewChildPosition != -1 ? "childAtPosition(" : "withParent(")
                + generateElementHierarchyConditionsRecursively(widget.getParent(), checkIsDisplayed, index + 1)
                + (groupViewChildPosition != -1 ? ",\n" + groupViewChildPosition : "") + ")"
                + (addIsDisplayed ? ",\n" + getIsDisplayedMatcher() : "") + (addAllOf ? ")" : "");
    }


    private boolean isAndroidFrameworkPrivateId(String resourceId) {
        Tuple<String, String> parsedId = parseId(resourceId);
        return parsedId != null && "android".equals(parsedId.getX());
    }

    private String convertIdToTestCodeFormat(String resourceId) {
        Tuple<String, String> parsedId = parseId(resourceId);

        if (parsedId == null) {
            // Parsing failed, return the raw id.
            return resourceId;
        }

        String testCodeId = "R.id." + parsedId.getY();
//    if (!parsedId.getFirst().equals(mApplicationId)) {
//      // Only the app's resource package will be explicitly imported, so use a fully qualified id for other packages.
//      testCodeId = parsedId.getFirst() + "." + testCodeId;
//    }

        return testCodeId;
    }

    private String createActionStatement(String variableName, int recyclerViewChildPosition, String action, boolean addScrollTo) {
        mIsRecyclerViewActionAdded = mIsRecyclerViewActionAdded || recyclerViewChildPosition != -1;

        // No need to explicitly scroll to perform an action on a RecyclerView child.
        String completeAction = (addScrollTo && recyclerViewChildPosition == -1 ? getScrollToAction() + ", " : "") + action;
        completeAction = recyclerViewChildPosition == -1
                ? completeAction
                : getActionOnItemAtPositionMethodCallPrefix() + recyclerViewChildPosition + ", " + completeAction + ")";

        String performStatement = variableName + ".perform(" + completeAction + ")" + getStatementTerminator();

        if (mSurroundPerformsWithTryCatch) {
            performStatement = surroundPerformWithTryCatch(performStatement);
        }

        return performStatement;
    }



    private String surroundPerformWithTryCatch(String performStatement) {
        performStatement = "\ntry {\n" +
                performStatement + "\n" +
                "} catch (Exception e) {\n" +
                "System.out.println(buildPerformExceptionMessage(e, " + performCount + "))" + getStatementTerminator() + "\n" +
                "}\n";

        performCount++;

        return performStatement;
    }

    private String getActionOnItemAtPositionMethodCallPrefix() {
        return mIsKotlinTestClass ? "actionOnItemAtPosition<ViewHolder>(" : "actionOnItemAtPosition(";
    }

    /**
     * TODO: This is a temporary workaround for picking a login option in a username-agnostic way
     * such that the generated test is generic enough to run on other devices.
     * TODO: Also, it assumes a single radio button choice (such that it could be identified by the class name).
     */
    private boolean isLoginRadioButton(Widget widget) {
        return widget.getClazz().endsWith(".widget.AppCompatRadioButton")
                && "R.id.welcome_account_list".equals(convertIdToTestCodeFormat(widget.getParent().getResourceID()));
    }

    public boolean isEmptyIgnoringChildPosition(Widget widget) {
        return isNullOrEmpty(widget.getResourceID()) && isNullOrEmpty(widget.getText())
                && isNullOrEmpty(widget.getContentDesc());
    }

    public boolean isEmpty(Widget widget) {
        return widget.getRecyclerViewChildPosition() == -1 && widget.getAdapterViewChildPosition() == -1 && widget.getGroupViewChildPosition() == -1
                && isEmptyIgnoringChildPosition(widget);
    }

    public boolean isChildAtPositionAdded() {
        return mIsChildAtPositionAdded;
    }

    public boolean isRecyclerViewActionAdded() {
        return mIsRecyclerViewActionAdded;
    }

    private String getStatementTerminator() {
        return mIsKotlinTestClass ? "" : ";";
    }

    public boolean isClassOrSuperClassesNameAdded() {
        return mIsclassOrSuperClassesNameAdded;
    }

    public boolean isTryCatchAdded() {
        return mSurroundPerformsWithTryCatch;
    }

    public boolean isClickActionAdded() {
        return clickActionAdded;
    }

    public boolean isLongClickActionAdded() {
        return longClickActionAdded;
    }

    public boolean isSwipeActionAdded() {
        return swipeActionAdded;
    }

    /**
     * actions and matchers. eventually move to another class
     * **/

    private String getLongClickAction() {
        return "getLongClickAction()";
    }

    private String getClickViewAction() {
        return "getClickAction()";
    }

    private String getSwipeAction(Swipe swipe) {
        String methdCall = "getSwipeAction($fromX, $fromY, $toX, $toY)";
        methdCall =
                methdCall
                        .replace("$fromX", String.valueOf(swipe.getInitialPosition().x))
                        .replace("$fromY", String.valueOf(swipe.getInitialPosition().y))
                        .replace("$toX", String.valueOf(swipe.getFinalPosition().x))
                        .replace("$toY", String.valueOf(swipe.getFinalPosition().y));
        return methdCall;
    }

    private String getPressMenuKeyAction() {
        return "pressMenuKey()";
    }

    private String getScrollToAction() {
        return "scrollTo()";
    }

    private String getCloseSoftKeyboard() {
        return "closeSoftKeyboard()";
    }

    private String getWaitToScrollEndStatement() {
        return "waitToScrollEnd()";
    }

    private String getIsDisplayedMatcher() {
        return "isDisplayed()";
    }

    private String getIsRootMatcher() {
        return "isRoot()";
    }

}
