package org.etg.espresso.templates.kotlin;


import org.etg.espresso.templates.VelocityTemplate;

public class KotlinTestCodeTemplate implements VelocityTemplate {

    @Override
    public String getName() {
        return "TestCase.kt";
    }

    @Override
    public String getRelativePath() {
        return "";
    }

    public String getAsRawString() {
        return "#if (${PackageName} && ${PackageName} != \"\")\n" +
                "package ${PackageName}\n" +
                "\n" +
                "#end\n" +
                "\n" +
                "import ${EspressoPackageName}.espresso.Espresso\n" +
                "import ${EspressoPackageName}.espresso.ViewInteraction\n" +
                "import ${EspressoPackageName}.espresso.action.CoordinatesProvider\n" +
                "import ${EspressoPackageName}.espresso.action.GeneralSwipeAction\n" +
                "import ${EspressoPackageName}.espresso.action.Press\n" +
                "import ${EspressoPackageName}.espresso.action.Swipe\n" +
                "import ${EspressoPackageName}.espresso.UiController\n" +
                "import ${EspressoPackageName}.espresso.action.ViewActions\n" +
                "import ${EspressoPackageName}.espresso.action.Tap\n" +
                "import ${EspressoPackageName}.espresso.action.GeneralLocation\n" +
                "import ${EspressoPackageName}.espresso.ViewAction\n" +
                "#if ($EspressoPackageName.toString().contains(\"androidx\"))\n" +
                "import androidx.test.rule.ActivityTestRule\n" +
                "import androidx.test.ext.junit.runners.AndroidJUnit4\n" +
                "import androidx.test.filters.LargeTest\n" +

                "#if (${AddScreenshotImport})\n" +
                "import androidx.test.runner.screenshot.ScreenCapture\n" +
                "import androidx.test.runner.screenshot.Screenshot\n" +
                "import java.util.Locale\n" +
                "#end\n" +

                "#else\n" +
                "import android.support.test.rule.ActivityTestRule\n" +
                "import android.support.test.runner.AndroidJUnit4\n" +
                "import android.support.test.filters.LargeTest\n" +

                "#if (${AddScreenshotImport})\n" +
                "import android.support.test.runner.screenshot.ScreenCapture\n" +
                "import android.support.test.runner.screenshot.Screenshot\n" +
                "import java.util.Locale\n" +
                "#end\n" +

                "#end\n" +

                "#if (${AddScreenshotImport})\n" +
                "import android.graphics.Bitmap\n" +
                "import java.io.IOException\n" +
                "#end\n" +

                "import android.os.SystemClock\n" +
                "import android.view.KeyEvent\n" +
                "import android.view.View\n" +
                "import android.view.ViewGroup\n" +
                "import android.view.ViewParent\n" +
                "import android.view.InputDevice\n" +
                "import android.view.MotionEvent\n" +
                "\n" +
                "import ${EspressoPackageName}.InstrumentationRegistry.getInstrumentation\n" +
                "import ${EspressoPackageName}.espresso.Espresso.onView\n" +
                "import ${EspressoPackageName}.espresso.Espresso.openActionBarOverflowOrOptionsMenu\n" +
                "import ${EspressoPackageName}.espresso.action.ViewActions.*\n" +
                "import ${EspressoPackageName}.espresso.assertion.ViewAssertions.*\n" +
                "import ${EspressoPackageName}.espresso.matcher.ViewMatchers.*\n" +
                "import ${PackageName}.utils.MockedServerTest\n" +
                "import ${TestActivityName}\n" +
                "\n" +
                "import ${ResourcePackageName}.R\n" +
                "\n" +
                "import org.hamcrest.Description\n" +
                "import org.hamcrest.Matcher\n" +
                "import org.hamcrest.TypeSafeMatcher\n" +
                "import org.hamcrest.core.IsInstanceOf\n" +
                "import org.junit.After\n" +
                "import org.junit.Before\n" +
                "import org.junit.Rule\n" +
                "import org.junit.Test\n" +
                "import org.junit.runner.RunWith\n" +
                "\n" +
                "import org.koin.test.KoinTest\n" +
                "import org.koin.test.inject\n" +
                "import org.koin.core.qualifier.named\n" +
                "import org.koin.test.mock.declare\n" +
                "import okhttp3.mockwebserver.MockResponse\n" +
                "import okhttp3.mockwebserver.MockWebServer\n" +
                "\n" +
                "import org.hamcrest.Matchers.*\n" +
                "\n" +
                "@LargeTest\n" +
                "@RunWith(AndroidJUnit4::class)\n" +
                "class ${ClassName} : MockedServerTest() {\n" +
                "\n" +
                "#if (${AddTryCatchImport})" +
                "private int errorCount\n" +
                "\n" +
                "@Before\n" +
                "    fun setUp() {\n" +
                "        errorCount = 0\n" +
                "    }\n" +
                "\n" +
                "    @After\n" +
                "    fun tearDown() {\n" +
                "        System.out.println(\"Error count: \" + errorCount)\n" +
                "    }\n" +
                "#end" +
                "\n" +
                "    @Rule\n" +
                "    @JvmField\n" +
                "    val activityTestRule: ActivityTestRule<${TestActivitySimpleName}> = ActivityTestRule(\n" +
                "            ${TestActivitySimpleName}::class.java, true, false)" +
                "\n" +
                "    @Test\n" +
                "    fun ${TestMethodName}() {\n" +
                "    activityTestRule.launchActivity(null)\n" +
                "    #foreach (${line} in ${TestCode})\n" +
                "    ${line}\n" +
                "    #end\n" +
                "    }\n" +
                "\n" +
                "    #if (${AddClassOrSuperClassesNameMethod})\n" +
                "private Matcher<View> classOrSuperClassesName(final Matcher<String> classNameMatcher) {\n" +
                "\n" +
                "        return new TypeSafeMatcher<View>() {\n" +
                "            @Override\n" +
                "            fun describeTo(Description description) {\n" +
                "                description.appendText(\"Class name or any super class name \")\n" +
                "                classNameMatcher.describeTo(description)\n" +
                "            }\n" +
                "\n" +
                "            @Override\n" +
                "            public boolean matchesSafely(View view) {\n" +
                "                Class<?> clazz = view.getClass()\n" +
                "                String canonicalName\n" +
                "\n" +
                "                do {\n" +
                "                    canonicalName = clazz.getCanonicalName()\n" +
                "                    if (canonicalName == null) {\n" +
                "                        return false\n" +
                "                    }\n" +
                "\n" +
                "                    if (classNameMatcher.matches(canonicalName)) {\n" +
                "                        return true\n" +
                "                    }\n" +
                "\n" +
                "                    clazz = clazz.getSuperclass()\n" +
                "                    if (clazz == null) {\n" +
                "                        return false\n" +
                "                    }\n" +
                "                } while (!\"java.lang.Object\".equals(canonicalName))\n" +
                "\n" +
                "                return false\n" +
                "            }\n" +
                "        }\n" +
                "    }\n" +
                "    #end\n" +
                "    #if (${AddTryCatchImport})\n" +
                "private String buildPerformExceptionMessage(Exception e, int performNumber) {\n" +
                "        errorCount++\n" +
                "        String testPackageName = \"${PackageName}\"\n" +
                "        for (StackTraceElement stackTraceElement : e.getStackTrace()) {\n" +
                "            if (stackTraceElement.getClassName().startsWith(testPackageName)) {\n" +
                "                return \"ERROR: when executing line number: \" + stackTraceElement.getLineNumber() + \", perform number: \" + performNumber\n" +
                "            }\n" +
                "        }\n" +
                "\n" +
                "        return \"ERROR: when executing line number: unknown, perform number: \" + performNumber\n" +
                "    }\n" +
                "    #end\n" +
                "    #if (${AddScreenshotImport})\n" +
                "private void getScreenshot(int performNumber) {\n" +
                "      try {\n" +
                "          ScreenCapture capture = Screenshot.capture()\n" +
                "          String filename = String.format(Locale.US, \"${ClassName}_%d\", performNumber)\n" +
                "          capture.setName(filename)\n" +
                "          capture.setFormat(Bitmap.CompressFormat.PNG)\n" +
                "          capture.process()\n" +
                "      } catch (IOException e) {\n" +
                "          e.printStackTrace()\n" +
                "      }\n" +
                "    }\n" +
                "    #end\n" +
                "}";
    }


    @Override
    public boolean equals(Object o) {
        return o instanceof KotlinTestCodeTemplate;
    }

    @Override
    public int hashCode() {
        return getAsRawString().hashCode();
    }
}
