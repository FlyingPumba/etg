package org.etg.espresso.templates.kotlin.utils;

import org.etg.espresso.templates.VelocityTemplate;

public class ClassOrSuperClassNameMatcherKotlinTemplate implements VelocityTemplate {

    @Override
    public String getFileName() {
        return "ClassOrSuperClassNameMatcher.kt";
    }

    @Override
    public String getRelativePath() {
        return "utils/";
    }

    public String getAsRawString() {
        return "package ${PackageName}.utils\n" +
                "\n" +
                "import android.view.View\n" +
                "import org.hamcrest.Description\n" +
                "import org.hamcrest.Matcher\n" +
                "import org.hamcrest.TypeSafeMatcher\n" +
                "\n" +
                "class ClassOrSuperClassNameMatcher(val classNameMatcher: Matcher<String>) : TypeSafeMatcher<View>() {\n" +
                "    companion object {\n" +
                "        fun classOrSuperClassesName(classNameMatcher: Matcher<String>) =\n" +
                "                ClassOrSuperClassNameMatcher(classNameMatcher)\n" +
                "    }\n" +
                "\n" +
                "    override fun matchesSafely(target: View): Boolean {\n" +
                "        var clazz: Class<in View>? = target.javaClass\n" +
                "        var canonicalName: String?\n" +
                "\n" +
                "        do {\n" +
                "            canonicalName = clazz!!.getCanonicalName()\n" +
                "            if (canonicalName == null) {\n" +
                "                return false\n" +
                "            }\n" +
                "\n" +
                "            if (classNameMatcher.matches(canonicalName)) {\n" +
                "                return true\n" +
                "            }\n" +
                "\n" +
                "            clazz = clazz.getSuperclass()\n" +
                "            if (clazz == null) {\n" +
                "                return false\n" +
                "            }\n" +
                "        } while (!\"java.lang.Object\".equals(canonicalName))\n" +
                "\n" +
                "        return false\n" +
                "    }\n" +
                "\n" +
                "    override fun describeTo(description: Description) {\n" +
                "        description.appendText(\"Class name or any super class name \")\n" +
                "        classNameMatcher.describeTo(description)\n" +
                "    }\n" +
                "}";
    }


    @Override
    public boolean equals(Object o) {
        return o instanceof ClassOrSuperClassNameMatcherKotlinTemplate;
    }

    @Override
    public int hashCode() {
        return getAsRawString().hashCode();
    }


}
