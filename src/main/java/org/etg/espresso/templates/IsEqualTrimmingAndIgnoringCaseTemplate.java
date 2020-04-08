package org.etg.espresso.templates;

public class IsEqualTrimmingAndIgnoringCaseTemplate implements VelocityTemplate {

    @Override
    public String getName() {
        return "IsEqualTrimmingAndIgnoringCase.java";
    }

    public String getAsRawString() {
        return "#if (${PackageName} && ${PackageName} != \"\")\n" +
                "package ${PackageName};\n" +
                "\n" +
                "#end\n" +
                "import org.hamcrest.Description;\n" +
                "import org.hamcrest.TypeSafeMatcher;\n" +
                "\n" +
                "\n" +
                "public class IsEqualTrimmingAndIgnoringCase extends TypeSafeMatcher<String> {\n" +
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
                "    @Override\n" +
                "    public boolean matchesSafely(String item) {\n" +
                "        return string.trim().equalsIgnoreCase(item.trim());\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    public void describeMismatchSafely(String item, Description mismatchDescription) {\n" +
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
                "}";
    }


    @Override
    public boolean equals(Object o) {
        return o instanceof IsEqualTrimmingAndIgnoringCaseTemplate;
    }

    @Override
    public int hashCode() {
        return getAsRawString().hashCode();
    }


}
