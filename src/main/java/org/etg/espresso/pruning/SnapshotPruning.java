package org.etg.espresso.pruning;

import org.etg.ETGProperties;
import org.etg.espresso.EspressoTestCase;

import java.util.List;

public class SnapshotPruning extends PruningAlgorithm {
    @Override
    public void pruneFailingPerforms(EspressoTestCase espressoTestCase, ETGProperties properties) throws Exception {
        // TODO
    }

    @Override
    void updateTestCase(EspressoTestCase espressoTestCase, List<Integer> newFailingPerformLines) {
        // TODO
    }

    @Override
    public void printSummary(EspressoTestCase espressoTestCase) {
        // TODO
    }
}
