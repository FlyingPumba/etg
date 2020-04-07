package org.etg.espresso.pruning;

import org.etg.espresso.EspressoTestCase;

import java.util.List;

public class NoPruning extends PruningAlgorithm {
    @Override
    void updateTestCase(EspressoTestCase espressoTestCase, List<Integer> newFailingPerformLines) {
        // do nothing
    }

    @Override
    public void printSummary(EspressoTestCase espressoTestCase) {
        // do nothing
    }
}
