package org.etg.espresso.templates;


/**
 * Velocity's template interface
 * **/
public interface VelocityTemplate {
    String getFileName();
    String getRelativePath();
    String getAsRawString();
    boolean equals(Object o);
    int hashCode();
}
