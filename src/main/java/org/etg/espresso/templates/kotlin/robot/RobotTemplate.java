package org.etg.espresso.templates.kotlin.robot;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import org.etg.espresso.codegen.viewPicking.ViewPickingStatementGenerator2;
import org.etg.espresso.templates.VelocityTemplate;
import org.etg.mate.models.Action;
import org.etg.mate.models.ActionType;
import org.etg.mate.models.Widget;

import java.util.*;

import static org.etg.espresso.util.StringHelper.*;

public class RobotTemplate implements VelocityTemplate {

    class RobotTemplateMethod {
        public final String methodName;
        public final Action action;
        public final List<String> lines;

        RobotTemplateMethod(String methodName, Action action, List<String> lines) {
            this.methodName = methodName;
            this.action = action;
            this.lines = lines;
        }
    }

    private static int unknownActionNumber = 0;
    private final String robotName;
    private final Map<String, RobotTemplateMethod> methods = new HashMap<>();

    public RobotTemplate(String robotName) {
        this.robotName = robotName;
    }

    @Override
    public String getFileName() {
        return String.format("%s.kt", getRobotName());
    }

    @Override
    public String getRelativePath() {
        return "robot/";
    }

    public String getRobotName() { return buildRobotName(robotName); }

    public String getRobotScreenName() { return buildRobotScreenName(robotName); }

    public static String buildRobotName(String robotName) { return String.format("%sRobot", robotName); }

    public static String buildRobotScreenName(String robotName) { return String.format("%sScreen", lowerCaseFirstCharacter(robotName)); }

    public String getAsRawString() {
        return "package ${PackageName}.robot\n" +
                "\n" +
                "import ${EspressoPackageName}.espresso.Espresso.*\n" +
                "import ${EspressoPackageName}.espresso.action.ViewActions.*\n" +
                "import ${EspressoPackageName}.espresso.matcher.ViewMatchers.*\n" +
                "import ${PackageName}.R\n" +
                "import ${PackageName}.utils.ClassOrSuperClassNameMatcher.Companion.classOrSuperClassesName\n" +
                "import ${PackageName}.utils.IsEqualTrimmingAndIgnoringCase.Companion.equalToTrimmingAndIgnoringCase\n" +
                "import ${PackageName}.utils.IsImmediateDescendantOfAMatcher.Companion.isImmediateDescendantOfA\n" +
                "import ${PackageName}.utils.VisibleViewMatcher.Companion.isVisible\n" +
                "import ${PackageName}.utils.EspressoUtils.Companion.getClickAction\n" +
                "import ${PackageName}.utils.EspressoUtils.Companion.getLongClickAction\n" +
                "import ${PackageName}.utils.EspressoUtils.Companion.withTextOrHint\n" +
                "import org.hamcrest.Matchers.*\n" +
                "\n" +
                String.format("fun %s(func: %s.() -> Unit) = %s().apply { func() }\n",
                        getRobotScreenName(), getRobotName(), getRobotName()) +
                "\n" +
                String.format("class %s : ScreenRobot() {\n", getRobotName()) +
                this.getMethodsString() +
                "}";
    }


    @Override
    public boolean equals(Object o) {
        return o instanceof RobotTemplate;
    }

    @Override
    public int hashCode() {
        return getAsRawString().hashCode();
    }

    public String addMethod(Action action, List<String> lines) {
        String methodName = getMethodNameForAction(action);
        if (methods.containsKey(methodName)) {
            return methodName;
        }

        methods.put(methodName, new RobotTemplateMethod(methodName, action, lines));
        return methodName;
    }

    private String getMethodNameForAction(Action action) {
        String methodName = "";
        Widget targetWidget = action.getWidget();

        // is Matcher combination available?
        ViewPickingStatementGenerator2.MatcherCombination matcherCombination =
                ViewPickingStatementGenerator2.reducedMatcherCombinationsForWidget.get(targetWidget);
        if (matcherCombination != null) {
            methodName = buildMethodNameFromMatcherCombination(matcherCombination, targetWidget);
            if (!methodName.isEmpty()) {
                return methodName;
            }
        }

        // can we use Resource Id?
        methodName = parseResourceIdForMethodName(targetWidget);
        if (!methodName.isEmpty()) {
            return methodName;
        }

        // can we use the Content Description?
        methodName = parseContentDescriptionForMethodName(targetWidget);
        if (!methodName.isEmpty()) {
            return methodName;
        }

        // mark this method as an unknown action
        String name = "unkownAction" + unknownActionNumber;
        unknownActionNumber++;
        return name;
    }

    private String getMethodsString() {
        Set<String> methodNames = methods.keySet();
        StringBuilder code = new StringBuilder();
        for (String name: methodNames) {
            String str = this.getMethodString(name);
            code.append(str);
        }

        return code.toString();
    }

