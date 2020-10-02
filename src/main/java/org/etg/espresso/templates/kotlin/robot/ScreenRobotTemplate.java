package org.etg.espresso.templates.kotlin.robot;

import org.etg.espresso.codegen.actions.*;
import org.etg.espresso.templates.VelocityTemplate;
import org.etg.mate.models.Action;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ScreenRobotTemplate implements VelocityTemplate {

    private final Map<String, List<String>> methods = new HashMap<>();

    @Override
    public String getFileName() {
        return "ScreenRobot.kt";
    }
    
    @Override
    public String getRelativePath() {
        return "robot/";
    }

    public String getAsRawString() {
        return "package ${PackageName}.robot\n" +
                "\n" +
                "import android.view.KeyEvent\n" +
                "import ${EspressoPackageName}.espresso.Espresso\n" +
                "import ${EspressoPackageName}.espresso.Espresso.onView\n" +
                "import ${EspressoPackageName}.espresso.action.ViewActions.*\n" +
                "import ${EspressoPackageName}.espresso.matcher.ViewMatchers.*\n" +
                "import ${PackageName}.utils.EspressoUtils.Companion.getSwipeAction\n" +
                "\n" +
                "open class ScreenRobot {\n" +
                this.getMethodsString() +
                "}";
    }


    @Override
    public boolean equals(Object o) {
        return o instanceof ScreenRobotTemplate;
    }

    @Override
    public int hashCode() {
        return getAsRawString().hashCode();
    }

    private void addMethod(String name, List<String> lines) {
        if(methods.containsKey(name)) {
            return;
        }

        methods.put(name, lines);
    }

    public String addMethod(Action action, List<String> lines) {
        String methodNameForAction = getMethodNameForAction(action);
        addMethod(methodNameForAction, lines);
        return methodNameForAction;
    }

    private String getMethodNameForAction(Action action) {
        switch (action.getActionType()) {
            case SWIPE_UP:
                return "scrollUp";
            case SWIPE_DOWN:
                return "scrollDown";
            case SWIPE_LEFT:
                return "scrollLeft";
            case SWIPE_RIGHT:
                return "scrollRight";
            case BACK:
                return "pressBack";
            case MENU:
                return "pressMenu";
            case ENTER:
                return "pressEnter";
            default:
                throw new RuntimeException("Unsupported event type for Screen Robot: " + action.getActionType());
        }
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
        StringBuilder methodBody = new StringBuilder(String.format("    fun %s(): ScreenRobot {\n", name));

        List<String> lines = methods.get(name);
        for (String line: lines) {
            methodBody.append(String.format("        %s\n", line));
        }

        methodBody.append("        return this\n")
                .append("    }\n")
                .append("\n");

        return methodBody.toString();
    }
}
