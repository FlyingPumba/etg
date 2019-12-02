package org.etg.mate.models;

import java.awt.Point;

public class Swipe {

    private Point initialPosition;
    private Point finalPosition;
    private int steps;

    public Swipe(Point initialPosition, Point finalPosition, int steps) {
        this.initialPosition = initialPosition;
        this.finalPosition = finalPosition;
        this.steps = steps;
    }

    public Point getFinalPosition() {
        return finalPosition;
    }

    public Point getInitialPosition() {
        return initialPosition;
    }

    public int getSteps() {
        return steps;
    }
}

