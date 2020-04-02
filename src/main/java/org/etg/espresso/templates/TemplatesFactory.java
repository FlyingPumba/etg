package org.etg.espresso.templates;


/**
 * In charge of create the needed templates to perform specific operation
 * **/
public class TemplatesFactory {

    public enum Template {
        CLICK_ACTION,
        LONG_CLICK_ACTION
    }

    public VelocityTemplate createFor(Template neededTemplate){
        switch (neededTemplate){
            case CLICK_ACTION:
            case LONG_CLICK_ACTION:
                return new ClickWithoutDisplayConstraintTemplate();
            default: throw new IllegalArgumentException("Invalid template");
        }
    }

}
