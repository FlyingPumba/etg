package org.etg.espresso.codegen;

import org.etg.ETGProperties;
import org.etg.espresso.EspressoTestCase;
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

            EspressoTestCase testCase = new EspressoTestCase(properties, widgetTestCase, String.format("TestCase%d", i));

            espressoTestCases.add(testCase);
        }

        return espressoTestCases;
    }
}
