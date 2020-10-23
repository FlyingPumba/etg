package org.etg.mate.models;

import java.util.List;
import java.util.Objects;
import java.util.Vector;

public class Action {

    private Widget rootWidget;
    private Vector<Integer> targetWidgetPath = new Vector<>();
    private ActionType actionType;
    private String extraInfo;
    private boolean executed;

    private float fitness;
    private long timeToWait;
    private float pheromone;
    private float proportionalPheromone;
    private Swipe swipe;
    private List<String> networkingInfo;

    private Vector<Action> adjActions;

    public Vector<Action> getAdjActions() {
        return adjActions;
    }

    public Action(ActionType actionType) {
        this.actionType = actionType;
        fitness = 0;
        rootWidget = new Widget("", "", "");
    }

    public Action(Widget rootWidget, Vector<Integer> widgetPath, ActionType actionType) {
        setActionType(actionType);

        setRootWidget(rootWidget);
        targetWidgetPath.addAll(widgetPath);

        setExtraInfo("");
        adjActions = new Vector<Action>();
        setExecuted(false);
    }

    public Swipe getSwipe() {
        return swipe;
    }

    public void setSwipe(Swipe swipe) {
        this.swipe = swipe;
    }

    public void setExecuted(boolean executed) {
        this.executed = executed;
    }

    public Widget getWidget() {
        Widget targetWidget = rootWidget;
        for (Integer index : targetWidgetPath) {
            targetWidget = targetWidget.getChildren().get(index);
        }
        return targetWidget;
    }

    public void setRootWidget(Widget widget) {
        this.rootWidget = widget;
    }

    public Widget getRootWidget() {
        return rootWidget;
    }

    public Vector<Integer> getTargetWidgetPath() {
        return targetWidgetPath;
    }

    public ActionType getActionType() {
        return actionType;
    }

    public void setActionType(ActionType actionType) {
        this.actionType = actionType;
    }

    public String getExtraInfo() {
        return extraInfo;
    }

    public void setExtraInfo(String extraInfo) {
        this.extraInfo = extraInfo;
    }

    public void addAdjAction(Action eventAction) {
        adjActions.add(eventAction);
    }

    public void setFitness(float fitness) {
        this.fitness = fitness;
    }

    public long getTimeToWait() {
        return timeToWait;
    }

    public void setTimeToWait(long timeToWait) {
        this.timeToWait = timeToWait;
    }

    public float getPheromone() {
        return pheromone;
    }

    public void setPheromone(float pheromone) {
        this.pheromone = pheromone;
    }

    public float getProportionalPheromone() {
        return proportionalPheromone;
    }

    public void setProportionalPheromone(float proportionalPheromone) {
        this.proportionalPheromone = proportionalPheromone;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Action action = (Action) o;
        return actionType == action.actionType &&
                Objects.equals(getWidget().getIdByActivity(), action.getWidget().getIdByActivity());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getWidget().getIdByActivity(), actionType);
    }

    public List<String> getNetworkingInfo() {
        return networkingInfo;
    }

    public void setNetworkingInfo(List<String> networkingInfo) {
        this.networkingInfo = networkingInfo;
    }
}
