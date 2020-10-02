package org.etg.espresso.codegen.viewPicking;

import org.etg.ETGProperties;

import static org.etg.espresso.util.StringHelper.boxString;
import static org.etg.espresso.util.StringHelper.isNullOrEmpty;

public class MatcherBuilder {

    public enum Kind {Id, Text, ContentDescription, ClassName}

    private ETGProperties etgProperties;
    private int matcherCount = 0;
    private final StringBuilder matchers = new StringBuilder();

    public MatcherBuilder(ETGProperties etgProperties) {
        this.etgProperties = etgProperties;
    }

    public void addMatcher(Kind kind, String matchedString, boolean shouldBox, boolean isAssertionMatcher) {
        if (!isNullOrEmpty(matchedString)) {
            if (kind == Kind.ClassName && !isAssertionMatcher) {
                matchedString = getInternalName(matchedString);
            }

            if (kind == Kind.Text && etgProperties.disableTextMatchers()) {
                return;
            }

            if (matcherCount > 0) {
                matchers.append(", ");
            }

            if (kind == Kind.ClassName) {
                if (isAssertionMatcher) {
                    matchers.append("IsInstanceOf.<View>instanceOf(" + matchedString + ".class)");
                } else {
                    matchers.append("classOrSuperClassesName(is(" + boxString(matchedString) + "))");
                }
            } else if (kind == Kind.Id) {
                matchers.append("\nwith").append(kind.name()).append("(")
                        .append(shouldBox ? boxString(matchedString) : matchedString).append(")");
            } else if (kind == Kind.Text) {
                matchers.append("\nwithTextOrHint(").append("equalToTrimmingAndIgnoringCase(")
                        .append(shouldBox ? boxString(matchedString) : matchedString).append("))");
            } else {
                matchers.append("\nwith").append(kind.name()).append("(equalToTrimmingAndIgnoringCase(")
                        .append(shouldBox ? boxString(matchedString) : matchedString).append("))");
            }

            matcherCount++;
        }
    }

    /**
     * Returns the name of the class that can be used in the generated test code.
     * For example, for a class foo.bar.Foo.Bar it returns foo.bar.Foo$Bar.
     */
    private String getInternalName(String className) {
        // If the PsiClass was not found or its internal name was not obtained, apply a simple heuristic.
        String[] nameFragments = className.split("\\.");
        String resultClassName = "";
        for (int i = 0; i < nameFragments.length - 1; i++) {
            String fragment = nameFragments[i];
            resultClassName += fragment + (Character.isUpperCase(fragment.charAt(0)) ? "$" : ".");
        }
        resultClassName += nameFragments[nameFragments.length - 1];

        return resultClassName;
    }

    public int getMatcherCount() {
        return matcherCount;
    }

    public String getMatchers() {
        return matchers.toString();
    }

}
