package org.etg.espresso.util;

import org.etg.utils.Tuple;

public class StringHelper {

    /**
     * trim (double) quotation marks in the ends of the given string
     * "\"txt\"" => "txt"
     *
     * @param org
     * @return
     */
    public static String trimQuotations(String org) {
        if (org.length() > 2 && org.charAt(0) == '"' && org.charAt(org.length() - 1) == '"')
            return org.substring(1, org.length() - 1);
        else
            return org;
    }


    /**
     * boxing string constant, including escape characters
     * "txt\n" => "\"txt\\n\""
     *
     * @param str
     * @return
     */
    public static String boxString(String str) {
        return "\"" + escapeStringCharacters(str) + "\"";
    }

    public static String lowerCaseFirstCharacter(String originalString) {
        if (originalString.isEmpty()) {
            return originalString;
        }
        return originalString.substring(0, 1).toLowerCase() + (originalString.length() > 1 ? originalString.substring(1) : "");
    }

    public static String upperCaseFirstCharacter(String originalString) {
        if (originalString.isEmpty()) {
            return originalString;
        }
        return originalString.substring(0, 1).toUpperCase() + (originalString.length() > 1 ? originalString.substring(1) : "");
    }

    public static String getClassName(String qualifiedClassName) {
        return qualifiedClassName.substring(qualifiedClassName.lastIndexOf(".") + 1);
    }

    /**
     * Returns a pair of (package_name, element_id).
     */
    public static Tuple parseId(String resourceId) {
        final String idMarker = ":id/";
        final int idMarkerIndex = resourceId.indexOf(idMarker);

        if (idMarkerIndex == -1) {
            // Unexpected resource id format.
            return null;
        }

        return new Tuple(resourceId.substring(0, idMarkerIndex), resourceId.substring(idMarkerIndex + idMarker.length()));
    }

    public static void escapeStringCharacters(int length, String str, StringBuilder buffer) {
        escapeStringCharacters(length, str, "\"", buffer);
    }

    public static StringBuilder escapeStringCharacters(int length,
                                                       String str,
                                                       String additionalChars,
                                                       StringBuilder buffer) {
        for (int idx = 0; idx < length; idx++) {
            char ch = str.charAt(idx);
            switch (ch) {
                case '\b':
                    buffer.append("\\b");
                    break;

                case '\t':
                    buffer.append("\\t");
                    break;

                case '\n':
                    buffer.append("\\n");
                    break;

                case '\f':
                    buffer.append("\\f");
                    break;

                case '\r':
                    buffer.append("\\r");
                    break;

                case '\\':
                    buffer.append("\\\\");
                    break;

                default:
                    if (additionalChars != null && additionalChars.indexOf(ch) > -1) {
                        buffer.append("\\").append(ch);
                    } else if (Character.isISOControl(ch)) {
                        String hexCode = Integer.toHexString(ch).toUpperCase();
                        buffer.append("\\u");
                        int paddingCount = 4 - hexCode.length();
                        while (paddingCount-- > 0) {
                            buffer.append(0);
                        }
                        buffer.append(hexCode);
                    } else {
                        buffer.append(ch);
                    }
            }
        }
        return buffer;
    }

    public static String escapeStringCharacters(String s) {
        StringBuilder buffer = new StringBuilder();
        escapeStringCharacters(s.length(), s, buffer);
        return buffer.toString();
    }

    public static boolean isNullOrEmpty(String s) {
        return s == null || s.isEmpty();
    }

    public static String upperCaseParts(String s, String delimiter) {
        String[] parts = s.split(delimiter);
        StringBuilder builder = new StringBuilder();
        for (String part: parts) {
            builder.append(upperCaseFirstCharacter(part));
        }

        return builder.toString();
    }
}
