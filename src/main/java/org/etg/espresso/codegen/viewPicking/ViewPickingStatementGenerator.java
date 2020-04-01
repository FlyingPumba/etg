package org.etg.espresso.codegen.viewPicking;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.stmt.Statement;
import org.etg.ETGProperties;
import org.etg.espresso.codegen.TestCodeMapper;
import org.etg.espresso.codegen.actions.ActionCodeMapper;
import org.etg.mate.models.Action;
import org.etg.mate.models.Widget;
import org.etg.utils.Tuple;

import java.util.List;

import static org.etg.espresso.codegen.viewPicking.ImproverWithChildrenInfo.improveStatementWithChildrensOf;
import static org.etg.espresso.codegen.viewPicking.ImproverWithParentInfo.improveStatementWithParentsOf;
import static org.etg.espresso.codegen.viewPicking.MatcherBuilder.Kind.*;
import static org.etg.espresso.util.StringHelper.isNullOrEmpty;
import static org.etg.espresso.util.StringHelper.parseId;

public class ViewPickingStatementGenerator extends ActionCodeMapper {

    private static final int MAX_HIERARCHY_VIEW_LEVEL = 2;
    private static final String VIEW_VARIABLE_CLASS_NAME = "ViewInteraction";
    private static final String DATA_VARIABLE_CLASS_NAME = "DataInteraction";
    private static final String CLASS_VIEW_PAGER = "android.support.v4.view.ViewPager";

    public ViewPickingStatementGenerator(ETGProperties etgProperties, Action action) {
        super(etgProperties, action);
    }

    // TODO: This will not detect an adapter view action if the affected element's immediate parent is not an AdapterView
    // (e.g., clicking on a button, whose parent's parent is AdapterView will not be detected as an AdapterView action).
    private static boolean isAdapterViewAction(Action action) {
        return action.getWidget().getAdapterViewChildPosition() != -1 && action.getWidget().getParent() != null;
    }

    public static String convertIdToTestCodeFormat(String resourceId) {
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

        testCodeId = testCodeId.split("-")[0];//converts com.pkg:id/anId-child-3:android.widget.FrameLayout

        return testCodeId;
    }

    @Override
    public String addTestCodeLines(List<String> testCodeLines, TestCodeMapper testCodeMapper) {
        if (isSwipeAction(action)) {
            return getRootPickingStatement(action, testCodeLines, testCodeMapper);
        } else if (isAdapterViewAction(action)) {
            return addDataPickingStatement(action, testCodeLines, testCodeMapper);
        }

        //1- refine action according receiver of action according to coordenates
        refineReceiverOfAction(action);
        String variableName = createViewPickingStatement(testCodeLines, testCodeMapper);
        String statement = testCodeLines.get(testCodeLines.size() - 1);

        //parse statement as AST, find first allOf expression or add it if missing
        Statement parsedStatement = StaticJavaParser.parseStatement(statement);

        //2- check children to be more specific
        improveStatementWithChildrensOf(action.getWidget(), parsedStatement);

        //3- check parent to be more specific
        improveStatementWithParentsOf(action.getWidget(), parsedStatement);

        //update last statement with improved statement
        testCodeLines.remove(testCodeLines.size() - 1);
        String improvedStatementString = parsedStatement.toString();
        testCodeLines.add(improvedStatementString);

        return variableName;
    }

    private boolean isSwipeAction(Action action) {
        return action.getSwipe() != null;
    }

    private String createViewPickingStatement(List<String> testCodeLines, TestCodeMapper testCodeMapper) {
        // Skip a level for RecyclerView children as they will be identified through their position.
        int startIndex = this.action.getWidget().getRecyclerViewChildPosition() != -1 && this.action.getWidget().getParent() != null ? 1 : 0;

        String variableClassName = startIndex == 0 ? this.action.getWidget().getClazz() : this.action.getWidget().getParent().getClazz();
        String variableName = generateVariableNameFromElementClassName(variableClassName, VIEW_VARIABLE_CLASS_NAME, testCodeMapper);
        String viewMatchers = generateElementHierarchyConditions(this.action, startIndex, testCodeMapper);

        if (getIsDisplayedMatcher().equals(viewMatchers)) {
            // this means that the action has an empty widget as a target
            viewMatchers = getIsRootMatcher();
        }

        testCodeLines.add(getVariableTypeDeclaration(true, testCodeMapper) + " " + variableName + " = onView(" +
                viewMatchers + ")" + testCodeMapper.getStatementTerminator());

        return variableName;
    }

    private String generateVariableNameFromElementClassName(String elementClassName, String defaultClassName, TestCodeMapper testCodeMapper) {
        if (isNullOrEmpty(elementClassName)) {
            return generateVariableNameFromTemplate(defaultClassName, testCodeMapper);
        }
        return generateVariableNameFromTemplate(elementClassName, testCodeMapper);
    }

    private String generateVariableNameFromTemplate(String template, TestCodeMapper testCodeMapper) {
        template = template.replace(".", "_");
        String variableName = Character.toLowerCase(template.charAt(0)) + template.substring(1);
//        if (JavaLexer.isKeyword(variableName, LanguageLevel.HIGHEST)) {
//            variableName += "_";
//        }

        Integer unusedIndex = testCodeMapper.mVariableNameIndexes.get(variableName);
        if (unusedIndex == null) {
            testCodeMapper.mVariableNameIndexes.put(variableName, 2);
            return variableName;
        }

        testCodeMapper.mVariableNameIndexes.put(variableName, unusedIndex + 1);
        return variableName + unusedIndex;
    }

