package org.etg.espresso.util;

import org.apache.commons.io.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class ResourceHelper {

    public static File getFileForResource(Object resourceRequester, String resourcePath, String prefix,
                                          String suffix) {
        InputStream in = resourceRequester.getClass().getClassLoader().getResourceAsStream(resourcePath);
        FileOutputStream out = null;
        try {
            // TODO: Re-use unpacked resource across Test Recorder runs (for performance).
            File tempFile = File.createTempFile(prefix, suffix);
            tempFile.deleteOnExit();
            out = new FileOutputStream(tempFile);
            IOUtils.copy(in, out);
            return tempFile;
        } catch (Exception e) {
            throw new RuntimeException("Could not create file for resource " + resourcePath, e);
        } finally {
            try {
                in.close();
                if (out != null) {
                    out.close();
                }
            } catch (Exception e) {
                // ignore
            }
        }
    }

    public static byte[] readResourceFully(Object resourceRequester, String resourcePath) {
        ByteArrayOutputStream readBytes = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int count;
        InputStream in = resourceRequester.getClass().getClassLoader().getResourceAsStream(resourcePath);
        try {
            while ((count = in.read(buffer)) != -1) {
                readBytes.write(buffer, 0, count);
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not read resource " + resourcePath, e);
        } finally {
            try {
                in.close();
            } catch (Exception e) {
                // ignore
            }
        }
        return readBytes.toByteArray();
    }

}
