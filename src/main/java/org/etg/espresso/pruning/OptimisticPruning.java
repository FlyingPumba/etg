package org.etg.espresso.pruning;

import org.etg.espresso.EspressoTestCase;

import java.util.List;

/**
 *
 */
public class OptimisticPruning extends PruningAlgorithm {
    @Override
    void updateTestCase(EspressoTestCase espressoTestCase, List<Integer> newFailingPerformLines) {
        for (int tryCatchIndex: newFailingPerformLines) {
            int widgetActionIndex = espressoTestCase.geWidgetActionIndexForTryCatchIndex(tryCatchIndex);
            espressoTestCase.addFailingWidgetActionIndex(widgetActionIndex);
        }

        System.out.println(String.format("Pruning %d widget actions (out of %d) TEST %s",
                newFailingPerformLines.size(), espressoTestCase.getWidgetActionsCount(), espressoTestCase.getTestName()));
    }

    @Override
    public void printSummary(EspressoTestCase espressoTestCase) {
        int widgetActionsCount = espressoTestCase.getWidgetActionsCount();
        System.out.println(String.format("TEST: %s FAILING-ACTIONS: %d TOTAL-ACTIONS: %d",
                espressoTestCase.getTestName(),
                espressoTestCase.getFailingWidgetActionIndexes().size(),
                widgetActionsCount));
    }
}
