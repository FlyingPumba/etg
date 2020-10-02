package org.etg.espresso.templates.kotlin;

import org.etg.espresso.templates.VelocityTemplate;

public class VisibleViewMatcherKotlinTemplate implements VelocityTemplate {

    @Override
    public String getName() {
        return "VisibleViewMatcher.kt";
    }

    @Override
    public String getRelativePath() {
        return "";
    }

    public String getAsRawString() {
        return "package ${PackageName}\n" +
                "\n" +
                "import android.view.View\n" +
                "\n" +
                "import androidx.test.espresso.matcher.ViewMatchers.Visibility\n" +
                "import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility\n" +
                "\n" +
                "import org.hamcrest.Description\n" +
                "import org.hamcrest.TypeSafeMatcher\n" +
                "\n" +
                "class VisibleViewMatcher : TypeSafeMatcher<View>() {\n" +
                "    companion object {\n" +
                "        fun isVisible() = VisibleViewMatcher()\n" +
                "    }\n" +
                "\n" +
                "    override fun matchesSafely(target: View): Boolean {\n" +
                "        return withEffectiveVisibility(Visibility.VISIBLE).matches(target) &&\n" +
                "                target.getWidth() > 0 && target.getHeight() > 0\n" +
                "    }\n" +
                "\n" +
                "    override fun describeTo(description: Description) {\n" +
                "        description.appendText(\"view has effective visibility VISIBLE and has width and height greater than zero\")\n" +
                "    }\n" +
                "}";
    }


    @Override
    public boolean equals(Object o) {
        return o instanceof VisibleViewMatcherKotlinTemplate;
    }

    @Override
    public int hashCode() {
        return getAsRawString().hashCode();
    }


}
