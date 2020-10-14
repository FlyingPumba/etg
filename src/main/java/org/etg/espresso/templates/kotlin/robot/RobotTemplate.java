package org.etg.espresso.templates.kotlin.robot;

import org.etg.espresso.templates.VelocityTemplate;
import org.etg.mate.models.Action;
import org.etg.mate.models.ActionType;

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
                "import ${PackageName}.utils.IsEqualTrimmingAndIgnoringCase.Companion.equalToTrimmingAndIgnoringCase\n" +
                "import ${PackageName}.utils.VisibleViewMatcher.Companion.isVisible\n" +
                "import ${PackageName}.utils.EspressoUtils.Companion.getClickAction\n" +
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
        String resourceID = action.getWidget().getResourceID();
        if (resourceID == null || resourceID.isEmpty()) {
            String name = "unkownAction" + unknownActionNumber;
            unknownActionNumber++;
            return name;
        }

        String actualWidgetName = resourceID.split("/")[1];

        return lowerCaseFirstCharacter(upperCaseParts(actualWidgetName, "_"));
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
}
