package org.etg.espresso.templates.kotlin;

import org.etg.espresso.templates.VelocityTemplate;

public class IsEqualTrimmingAndIgnoringCaseKotlinTemplate implements VelocityTemplate {

    @Override
    public String getName() {
        return "IsEqualTrimmingAndIgnoringCase.kt";
    }

    @Override
    public String getRelativePath() {
        return "";
    }

    public String getAsRawString() {
        return "package ${PackageName}\n" +
                "\n" +
                "import org.hamcrest.BaseMatcher\n" +
                "import org.hamcrest.Description\n" +
                "\n" +
                "class IsEqualTrimmingAndIgnoringCase(private val string: String) : BaseMatcher<String>() {\n" +
                "    companion object {\n" +
                "        fun equalToTrimmingAndIgnoringCase(string: String) = IsEqualTrimmingAndIgnoringCase(string)\n" +
                "    }\n" +
                "\n" +
                "    fun matchesSafely(item: String): Boolean {\n" +
                "        return string.trim().equals(item.trim(), true)\n" +
                "    }\n" +
                "\n" +
                "    fun describeMismatchSafely(item: String, mismatchDescription: Description) {\n" +
                "        mismatchDescription.appendText(\"was \").appendText(item)\n" +
                "    }\n" +
                "\n" +
                "    override fun describeTo(description: Description) {\n" +
                "        description.appendText(\"equalToTrimmingAndIgnoringCase(\")\n" +
                "                .appendValue(string)\n" +
                "                .appendText(\")\")\n" +
                "    }\n" +
                "\n" +
                "    override fun matches(item: Any?): Boolean {\n" +
                "        return item != null && matchesSafely(item.toString())\n" +
                "    }\n" +
                "\n" +
                "    override fun describeMismatch(item: Any?, description: Description) {\n" +
                "        if (item == null) {\n" +
                "            super.describeMismatch(item, description)\n" +
                "        } else {\n" +
                "            describeMismatchSafely(item.toString(), description)\n" +
                "        }\n" +
                "    }\n" +
                "}";
    }


    @Override
    public boolean equals(Object o) {
        return o instanceof IsEqualTrimmingAndIgnoringCaseKotlinTemplate;
    }

    @Override
    public int hashCode() {
        return getAsRawString().hashCode();
    }


}
