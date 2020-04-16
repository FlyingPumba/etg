package org.etg.espresso.pruning;

import org.etg.espresso.EspressoTestCase;
import org.etg.mate.models.Action;

import java.util.List;

public class PesimisticPruning extends PruningAlgorithm {
    @Override
    void updateTestCase(EspressoTestCase espressoTestCase, List<Integer> newFailingPerformLines) {
        int lowestFailingWidgetActionIndex = newFailingPerformLines.get(0);

        for (int i = lowestFailingWidgetActionIndex; i < espressoTestCase.getWidgetActionsCount(); i++) {
            espressoTestCase.addFailingWidgetActionIndex(i);
        }

        System.out.println(String.format("Pruning actions starting after widget action with index %d (out of %d) TEST %s",
                lowestFailingWidgetActionIndex, espressoTestCase.getWidgetActionsCount(), espressoTestCase.getTestName()));
    }

    @Override
    public void printSummary(EspressoTestCase espressoTestCase) {
        int lowestFailingWidgetActionIndex = espressoTestCase.getLowestFailingWidgetActionIndex();
        int widgetActionsCount = espressoTestCase.getWidgetActionsCount();
        System.out.println(String.format("TEST: %s LOWEST-FAILING-ACTION-INDEX: %d TOTAL-ACTIONS: %d",
                espressoTestCase.getTestName(),
                lowestFailingWidgetActionIndex,
                widgetActionsCount));

        Action lowestFailingAction = null;
        Action highestSuccessfulAction = null;

        if (lowestFailingWidgetActionIndex < widgetActionsCount) {
            lowestFailingAction = espressoTestCase.getWidgetActions().get(lowestFailingWidgetActionIndex);

            if (lowestFailingWidgetActionIndex > 0) {
                highestSuccessfulAction = espressoTestCase.getWidgetActions().get(lowestFailingWidgetActionIndex - 1);
            }
        }

        System.out.println(String.format("TEST: %s LOWEST-FAILING-ACTION-TYPE: %s HIGHEST-SUCCESSFUL-ACTION-TYPE: %s",
                espressoTestCase.getTestName(),
                lowestFailingAction == null ? "-" : lowestFailingAction.getActionType().toString(),
                highestSuccessfulAction == null ? "-" : highestSuccessfulAction.getActionType().toString()));
    }
}
