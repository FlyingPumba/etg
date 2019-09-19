package org.etg.espresso.codegen;

import org.etg.ETGProperties;
import org.etg.espresso.EspressoTestCase;
import org.etg.mate.models.WidgetTestCase;

import java.util.ArrayList;
import java.util.List;

public class TestCodeGenerator {

    private String packageName;
    private String testPackageName;
    private String espressoPackageName;

    public TestCodeGenerator(String packageName, String testPackageName, String espressoPackageName) {
        this.packageName = packageName;
        this.testPackageName = testPackageName;
        this.espressoPackageName = espressoPackageName;
    }

    public TestCodeGenerator(ETGProperties properties) throws Exception {
        this(properties.getPackageName(), properties.getTestPackageName(), properties.getEspressoPackageName());
    }

    public List<EspressoTestCase> getEspressoTestCases(List<WidgetTestCase> widgetTestCases) {
        List<EspressoTestCase> espressoTestCases = new ArrayList<>();
        for (int i = 0; i < widgetTestCases.size(); i++) {
            WidgetTestCase widgetTestCase = widgetTestCases.get(i);

            EspressoTestCase testCase = new EspressoTestCase(packageName, testPackageName, espressoPackageName,
                    widgetTestCase, String.format("TestCase%d", i));

            espressoTestCases.add(testCase);
        }

        return espressoTestCases;
    }
}
