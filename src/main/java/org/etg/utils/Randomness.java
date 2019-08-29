package org.etg.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Randomness {
    private static Random rnd;

    static {
        // if (Properties.RANDOM_SEED != null) {
        //     rnd = new MersenneTwister(Properties.RANDOM_SEED);
        // } else {
            rnd = new MersenneTwister();
        // }
    }

    public static void setRnd(Random rnd) {
        Randomness.rnd = rnd;
    }

    public static Random getRnd() {
        return Randomness.rnd;
    }

    public static <T> T randomElement(List<T> list) {
        return list.get(rnd.nextInt(list.size()));
    }

    public static <T> int randomIndex(List<T> list) {
        return rnd.nextInt(list.size());
    }

    public static int getInRangeStd(int range) {
        return getInRangeStd(range, 2.0/15.0 * range);
    }

    public static int getInRangeStd(int range, double std) {
        int x;
        do {
            x = (int) Math.round(getRnd().nextGaussian() * std + (range - 1) / 2.0);
        } while (x < 0 || x >= range);
        return x;
    }

    public static <T> void shuffleList(List<T> list) {
        List<T> pickList = new ArrayList<>(list);
        list.clear();
        int size = pickList.size();

        for (int i = 0; i < size; i++) {
            int choice = randomIndex(pickList);
            list.add(pickList.get(choice));
            pickList.remove(choice);
        }
    }
}
