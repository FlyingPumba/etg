package org.etg.espresso.templates.java;

import org.etg.espresso.templates.VelocityTemplate;

public class IsEqualTrimmingAndIgnoringCaseJavaTemplate implements VelocityTemplate {

    @Override
    public String getName() {
        return "IsEqualTrimmingAndIgnoringCase.java";
    }

    @Override
    public String getRelativePath() {
        return "";
    }

    public String getAsRawString() {
        return "#if (${PackageName} && ${PackageName} != \"\")\n" +
                "package ${PackageName};\n" +
                "\n" +
                "#end\n" +
                "import org.hamcrest.BaseMatcher;\n" +
                "import org.hamcrest.Description;\n" +
                "\n" +
                "\n" +
                "public class IsEqualTrimmingAndIgnoringCase extends BaseMatcher<String> {\n" +
                "\n" +
                "    private final String string;\n" +
                "\n" +
                "    public IsEqualTrimmingAndIgnoringCase(String string) {\n" +
                "        if (string == null) {\n" +
                "            throw new IllegalArgumentException(\"Non-null value required by IsEqualTrimmingAndIgnoringCase()\");\n" +
                "        }\n" +
                "        this.string = string;\n" +
                "    }\n" +
                "\n" +
                "    public boolean matchesSafely(String item) {\n" +
                "        return string.trim().equalsIgnoreCase(item.trim());\n" +
                "    }\n" +
                "\n" +
                "    private void describeMismatchSafely(String item, Description mismatchDescription) {\n" +
                "        mismatchDescription.appendText(\"was \").appendText(item);\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    public void describeTo(Description description) {\n" +
                "        description.appendText(\"equalToTrimmingAndIgnoringCase(\")\n" +
                "                .appendValue(string)\n" +
                "                .appendText(\")\");\n" +
                "    }\n" +
                "\n" +
                "    public static IsEqualTrimmingAndIgnoringCase equalToTrimmingAndIgnoringCase(String string) {\n" +
                "        return new IsEqualTrimmingAndIgnoringCase(string);\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    public boolean matches(Object item) {\n" +
                "        return item != null && matchesSafely(item.toString());\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    final public void describeMismatch(Object item, Description description) {\n" +
                "        if (item == null) {\n" +
                "            super.describeMismatch(item, description);\n" +
                "        } else {\n" +
                "            describeMismatchSafely(item.toString(), description);\n" +
                "        }\n" +
                "    }\n" +
                "}";
    }


    @Override
    public boolean equals(Object o) {
        return o instanceof IsEqualTrimmingAndIgnoringCaseJavaTemplate;
    }

    @Override
    public int hashCode() {
        return getAsRawString().hashCode();
    }


}
