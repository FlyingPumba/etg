package org.etg.espresso.templates;


public class KotlinTestCodeTemplate implements VelocityTemplate {

    @Override
    public String getName() {
        return "TestCase.kt";
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
                "import ${PackageName}.IsEqualTrimmingAndIgnoringCase.equalToTrimmingAndIgnoringCase\n" +
                "import ${PackageName}.VisibleViewMatcher.isVisible\n" +
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
                "class ${ClassName} : KoinTest {\n" +
                "\n" +
                "private val webServer = MockWebServer()\n" +
                "    #if (${AddTryCatchImport})\n" +
                "private int errorCount\n" +
                "    #end\n" +
                "\n" +
                "@Before\n" +
                "    fun setUp() {\n" +
                "    #if (${AddTryCatchImport})\n" +
                "        errorCount = 0\n" +
                "    #end\n" +
                "        webServer.start()\n" +
                "        declare(named(\"base_url\")) {\n" +
                "            webServer.url(\"/\").toString()\n" +
                "        }\n" +
                "        declare(named(\"static_url\")) {\n" +
                "            webServer.url(\"/\").toString()\n" +
                "        }\n" +
                "    }\n" +
                "\n" +
                "    @After\n" +
                "    fun tearDown() {\n" +
                "    #if (${AddTryCatchImport})\n" +
                "        System.out.println(\"Error count: \" + errorCount)\n" +
                "    #end\n" +
                "        webServer.shutdown()\n" +
                "    }\n" +
                "\n" +
                "    @Rule\n" +
                "    @JvmField\n" +
                "    var activityTestRule: ActivityTestRule<${TestActivityName}> = ActivityTestRule(${TestActivityName}::class.java, true, false)\n" +
                "\n" +
                "    @Test\n" +
                "    fun ${TestMethodName}() {\n" +
                "    System.out.println(\"Starting run of ${ClassName}\")\n" +
                "    activityTestRule.launchActivity(null)\n" +
                "\n" +
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
                "    fun withTextOrHint(stringMatcher: Matcher<String>): Matcher<View> {\n" +
                "        return anyOf(withText(stringMatcher), withHint(stringMatcher))\n" +
                "    }\n" +
                "#if (${swipeActionAdded})\n" +
                "    fun getSwipeAction(fromX: Float, fromY: Float, toX: Float, toY: Float): ViewAction {\n" +
                "        return ViewActions.actionWithAssertions(\n" +
                "                GeneralSwipeAction(Swipe.SLOW,\n" +
                "                CoordinatesProvider { view -> floatArrayOf(fromX, fromY) },\n" +
                "                CoordinatesProvider { view -> floatArrayOf(toX, toY) },\n" +
                "                Press.FINGER))\n" +
                "    }\n" +
                "   fun waitToScrollEnd() {\n" +
                "        SystemClock.sleep(500)\n" +
                "    } \n"+
                "#end\n" +
                "#if (${clickActionAdded})\n" +
                "    fun getClickAction(): ClickWithoutDisplayConstraint {\n" +
                "        return ClickWithoutDisplayConstraint(\n" +
                "                Tap.SINGLE,\n" +
                "                GeneralLocation.VISIBLE_CENTER,\n" +
                "                Press.FINGER,\n" +
                "                InputDevice.SOURCE_UNKNOWN,\n" +
                "                MotionEvent.BUTTON_PRIMARY)\n" +
                "    }\n" +
                "#end\n" +
                "#if (${longClickActionAdded})\n" +
                "    fun getLongClickAction(): ClickWithoutDisplayConstraint {\n" +
                "        return ClickWithoutDisplayConstraint(\n" +
                "                Tap.LONG,\n" +
                "                GeneralLocation.CENTER,\n" +
                "                Press.FINGER,\n" +
                "                InputDevice.SOURCE_UNKNOWN,\n" +
                "                MotionEvent.BUTTON_PRIMARY)\n" +
                "    }\n"+
                "#end\n" +
                "#if (${waitForAdded})\n" +
                "    fun waitFor(millis: Long): ViewAction? {\n" +
                "        return object : ViewAction {\n" +
                "            override fun getConstraints(): Matcher<View> {\n" +
                "                return isRoot()\n" +
                "            }\n" +
                "\n" +
                "            override fun getDescription(): String {\n" +
                "                return \"Wait for $millis milliseconds.\"\n" +
                "            }\n" +
                "\n" +
                "            override fun perform(uiController: UiController, view: View?) {\n" +
                "                uiController.loopMainThreadForAtLeast(millis)\n" +
                "            }\n" +
                "        }\n" +
                "    }\n"+
                "#end\n" +
                "}"
                ;
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
