package org.etg.espresso.codegen;

import org.etg.ETGProperties;
import org.etg.espresso.EspressoTestCase;
import org.etg.espresso.templates.java.JavaTestCodeTemplate;
import org.etg.espresso.templates.kotlin.KotlinTestCodeTemplate;
import org.etg.espresso.templates.VelocityTemplate;
import org.etg.mate.models.WidgetTestCase;

import java.util.ArrayList;
import java.util.List;

public class TestCodeGenerator {

    private ETGProperties properties;

    public TestCodeGenerator(ETGProperties properties) {
        this.properties = properties;
    }

    public List<EspressoTestCase> getEspressoTestCases(List<WidgetTestCase> widgetTestCases) throws Exception {
        List<EspressoTestCase> espressoTestCases = new ArrayList<>();
        for (int i = 0; i < widgetTestCases.size(); i++) {
            WidgetTestCase widgetTestCase = widgetTestCases.get(i);

            VelocityTemplate testCaseTemplate;
            if (properties.useKotlinFormat()) {
                testCaseTemplate = new KotlinTestCodeTemplate();
            } else {
                testCaseTemplate = new JavaTestCodeTemplate();
            }

            EspressoTestCase testCase = new EspressoTestCase(properties, widgetTestCase,
                    String.format("%s%d", getETGTestCaseNamePrefix(), i), testCaseTemplate);

            espressoTestCases.add(testCase);
        }

        return espressoTestCases;
    }

    public static String getETGTestCaseNamePrefix() {
        return "ETGTestCase";
    }
}
