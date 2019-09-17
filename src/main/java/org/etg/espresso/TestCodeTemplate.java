package org.etg.espresso;

public class TestCodeTemplate {

    public static String getTemplate() {
        return "#if (${PackageName} && ${PackageName} != \"\")\n" +
                "package ${PackageName};\n" +
                "\n" +
                "#end\n" +
                "\n" +
                "import ${EspressoPackageName}.espresso.Espresso;\n" +
                "import ${EspressoPackageName}.espresso.ViewInteraction;\n" +
                "import ${EspressoPackageName}.espresso.action.ViewActions;\n" +
                "import android.support.test.rule.ActivityTestRule;\n" +
                "import android.support.test.runner.AndroidJUnit4;\n" +
                "import android.support.test.filters.LargeTest;\n" +
                "import android.view.KeyEvent;\n" +
                "import android.view.View;\n" +
                "import android.view.ViewGroup;\n" +
                "import android.view.ViewParent;\n" +
                "\n" +
                "import static ${EspressoPackageName}.InstrumentationRegistry.getInstrumentation;\n" +
                "import static ${EspressoPackageName}.espresso.Espresso.onView;\n" +
                "import static ${EspressoPackageName}.espresso.Espresso.openActionBarOverflowOrOptionsMenu;\n" +
                "#if (${AddContribImport})\n" +
                "import static ${EspressoPackageName}.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition;\n" +
                "#end\n" +
                "import static ${EspressoPackageName}.espresso.action.ViewActions.*;\n" +
                "import static ${EspressoPackageName}.espresso.assertion.ViewAssertions.*;\n" +
                "import static ${EspressoPackageName}.espresso.matcher.ViewMatchers.*;\n" +
                "\n" +
                "import ${ResourcePackageName}.R;\n" +
                "\n" +
                "import org.hamcrest.Description;\n" +
                "import org.hamcrest.Matcher;\n" +
                "import org.hamcrest.TypeSafeMatcher;\n" +
                "import org.hamcrest.core.IsInstanceOf;\n" +
                "import org.junit.Rule;\n" +
                "import org.junit.Test;\n" +
                "import org.junit.runner.RunWith;\n" +
                "\n" +
                "import static org.hamcrest.Matchers.*;\n" +
                "\n" +
                "@LargeTest\n" +
                "@RunWith(AndroidJUnit4.class)\n" +
                "public class ${ClassName} {\n" +
                "\n" +
                "    @Rule\n" +
                "    public ActivityTestRule<${TestActivityName}> mActivityTestRule = new ActivityTestRule<>(${TestActivityName}.class);\n" +
                "\n" +
                "    @Test\n" +
                "    public void ${TestMethodName}() {\n" +
                "    System.out.println(\"Starting run of ${ClassName}\");\n" +
                "    #foreach (${line} in ${TestCode})\n" +
                "    ${line}\n" +
                "    #end\n" +
                "    }\n" +
                "\n" +
                "    #if (${AddChildAtPositionMethod})\n" +
                "    private static Matcher<View> childAtPosition(\n" +
                "            final Matcher<View> parentMatcher, final int position) {\n" +
                "\n" +
                "        return new TypeSafeMatcher<View>() {\n" +
                "            @Override\n" +
                "            public void describeTo(Description description) {\n" +
                "                description.appendText(\"Child at position \" + position + \" in parent \");\n" +
                "                parentMatcher.describeTo(description);\n" +
                "            }\n" +
                "\n" +
                "            @Override\n" +
                "            public boolean matchesSafely(View view) {\n" +
                "                ViewParent parent = view.getParent();\n" +
                "                return parent instanceof ViewGroup && parentMatcher.matches(parent)\n" +
                "                        && view.equals(((ViewGroup)parent).getChildAt(position));\n" +
                "            }\n" +
                "        };\n" +
                "    }\n" +
                "    #end\n" +
                "    #if (${AddClassOrSuperClassesNameMethod})\n" +
                "private static Matcher<View> classOrSuperClassesName(final Matcher<String> classNameMatcher) {\n" +
                "\n" +
                "        return new TypeSafeMatcher<View>() {\n" +
                "            @Override\n" +
                "            public void describeTo(Description description) {\n" +
                "                description.appendText(\"Class name or any super class name \");\n" +
                "                classNameMatcher.describeTo(description);\n" +
                "            }\n" +
                "\n" +
                "            @Override\n" +
                "            public boolean matchesSafely(View view) {\n" +
                "                Class<?> clazz = view.getClass();\n" +
                "                String canonicalName;\n" +
                "\n" +
                "                do {\n" +
                "                    canonicalName = clazz.getCanonicalName();\n" +
                "                    if (canonicalName == null) {\n" +
                "                        return false;\n" +
                "                    }\n" +
                "\n" +
                "                    if (classNameMatcher.matches(canonicalName)) {\n" +
                "                        return true;\n" +
                "                    }\n" +
                "\n" +
                "                    clazz = clazz.getSuperclass();\n" +
                "                    if (clazz == null) {\n" +
                "                        return false;\n" +
                "                    }\n" +
                "                } while (!\"java.lang.Object\".equals(canonicalName));\n" +
                "\n" +
                "                return false;\n" +
                "            }\n" +
                "        };\n" +
                "    }\n" +
                "    #end\n" +
                "    #if (${AddTryCatchImport})\n" +
                "private String buildPerformExceptionMessage(Exception e, int performNumber) {\n" +
                "        String testPackageName = \"${PackageName}\";\n" +
                "        for (StackTraceElement stackTraceElement : e.getStackTrace()) {\n" +
                "            if (stackTraceElement.getClassName().startsWith(testPackageName)) {\n" +
                "                return \"ERROR: when executing line number: \" + stackTraceElement.getLineNumber() + \", perform number: \" + performNumber;\n" +
                "            }\n" +
                "        }\n" +
                "\n" +
                "        return e.getMessage();\n" +
                "    }\n" +
                "    #end\n" +
                "}";
    }
}
