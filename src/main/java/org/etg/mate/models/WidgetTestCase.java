package org.etg.mate.models;

import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

public class WidgetTestCase {
    private String id;
    private Set<String> visitedActivities;
    private Set<String> visitedStates;
    private Vector<Action> eventSequence;
    private float novelty;
    private boolean crashDetected;
    private double sparseness;


    public WidgetTestCase(String id) {
        setId(id);
        crashDetected = false;
        visitedActivities = new HashSet<>();
        visitedStates = new HashSet<>();
        eventSequence = new Vector<>();
        sparseness = 0;

    }

    public String getId() {
        return id;
    }

    private void setId(String id) {
        this.id = id;
    }

    public void addEvent(Action event) {
        this.eventSequence.add(event);
    }

    ;

    public void updateVisitedActivities(String activity) {
        this.visitedActivities.add(activity);
    }

    ;

    public Set<String> getVisitedActivities() {
        return visitedActivities;
    }

    public Set<String> getVisitedStates() {
        return visitedStates;
    }

    public Vector<Action> getEventSequence() {
        return this.eventSequence;
    }

    ;

    public boolean getCrashDetected() {
        return this.crashDetected;
    }

    ;

    public void setCrashDetected() {
        this.crashDetected = true;
    }

    ;

    public void setNovelty(float novelty) {
        this.novelty = novelty;
    }

    public float getNovelty() {
        return novelty;
    }

    public double getSparseness() {
        return sparseness;
    }

    public void setSparseness(double sparseness) {
        this.sparseness = sparseness;
    }
}
