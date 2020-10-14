package org.etg.espresso.codegen.codeMapper;

import org.etg.ETGProperties;
import org.etg.espresso.templates.TemplatesFactory;
import org.etg.espresso.templates.VelocityTemplate;
import org.etg.espresso.templates.java.IsEqualTrimmingAndIgnoringCaseJavaTemplate;
import org.etg.espresso.templates.java.VisibleViewMatcherJavaTemplate;
import org.etg.espresso.templates.kotlin.utils.EspressoUtilsTemplate;
import org.etg.espresso.templates.kotlin.utils.IsEqualTrimmingAndIgnoringCaseKotlinTemplate;
import org.etg.espresso.templates.kotlin.utils.MockedServerTest;
import org.etg.espresso.templates.kotlin.utils.VisibleViewMatcherKotlinTemplate;
import org.etg.mate.models.Action;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class TestCodeMapper {

    public boolean mIsclassOrSuperClassesNameAdded = false;
    public boolean mUseTextForElementMatching = true;
    public boolean swipeActionAdded = false;
    public boolean longClickActionAdded = false;
    public boolean clickActionAdded = false;
    public boolean waitActionAdded = false;

    /**
     * Needed templates, every extra template that we need must be listed here
     * **/
    protected static final Set<VelocityTemplate> neededTemplates = new HashSet<>();
    private final TemplatesFactory templatesFactory = new TemplatesFactory();

    protected static Set<String> extraImports = new HashSet<>();

    protected ETGProperties etgProperties;

    public TestCodeMapper(ETGProperties properties) {
        etgProperties = properties;
        if (etgProperties.useKotlinFormat()) {
            neededTemplates.add(new VisibleViewMatcherKotlinTemplate());
            neededTemplates.add(new IsEqualTrimmingAndIgnoringCaseKotlinTemplate());
            neededTemplates.add(new MockedServerTest());
            neededTemplates.add(new EspressoUtilsTemplate());
        } else {
            neededTemplates.add(new VisibleViewMatcherJavaTemplate());
            neededTemplates.add(new IsEqualTrimmingAndIgnoringCaseJavaTemplate());
        }
    }

    public void addTemplateFor(TemplatesFactory.Template action) {
        neededTemplates.add(templatesFactory.createFor(action, etgProperties));
    }

    public Set<VelocityTemplate> getNeededTemplates() {
        return neededTemplates;
    }

    abstract public void addTestCodeLinesForAction(int index, List<Action> actions, List<String> testCodeLines);

    public static String getStatementTerminator(ETGProperties etgProperties) {
        // The following breaks the Java parsing in ViewPickingStatementGenerator#addTestCodeLines
        // if (etgProperties.useKotlinFormat()) {
        //     return "";
        // }

        return ";";
    }

    public String getExtraImports() {
        StringBuilder lines = new StringBuilder();

        for (String imp: extraImports) {
            String importStatement;
            if (etgProperties.useKotlinFormat()) {
                importStatement = String.format("import %s\n", imp);
            } else {
                importStatement = String.format("import %s;\n", imp);
            }
            lines.append(importStatement);
        }

        return lines.toString();
    }

    public void addExtraImports(List<String> imports) {
        extraImports.addAll(imports);
    }
}
