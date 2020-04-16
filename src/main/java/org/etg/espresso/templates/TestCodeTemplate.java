package org.etg.espresso.templates;


public class TestCodeTemplate implements VelocityTemplate {

    @Override
    public String getName() {
        return "TestCase.java";
    }

    public String getAsRawString() {
        return "#if (${PackageName} && ${PackageName} != \"\")\n" +
                "package ${PackageName};\n" +
                "\n" +
                "#end\n" +
                "\n" +
                "import ${EspressoPackageName}.espresso.Espresso;\n" +
                "import ${EspressoPackageName}.espresso.ViewInteraction;\n" +
                "import ${EspressoPackageName}.espresso.action.CoordinatesProvider;\n" +
                "import ${EspressoPackageName}.espresso.action.GeneralSwipeAction;\n" +
                "import ${EspressoPackageName}.espresso.action.Press;\n" +
                "import ${EspressoPackageName}.espresso.action.Swipe;\n" +
                "import ${EspressoPackageName}.espresso.action.ViewActions;\n" +
                "import ${EspressoPackageName}.espresso.action.Tap;\n" +
                "import ${EspressoPackageName}.espresso.action.GeneralLocation;\n" +
                "import ${EspressoPackageName}.espresso.action.Press;\n" +
                "import ${EspressoPackageName}.espresso.ViewAction;\n" +
                "#if ($EspressoPackageName.toString().contains(\"androidx\"))\n" +
                "import androidx.test.rule.ActivityTestRule;\n" +
                "import androidx.test.runner.AndroidJUnit4;\n" +
                "import androidx.test.filters.LargeTest;\n" +

                "#if (${AddScreenshotImport})\n" +
                "import androidx.test.runner.screenshot.ScreenCapture;\n" +
                "import androidx.test.runner.screenshot.Screenshot;\n" +
                "#end\n" +

                "#else\n" +
                "import android.support.test.rule.ActivityTestRule;\n" +
                "import android.support.test.runner.AndroidJUnit4;\n" +
                "import android.support.test.filters.LargeTest;\n" +

                "#if (${AddScreenshotImport})\n" +
                "import android.support.test.runner.screenshot.ScreenCapture;\n" +
                "import android.support.test.runner.screenshot.Screenshot;\n" +
                "#end\n" +

                "#end\n" +

                "#if (${AddScreenshotImport})\n" +
                "import android.graphics.Bitmap;\n" +
                "import java.io.IOException;\n" +
                "#end\n" +

                "import android.os.SystemClock;\n" +
                "import android.view.KeyEvent;\n" +
                "import android.view.View;\n" +
                "import android.view.ViewGroup;\n" +
                "import android.view.ViewParent;\n" +
                "import android.view.InputDevice;\n" +
                "import android.view.MotionEvent;\n" +
                "\n" +
                "import static ${EspressoPackageName}.InstrumentationRegistry.getInstrumentation;\n" +
                "import static ${EspressoPackageName}.espresso.Espresso.onView;\n" +
                "import static ${EspressoPackageName}.espresso.Espresso.openActionBarOverflowOrOptionsMenu;\n" +
                "import static ${EspressoPackageName}.espresso.action.ViewActions.*;\n" +
                "import static ${EspressoPackageName}.espresso.assertion.ViewAssertions.*;\n" +
                "import static ${EspressoPackageName}.espresso.matcher.ViewMatchers.*;\n" +
                "import static ${PackageName}.IsEqualTrimmingAndIgnoringCase.equalToTrimmingAndIgnoringCase;\n" +
                "import static ${PackageName}.VisibleViewMatcher.isVisible;\n" +
                "\n" +
                "import ${ResourcePackageName}.R;\n" +
                "\n" +
                "import org.hamcrest.Description;\n" +
                "import org.hamcrest.Matcher;\n" +
                "import org.hamcrest.TypeSafeMatcher;\n" +
                "import org.hamcrest.core.IsInstanceOf;\n" +
                "import org.junit.After;\n" +
                "import org.junit.Before;\n" +
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
                "    #if (${AddTryCatchImport})\n" +
                "private int errorCount;\n" +
                "\n" +
                "    @Before\n" +
                "    public void setUp(){\n" +
                "        errorCount = 0;\n" +
                "    }" +
                "\n " +
                "    @After\n" +
                "    public void teardown(){\n" +
                "        System.out.println(\"Error count: \" + errorCount);\n" +
                "    }" +
                "\n" +
                "    #end\n" +
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
                "        errorCount++;\n" +
                "        String testPackageName = \"${PackageName}\";\n" +
                "        for (StackTraceElement stackTraceElement : e.getStackTrace()) {\n" +
                "            if (stackTraceElement.getClassName().startsWith(testPackageName)) {\n" +
                "                return \"ERROR: when executing line number: \" + stackTraceElement.getLineNumber() + \", perform number: \" + performNumber;\n" +
                "            }\n" +
                "        }\n" +
                "\n" +
                "        return \"ERROR: when executing line number: unknown, perform number: \" + performNumber;\n" +
                "    }\n" +
                "    #end\n" +
                "    #if (${AddScreenshotImport})\n" +
                "private void getScreenshot(int performNumber) {\n" +
                "      try {\n" +
                "          ScreenCapture capture = Screenshot.capture();\n" +
                "          capture.setName(String.format(\"${ClassName}_%d\", performNumber));\n" +
                "          capture.setFormat(Bitmap.CompressFormat.PNG);\n" +
                "          capture.process();\n" +
                "      } catch (IOException e) {\n" +
                "          e.printStackTrace();\n" +
                "      }\n" +
                "    }\n" +
                "    #end\n" +
                "    private static Matcher<View> withTextOrHint(final Matcher<String> stringMatcher) {\n" +
                "        return anyOf(withText(stringMatcher), withHint(stringMatcher));\n" +
                "    }\n" +
                "#if (${swipeActionAdded})\n" +
                "    private ViewAction getSwipeAction(final int fromX, final int fromY, final int toX, final int toY) {\n" +
                "        return ViewActions.actionWithAssertions(\n" +
                "                new GeneralSwipeAction(\n" +
                "                        Swipe.SLOW,\n" +
                "                        new CoordinatesProvider() {\n" +
                "                            @Override\n" +
                "                            public float[] calculateCoordinates(View view) {\n" +
                "                                float[] coordinates = {fromX, fromY};\n" +
                "                                return coordinates;\n" +
                "                            }\n" +
                "                        },\n" +
                "                        new CoordinatesProvider() {\n" +
                "                            @Override\n" +
                "                            public float[] calculateCoordinates(View view) {\n" +
                "                                float[] coordinates = {toX, toY};\n" +
                "                                return coordinates;\n" +
                "                            }\n" +
                "                        },\n" +
                "                        Press.FINGER));\n" +
                "    }\n" +
                "   private void waitToScrollEnd() {\n" +
                "        SystemClock.sleep(500);\n" +
                "    } \n"+
                "#end\n" +
                "#if (${clickActionAdded})\n" +
                "    private ClickWithoutDisplayConstraint getClickAction() {\n" +
                "        return new ClickWithoutDisplayConstraint(\n" +
                "                Tap.SINGLE,\n" +
                "                GeneralLocation.VISIBLE_CENTER,\n" +
                "                Press.FINGER,\n" +
                "                InputDevice.SOURCE_UNKNOWN,\n" +
                "                MotionEvent.BUTTON_PRIMARY);\n" +
                "    }\n" +
                "#end\n" +
                "#if (${longClickActionAdded})\n" +
                "    private ClickWithoutDisplayConstraint getLongClickAction() {\n" +
                "        return new ClickWithoutDisplayConstraint(\n" +
                "                Tap.LONG,\n" +
                "                GeneralLocation.CENTER,\n" +
                "                Press.FINGER,\n" +
                "                InputDevice.SOURCE_UNKNOWN,\n" +
                "                MotionEvent.BUTTON_PRIMARY);\n" +
                "    }\n"+
                "#end\n" +
                "}"
                ;
    }


    @Override
    public boolean equals(Object o) {
        return o instanceof TestCodeTemplate;
    }

    @Override
    public int hashCode() {
        return getAsRawString().hashCode();
    }
}
