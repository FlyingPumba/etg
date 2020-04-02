package org.etg.espresso.templates;

public class VisibleViewMatcherTemplate implements VelocityTemplate {

    @Override
    public String getName() {
        return "VisibleViewMatcher.java";
    }

    public String getAsRawString() {
        return "#if (${PackageName} && ${PackageName} != \"\")\n" +
                "package ${PackageName};\n" +
                "\n" +
                "#end\n" +
                "import android.view.View;\n" +
                "\n" +
                "import ${EspressoPackageName}.espresso.matcher.ViewMatchers.Visibility;\n" +
                "\n" +
                "import org.hamcrest.Description;\n" +
                "import org.hamcrest.TypeSafeMatcher;\n" +
                "\n" +
                "import static ${EspressoPackageName}.espresso.matcher.ViewMatchers.withEffectiveVisibility;\n" +
                "\n" +
                "\n" +
                "public final class VisibleViewMatcher extends TypeSafeMatcher<View> {\n" +
                "\n" +
                "    public VisibleViewMatcher() {\n" +
                "        super(View.class);\n" +
                "    }\n" +
                "\n" +
                "    public static VisibleViewMatcher isVisible(){\n" +
                "        return new VisibleViewMatcher();\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    protected boolean matchesSafely(View target) {\n" +
                "        return withEffectiveVisibility(Visibility.VISIBLE).matches(target) &&\n" +
                "                target.getWidth() >  0 && target.getHeight() > 0;\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    public void describeTo(Description description) {\n" +
                "        description.appendText(\"view has effective visibility VISIBLE and has width and height greater than zero\");\n" +
                "    }\n" +
                "}";
    }


    @Override
    public boolean equals(Object o) {
        return o instanceof VisibleViewMatcherTemplate;
    }

    @Override
    public int hashCode() {
        return getAsRawString().hashCode();
    }


}
