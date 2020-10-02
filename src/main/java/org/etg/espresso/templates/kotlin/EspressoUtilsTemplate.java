package org.etg.espresso.templates.kotlin;

import org.etg.espresso.templates.VelocityTemplate;

public class EspressoUtilsTemplate implements VelocityTemplate {

    @Override
    public String getFileName() {
        return "EspressoUtils.kt";
    }

    @Override
    public String getRelativePath() {
        return "utils/";
    }

    public String getAsRawString() {
        return "package ${PackageName}.utils\n" +
                "\n" +
                "import android.view.InputDevice\n" +
                "import android.view.MotionEvent\n" +
                "import android.view.View\n" +
                "import androidx.test.espresso.Espresso.onView\n" +
                "import androidx.test.espresso.UiController\n" +
                "import androidx.test.espresso.ViewAction\n" +
                "import androidx.test.espresso.action.*\n" +
                "import androidx.test.espresso.matcher.ViewMatchers.*\n" +
                "import ar.gob.coronavirus.ClickWithoutDisplayConstraint\n" +
                "import org.hamcrest.Matcher\n" +
                "import org.hamcrest.Matchers.anyOf\n" +
                "\n" +
                "class EspressoUtils {\n" +
                "    companion object {\n" +
                "        fun getSwipeAction(fromX: Float, fromY: Float, toX: Float, toY: Float): ViewAction {\n" +
                "            return ViewActions.actionWithAssertions(\n" +
                "                    GeneralSwipeAction(Swipe.SLOW,\n" +
                "                            CoordinatesProvider { view -> floatArrayOf(fromX, fromY) },\n" +
                "                            CoordinatesProvider { view -> floatArrayOf(toX, toY) },\n" +
                "                            Press.FINGER))\n" +
                "        }\n" +
                "\n" +
                "        fun getClickAction(): ClickWithoutDisplayConstraint {\n" +
                "            return ClickWithoutDisplayConstraint(\n" +
                "                    Tap.SINGLE,\n" +
                "                    GeneralLocation.VISIBLE_CENTER,\n" +
                "                    Press.FINGER,\n" +
                "                    InputDevice.SOURCE_UNKNOWN,\n" +
                "                    MotionEvent.BUTTON_PRIMARY)\n" +
                "        }\n" +
                "\n" +
                "        fun getLongClickAction(): ClickWithoutDisplayConstraint {\n" +
                "            return ClickWithoutDisplayConstraint(\n" +
                "                    Tap.LONG,\n" +
                "                    GeneralLocation.VISIBLE_CENTER,\n" +
                "                    Press.FINGER,\n" +
                "                    InputDevice.SOURCE_UNKNOWN,\n" +
                "                    MotionEvent.BUTTON_PRIMARY)\n" +
                "        }\n" +
                "\n" +
                "        fun withTextOrHint(stringMatcher: Matcher<String>): Matcher<View> {\n" +
                "            return anyOf(withText(stringMatcher), withHint(stringMatcher))\n" +
                "        }\n" +
                "\n" +
                "        fun waitFor(millis: Long) {\n" +
                "            onView(isRoot()).perform(getWaitForAction(millis));\n" +
                "        }\n" +
                "\n" +
                "        private fun getWaitForAction(millis: Long): ViewAction? {\n" +
                "            return object : ViewAction {\n" +
                "                override fun getConstraints(): Matcher<View> {\n" +
                "                    return isRoot()\n" +
                "                }\n" +
                "\n" +
                "                override fun getDescription(): String {\n" +
                "                    return \"Wait for $millis milliseconds.\"\n" +
                "                }\n" +
                "\n" +
                "                override fun perform(uiController: UiController, view: View?) {\n" +
                "                    uiController.loopMainThreadForAtLeast(millis)\n" +
                "                }\n" +
                "            }\n" +
                "        }\n" +
                "    }\n" +
                "}";
    }


    @Override
    public boolean equals(Object o) {
        return o instanceof EspressoUtilsTemplate;
    }

    @Override
    public int hashCode() {
        return getAsRawString().hashCode();
    }


}