    private String generateElementHierarchyConditions(Action action, int startIndex, TestCodeMapper testCodeMapper) {
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
        return generateElementHierarchyConditionsRecursively(widget, startIndex, testCodeMapper);
    }

    private String generateElementHierarchyConditionsRecursively(Widget widget, int index, TestCodeMapper testCodeMapper) {
        // Add isDisplayed only to the innermost element.
        boolean addIsDisplayed = index == 0;
        MatcherBuilder matcherBuilder = new MatcherBuilder();

        if (isEmpty(widget)
                // Cannot use child position for the last element, since no parent descriptor available.
                || widget.getParent() == null && isEmptyIgnoringChildPosition(widget)
                || index == 0 && isLoginRadioButton(widget)) {
            matcherBuilder.addMatcher(ClassName, widget.getClazz(), true, false);
            testCodeMapper.mIsclassOrSuperClassesNameAdded = true;
        } else {
            // Do not use android framework ids that are not visible to the compiler.
            String resourceId = widget.getResourceID();
            if (isAndroidFrameworkPrivateId(resourceId)) {
                matcherBuilder.addMatcher(ClassName, widget.getClazz(), true, false);
                testCodeMapper.mIsclassOrSuperClassesNameAdded = true;
            } else {
                matcherBuilder.addMatcher(Id, convertIdToTestCodeFormat(resourceId), false, false);
            }

            if (testCodeMapper.mUseTextForElementMatching &&
                    !"android.widget.Switch".equals(widget.getClazz())) {
                matcherBuilder.addMatcher(Text, widget.getText(), true, false);
            }

            // Content description tends to give problems, we will leave it commented
            // matcherBuilder.addMatcher(ContentDescription, widget.getContentDesc(), true, false);
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

        testCodeMapper.mIsChildAtPositionAdded = testCodeMapper.mIsChildAtPositionAdded || groupViewChildPosition != -1;

        //comento la parte recursiva, dejando solo lo que se macheaba sobre el widget actual
//        return (addAllOf ? "allOf(" : "") + matcherBuilder.getMatchers() + (matcherBuilder.getMatcherCount() > 0 ? "," : "")
//                + (groupViewChildPosition != -1 ? "childAtPosition(" : "withParent(")
//                + generateElementHierarchyConditionsRecursively(widget.getParent(), checkIsDisplayed, index + 1)
//                + (groupViewChildPosition != -1 ? ",\n" + groupViewChildPosition : "") + ")"
//                + (addIsDisplayed ? "\n" + getIsDisplayedMatcher() : "") + (addAllOf ? ")" : "");
        return (addAllOf ? "allOf(" : "") + matcherBuilder.getMatchers() +
                (addIsDisplayed ? ((matcherBuilder.getMatcherCount() > 0 ? ", " : "") + getIsDisplayedMatcher()) : "") +
                (addAllOf ? ")" : "");
    }

    private boolean isAndroidFrameworkPrivateId(String resourceId) {
        Tuple<String, String> parsedId = parseId(resourceId);
        return parsedId != null && "android".equals(parsedId.getX());
    }

    private String getRootPickingStatement(Action action, List<String> testCodeLines, TestCodeMapper testCodeMapper) {
        String variableName = generateVariableNameFromElementClassName("root", VIEW_VARIABLE_CLASS_NAME, testCodeMapper);
        testCodeLines.add("ViewInteraction " + variableName + " = onView(" + getIsRootMatcher() + ")" + testCodeMapper.getStatementTerminator());
        return variableName;
    }

    private void refineReceiverOfAction(Action action) {
        Widget receiverOfAction = action.getWidget().getReceiverOfClickInCoordinates(action.getWidget().getX(), action.getWidget().getY());
        if (receiverOfAction == null) {
            throw new RuntimeException("there is no receiver of click action for widget");
        }
        action.setWidget(receiverOfAction);
    }

    private String addDataPickingStatement(Action action, List<String> testCodeLines, TestCodeMapper testCodeMapper) {
        String variableName = generateVariableNameFromElementClassName(action.getWidget().getClazz(), DATA_VARIABLE_CLASS_NAME, testCodeMapper);
        // TODO: Add '.onChildView(...)' when we support AdapterView beyond the immediate parent of the affected element.
        testCodeLines.add(getVariableTypeDeclaration(false, testCodeMapper) + " " + variableName + " = onData(anything())\n.inAdapterView(" +
                generateElementHierarchyConditions(action, 1, testCodeMapper) + ")\n.atPosition(" + action.getWidget().getAdapterViewChildPosition() +
                ")" + testCodeMapper.getStatementTerminator());
        return variableName;
    }

    private String getVariableTypeDeclaration(boolean isOnViewInteraction, TestCodeMapper testCodeMapper) {
        if (testCodeMapper.mIsKotlinTestClass) {
            return "val";
        }
        return isOnViewInteraction ? VIEW_VARIABLE_CLASS_NAME : DATA_VARIABLE_CLASS_NAME;
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

    private String getIsDisplayedMatcher() {
        return "\nisDisplayed()";
    }

    private String getIsRootMatcher() {
        return "isRoot()";
    }
}
