package org.etg.espresso.templates;


/**
 * In charge of create the needed templates to perform specific operation
 * **/
public class TemplatesFactory {

    public enum Template {
        CLICK_ACTION
    }

    public VelocityTemplate createFor(Template neededTemplate){
        switch (neededTemplate){
            case CLICK_ACTION:
            default:
                return new ClickWithoutConstraintTemplate();

        }
    }

}
