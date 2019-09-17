package org.etg.espresso.codegen;

import org.etg.espresso.EspressoTestCase;
import org.etg.mate.models.WidgetTestCase;

import java.util.ArrayList;
import java.util.List;

public class TestCodeGenerator {

    private String packageName;
    private String testPackageName;

    public TestCodeGenerator(String packageName, String testPackageName) {
        this.packageName = packageName;
        this.testPackageName = testPackageName;
    }

    public List<EspressoTestCase> getEspressoTestCases(List<WidgetTestCase> widgetTestCases) {
        List<EspressoTestCase> espressoTestCases = new ArrayList<>();
        for (int i = 0; i < widgetTestCases.size(); i++) {
            WidgetTestCase widgetTestCase = widgetTestCases.get(i);

            EspressoTestCase testCase = new EspressoTestCase(packageName, testPackageName,
                    widgetTestCase, String.format("TestCase%d", i));

            espressoTestCases.add(testCase);
        }

        return espressoTestCases;
    }
}