    private String getMethodString(String name) {
        RobotTemplateMethod method = methods.get(name);

        StringBuilder methodBody;
        String valueToReplace = "";
        if (method.action.getActionType() == ActionType.TYPE_TEXT) {
            valueToReplace = boxString(method.action.getExtraInfo());
            methodBody = new StringBuilder(String.format("    fun %s(value: String): %s {\n", name, getRobotName()));
        } else {
            methodBody = new StringBuilder(String.format("    fun %s(): %s {\n", name, getRobotName()));
        }

        for (String line: method.lines) {
            if (!valueToReplace.isEmpty()) {
                line = line.replace(valueToReplace, "value");
            }
            methodBody.append(String.format("        %s\n", line));
        }

        methodBody.append("        return this\n")
                .append("    }\n")
                .append("\n");

        return methodBody.toString();
    }

    private String buildMethodNameFromMatcherCombination(
            ViewPickingStatementGenerator2.MatcherCombination matcherCombination,
            Widget targetWidget
    ) {
        StringBuilder methodName = new StringBuilder();

        for (ViewPickingStatementGenerator2.MatcherForRelativeWidget matcher : matcherCombination.getMatchers()) {
            Widget widgetToMatch = matcher.getRelativePath().isEmpty()?
                    targetWidget : targetWidget.getWidgetByRelativePath(matcher.getRelativePath());

            String methodNamePart = "";
            if (matcher.getType().equals(ViewPickingStatementGenerator2.MatcherType.ResourceId)) {
                methodNamePart = parseResourceIdForMethodName(widgetToMatch);
            } else if (matcher.getType().equals(ViewPickingStatementGenerator2.MatcherType.Clazz)) {
                methodNamePart = parseClassNameForMethodName(widgetToMatch);
            } else if (matcher.getType().equals(ViewPickingStatementGenerator2.MatcherType.ContentDescription)) {
                methodNamePart = parseContentDescriptionForMethodName(widgetToMatch);
            } else if (matcher.getType().equals(ViewPickingStatementGenerator2.MatcherType.Text)) {
                methodNamePart = parseTextOrHintForMethodName(widgetToMatch);
            }

            if (methodName.length() > 0) {
                methodName.append(upperCaseFirstCharacter(methodNamePart));
            } else {
                methodName.append(methodNamePart);
            }
        }

        return methodName.toString();
    }

    private String parseResourceIdForMethodName(Widget widgetToMatch) {
        String resourceID = widgetToMatch.getResourceID();
        if (resourceID != null && !resourceID.isEmpty()) {
            String actualWidgetName = resourceID.split("/")[1];
            return lowerCaseFirstCharacter(upperCaseParts(actualWidgetName, "_"));
        }

        return "";
    }

    private String parseClassNameForMethodName(Widget widgetToMatch) {
        String className = widgetToMatch.getClazz();
        if (className != null && !className.isEmpty()) {
            return lowerCaseFirstCharacter(upperCaseParts(className, "."));
        }

        return "";
    }

    private String parseContentDescriptionForMethodName(Widget widgetToMatch) {
        String contentDesc = widgetToMatch.getContentDesc();
        if (contentDesc != null && !contentDesc.isEmpty()) {
            return lowerCaseFirstCharacter(upperCaseParts(contentDesc, " "));
        }

        return "";
    }

    private String parseTextOrHintForMethodName(Widget widgetToMatch) {
        String methodName = "";

        String text = widgetToMatch.getText();
        if (text != null && !text.isEmpty()) {
            String aux = removeSpecialCharacters(text);
            if (aux.length() > 2) {
                methodName = lowerCaseFirstCharacter(upperCaseParts(aux, " "));
            } else {
                methodName = aux.toLowerCase();
            }
        }

        String hint = widgetToMatch.getHint();
        if (hint != null && !hint.isEmpty()) {
            String aux = removeSpecialCharacters(hint);
            if (aux.length() > 2) {
                methodName += lowerCaseFirstCharacter(upperCaseParts(aux, " "));
            } else {
                methodName += aux.toLowerCase();
            }
        }

        return methodName;
    }

    private String removeSpecialCharacters(String value) {
        // remove special symbols
        value = value.replace("$", "");
        value = value.replace("(", "");
        value = value.replace(")", "");
        value = value.replace("?", "");
        value = value.replace("¿", "");
        value = value.replace("¡", "");
        value = value.replace("!", "");
        value = value.replace("-", "");
        value = value.replace("_", "");

        // remove characters with tilde
        value = value.replace("á", "a");
        value = value.replace("é", "e");
        value = value.replace("í", "i");
        value = value.replace("ó", "o");
        value = value.replace("ú", "u");

        return value;
    }
}
