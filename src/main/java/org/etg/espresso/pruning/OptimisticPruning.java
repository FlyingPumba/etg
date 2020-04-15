package org.etg.espresso.pruning;

import org.etg.espresso.EspressoTestCase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
        Set<Integer> failingWidgetActionIndexes = espressoTestCase.getFailingWidgetActionIndexes();
        System.out.println(String.format("TEST: %s FAILING-ACTIONS: %d TOTAL-ACTIONS: %d",
                espressoTestCase.getTestName(),
                failingWidgetActionIndexes.size(),
                widgetActionsCount));

        int lowestFailingWidgetActionIndex = espressoTestCase.getLowestFailingWidgetActionIndex();
        System.out.println(String.format("TEST: %s LOWEST-FAILING-ACTION-INDEX: %d TOTAL-ACTIONS: %d",
                espressoTestCase.getTestName(),
                lowestFailingWidgetActionIndex,
                widgetActionsCount));

        // Order indexes in ascendant order
        ArrayList<Integer> orderedIndexes = new ArrayList<>(failingWidgetActionIndexes);
        Collections.sort(orderedIndexes);

        // Get clusters of failing actions
        int lastFailingIndex = -2;
        ArrayList<ArrayList<Integer>> failingClusters = new ArrayList<>();
        for (Integer failingIndex: orderedIndexes) {
            if (failingIndex != lastFailingIndex + 1) {
                // this is a new cluster
                ArrayList<Integer> cluster = new ArrayList<>();
                cluster.add(failingIndex);
                failingClusters.add(cluster);
            } else {
                // we continue on the same cluster
                ArrayList<Integer> cluster = failingClusters.get(failingClusters.size() - 1);
                cluster.add(failingIndex);
            }

            lastFailingIndex = failingIndex;
        }

        for (ArrayList<Integer> cluster: failingClusters) {
            String clusterIndexes = cluster.stream().map(Object::toString).collect(Collectors.joining(","));
            System.out.println(String.format("TEST: %s FAILING-CLUSTER-SIZE: %d FAILING-CLUSTER-INDEXES: %s",
                    espressoTestCase.getTestName(),
                    cluster.size(),
                    clusterIndexes));
        }
    }
}
