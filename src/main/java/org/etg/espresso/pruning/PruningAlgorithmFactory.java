package org.etg.espresso.pruning;

public class PruningAlgorithmFactory {
    public static PruningAlgorithm getPruningAlgorithm(String type) {
        switch (type) {
            case "optimistic":
                return new OptimisticPruning();
            case "pesimistic":
                return new PesimisticPruning();
            case "snapshots":
                return new SnapshotPruning();
            case "no-pruning":
            default:
                return new NoPruning();
        }
    }
}
