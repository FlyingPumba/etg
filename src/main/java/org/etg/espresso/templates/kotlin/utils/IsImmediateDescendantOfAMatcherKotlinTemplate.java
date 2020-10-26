package org.etg.espresso.templates.kotlin.utils;

import org.etg.espresso.templates.VelocityTemplate;

public class IsImmediateDescendantOfAMatcherKotlinTemplate implements VelocityTemplate {

    @Override
    public String getFileName() {
        return "IsImmediateDescendantOfAMatcher.kt";
    }

    @Override
    public String getRelativePath() {
        return "utils/";
    }

    public String getAsRawString() {
        return "import android.view.View\n" +
                "import org.hamcrest.Description\n" +
                "import org.hamcrest.Matcher\n" +
                "import org.hamcrest.TypeSafeMatcher\n" +
                "\n" +
                "class IsImmediateDescendantOfAMatcher(private val ancestorMatcher: Matcher<View>) : TypeSafeMatcher<View>() {\n" +
                "    companion object {\n" +
                "        fun isImmediateDescendantOfA(ancestorMatcher: Matcher<View>) =\n" +
                "                IsImmediateDescendantOfAMatcher(ancestorMatcher)\n" +
                "    }\n" +
                "\n" +
                "    override fun describeTo(description: Description) {\n" +
                "        description.appendText(\"is immediate descendant of a: \")\n" +
                "        ancestorMatcher.describeTo(description)\n" +
                "    }\n" +
                "\n" +
                "    public override fun matchesSafely(view: View): Boolean {\n" +
                "        if (view.parent !is View) {\n" +
                "            return false\n" +
                "        }\n" +
                "        return ancestorMatcher.matches(view.parent)\n" +
                "    }\n" +
                "}";
    }


    @Override
    public boolean equals(Object o) {
        return o instanceof IsImmediateDescendantOfAMatcherKotlinTemplate;
    }

    @Override
    public int hashCode() {
        return getAsRawString().hashCode();
    }


}
