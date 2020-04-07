package org.etg.espresso.pruning;

import org.etg.ETGProperties;
import org.etg.espresso.EspressoTestCase;
import org.etg.espresso.EspressoTestRunner;

import java.util.ArrayList;
import java.util.List;

public abstract class PruningAlgorithm {
    /**
     * Perform fixed-point removal of failing performs in the test case
     *
     * @param properties
     * @throws Exception
     */
    public void pruneFailingPerforms(EspressoTestCase espressoTestCase, ETGProperties properties) throws Exception {
        List<Integer> failingPerformLines;
        List<Integer> newFailingPerformLines = new ArrayList<>();
        do {
            failingPerformLines = new ArrayList<>(newFailingPerformLines);
            espressoTestCase.addToProject(properties, false);
            newFailingPerformLines = EspressoTestRunner.runTestCase(properties, espressoTestCase, false);

            if (newFailingPerformLines.size() > 0) {
                updateTestCase(espressoTestCase, newFailingPerformLines);
            }
        } while (!failingPerformLines.equals(newFailingPerformLines) && newFailingPerformLines.size() > 0);
    }

    /**
     * Update test case failing actions
     * @param espressoTestCase
     * @param newFailingPerformLines
     */
    abstract void updateTestCase(EspressoTestCase espressoTestCase, List<Integer> newFailingPerformLines);

    /**
     * Calculate and output information about failing actions
     * @param espressoTestCase
     */
    public abstract void printSummary(EspressoTestCase espressoTestCase);
}
