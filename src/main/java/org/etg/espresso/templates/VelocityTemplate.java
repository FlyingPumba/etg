package org.etg.espresso.templates;


/**
 * Velocity's template trait
 * **/
public interface VelocityTemplate {

    String getName();
    String getAsRawString();
    boolean equals(Object o);
    int hashCode();

}
