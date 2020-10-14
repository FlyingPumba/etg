package org.etg.espresso.templates;


import org.etg.ETGProperties;
import org.etg.espresso.templates.java.ClickWithoutDisplayConstraintJavaTemplate;
import org.etg.espresso.templates.kotlin.utils.ClickWithoutDisplayConstraintKotlinTemplate;

/**
 * In charge of create the needed templates to perform specific operation
 * **/
public class TemplatesFactory {

    public enum Template {
        CLICK_ACTION,
        LONG_CLICK_ACTION
    }

    public VelocityTemplate createFor(Template neededTemplate, ETGProperties etgProperties){
        switch (neededTemplate){
            case CLICK_ACTION:
            case LONG_CLICK_ACTION:
                if (etgProperties.useKotlinFormat()) {
                    return new ClickWithoutDisplayConstraintKotlinTemplate();
                } else {
                    return new ClickWithoutDisplayConstraintJavaTemplate();
                }
            default: throw new IllegalArgumentException("Invalid template");
        }
    }

}
