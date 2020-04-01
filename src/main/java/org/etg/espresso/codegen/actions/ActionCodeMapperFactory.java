package org.etg.espresso.codegen.actions;

import org.etg.ETGProperties;
import org.etg.mate.models.Action;

public class ActionCodeMapperFactory {

    public static ActionCodeMapper get(ETGProperties etgProperties, Action action) {
        switch (action.getActionType()) {
            case CLICK:
                return new ClickActionCodeMapper(etgProperties, action);
            case LONG_CLICK:
                return new LongClickActionCodeMapper(etgProperties, action);
            case TYPE_TEXT:
                return new TypeTextActionCodeMapper(etgProperties, action);
            case SWIPE_UP:
            case SWIPE_DOWN:
            case SWIPE_LEFT:
            case SWIPE_RIGHT:
                return new SwipeActionCodeMapper(etgProperties, action);
            case BACK:
                return new BackActionCodeMapper(etgProperties, action);
            case MENU:
                return new MenuActionCodeMapper(etgProperties, action);
            case ENTER:
                return new EnterActionCodeMapper(etgProperties, action);
            case WAIT:
            case RESTART:
            case REINSTALL:
            case CLEAR_WIDGET:
                throw new RuntimeException("Unsupported event type: " + action.getActionType());
        }

        throw new RuntimeException("Unsupported event type: " + action.getActionType());
    }
}
