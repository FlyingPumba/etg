package org.etg.espresso.codegen.codeMapper;

import org.etg.ETGProperties;
import org.etg.espresso.templates.TemplatesFactory;
import org.etg.espresso.templates.VelocityTemplate;
import org.etg.espresso.templates.java.IsEqualTrimmingAndIgnoringCaseJavaTemplate;
import org.etg.espresso.templates.java.VisibleViewMatcherJavaTemplate;
import org.etg.espresso.templates.kotlin.EspressoUtils;
import org.etg.espresso.templates.kotlin.IsEqualTrimmingAndIgnoringCaseKotlinTemplate;
import org.etg.espresso.templates.kotlin.MockedServerTest;
import org.etg.espresso.templates.kotlin.VisibleViewMatcherKotlinTemplate;
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
    private final Set<VelocityTemplate> neededTemplates = new HashSet<>();
    private final TemplatesFactory templatesFactory = new TemplatesFactory();

    protected ETGProperties etgProperties;

    public TestCodeMapper(ETGProperties properties) {
        etgProperties = properties;
        if (etgProperties.useKotlinFormat()) {
            neededTemplates.add(new VisibleViewMatcherKotlinTemplate());
            neededTemplates.add(new IsEqualTrimmingAndIgnoringCaseKotlinTemplate());
            neededTemplates.add(new MockedServerTest());
            neededTemplates.add(new EspressoUtils());
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

    abstract public void addTestCodeLinesForAction(Action action, List<String> testCodeLines, int actionIndex, int actionsCount);

    public static String getStatementTerminator(ETGProperties etgProperties) {
        // The following breaks the Java parsing in ViewPickingStatementGenerator#addTestCodeLines
        // if (etgProperties.useKotlinFormat()) {
        //     return "";
        // }

        return ";";
    }
}
