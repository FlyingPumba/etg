package org.etg.espresso.codegen.viewPicking;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.stmt.Statement;
import org.etg.ETGProperties;
import org.etg.espresso.codegen.TestCodeMapper;
import org.etg.espresso.codegen.actions.ActionCodeMapper;
import org.etg.mate.models.Action;
import org.etg.mate.models.Widget;
import org.etg.utils.Tuple;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.etg.espresso.codegen.viewPicking.MatcherBuilder.Kind.*;
import static org.etg.espresso.util.StringHelper.isNullOrEmpty;
import static org.etg.espresso.util.StringHelper.parseId;

public class ViewPickingStatementGenerator extends ActionCodeMapper {

    private static final String VIEW_VARIABLE_CLASS_NAME = "ViewInteraction";

    public ViewPickingStatementGenerator(ETGProperties etgProperties, Action action) {
        super(etgProperties, action);
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
    public String addTestCodeLines(List<String> testCodeLines, TestCodeMapper testCodeMapper, int actionIndex, int actionsCount) {
        if (isSwipeAction(action)) {
            return getRootPickingStatement(action, testCodeLines, testCodeMapper);
        }

        //1- Create a basic view picking statement using info from target widget.
        String variableName = createViewPickingStatement(testCodeLines, testCodeMapper);
        String statement = testCodeLines.get(testCodeLines.size() - 1);

        //parse statement as AST, find first allOf expression or add it if missing
        Statement parsedStatement = StaticJavaParser.parseStatement(statement);

        //2- check children to be more specific
        (new ImproverWithChildrenInfo(etgProperties)).improveStatementWithChildrensOf(action.getWidget(), parsedStatement);

        //3- check parent to be more specific
        (new ImproverWithParentInfo(etgProperties)).improveStatementWithParentsOf(action.getWidget(), parsedStatement);

        //update last statement with improved statement
        testCodeLines.remove(testCodeLines.size() - 1);
        String improvedStatementString = parsedStatement.toString();

        if(etgProperties.useKotlinFormat()) {
            // convert Java statement into Kotlin format
            List<String> words = new ArrayList<>(Arrays.asList(improvedStatementString.split(" ")));
            // remove type
            words.remove(0);
            // add "val" declaration
            words.add(0, "val");
            // join all together again
            improvedStatementString = String.join(" ", words);
        }

        testCodeLines.add(improvedStatementString);

        return variableName;
    }

    private boolean isSwipeAction(Action action) {
        return action.getSwipe() != null;
    }

    private String createViewPickingStatement(List<String> testCodeLines, TestCodeMapper testCodeMapper) {
        String variableClassName = this.action.getWidget().getClazz();

        String variableName = generateVariableNameFromElementClassName(variableClassName, VIEW_VARIABLE_CLASS_NAME, testCodeMapper);
        String viewMatchers = generateBasicPickingStatement(action.getWidget(), testCodeMapper);

        if (getIsVisibleMatcher().equals(viewMatchers)) {
            // this means that the action has an empty widget as a target
            viewMatchers = getIsRootMatcher();
        }

        testCodeLines.add(getVariableTypeDeclaration(testCodeMapper) + " " + variableName + " = onView(" +
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

    private String generateBasicPickingStatement(Widget widget, TestCodeMapper testCodeMapper) {
        MatcherBuilder matcherBuilder = new MatcherBuilder(etgProperties);

        if (isEmpty(widget)) {
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

            // Content description tends to give problems, use only if there ire no other options
            if(matcherBuilder.getMatcherCount() == 0) {
                matcherBuilder.addMatcher(ContentDescription, widget.getContentDesc(), true, false);
            }
        }

        String matchers = matcherBuilder.getMatchers();
        if (!matchers.isEmpty()) {
            return "allOf(" + matchers + ", " + getIsVisibleMatcher() + ")";
        } else {
            return getIsVisibleMatcher();
        }
    }

    private boolean isAndroidFrameworkPrivateId(String resourceId) {
        Tuple<String, String> parsedId = parseId(resourceId);
        return parsedId != null && "android".equals(parsedId.getX());
    }

    private String getRootPickingStatement(Action action, List<String> testCodeLines, TestCodeMapper testCodeMapper) {
        String variableName = generateVariableNameFromElementClassName("root", VIEW_VARIABLE_CLASS_NAME, testCodeMapper);
        if (etgProperties.useKotlinFormat()) {
            testCodeLines.add("val " + variableName + " = onView(" + getIsRootMatcher() + ")");
        } else {
            testCodeLines.add("ViewInteraction " + variableName + " = onView(" + getIsRootMatcher() + ")" + testCodeMapper.getStatementTerminator());
        }
        return variableName;
    }

    private String getVariableTypeDeclaration(TestCodeMapper testCodeMapper) {
        return VIEW_VARIABLE_CLASS_NAME;
    }

    public boolean isEmpty(Widget widget) {
        return isNullOrEmpty(widget.getResourceID()) && isNullOrEmpty(widget.getText())
                && isNullOrEmpty(widget.getContentDesc());
    }

    private String getIsVisibleMatcher() {
        return "\nisVisible()";
    }

    public static String getIsRootMatcher() {
        return "isRoot()";
    }
}
