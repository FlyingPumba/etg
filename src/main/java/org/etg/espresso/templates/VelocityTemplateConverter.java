package org.etg.espresso.templates;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;

import java.io.StringWriter;
import java.io.Writer;

/**
 * Given a IVelocityTemplate and a VelocityContext generate the string template
 * **/
public class VelocityTemplateConverter {

    private VelocityContext velocityContext;

    public VelocityTemplateConverter(VelocityContext velocityContext) {
        this.velocityContext = velocityContext;
    }

    public String applyContextToTemplate(VelocityTemplate template){
        Writer writer = null;
        try {
            writer = new StringWriter();

            VelocityEngine velocityEngine = new VelocityEngine();
            // Suppress creation of velocity.log file.
            velocityEngine.setProperty(RuntimeConstants.RUNTIME_LOG_LOGSYSTEM_CLASS, "org.apache.velocity.runtime.log.NullLogChute");
            velocityEngine.init();
            velocityEngine.evaluate(velocityContext, writer, "mystring", template.getAsRawString());
            writer.flush();

            return writer.toString();

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate test class file: ", e);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (Exception e) {
                    // ignore
                }
            }
        }
    }

}
