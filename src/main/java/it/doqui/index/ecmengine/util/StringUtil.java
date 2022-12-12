package it.doqui.index.ecmengine.util;

import java.util.Collection;
import java.util.Map;

public class StringUtil {
    public static String toLength(String element) {
	String toReturn = null;
	if (element == null) {
	    toReturn = "null";
	} else {
	    toReturn = "" + element.length();
	}
	return toReturn;
    }

    public static String toLength(byte[] element) {
	String toReturn = null;
	if (element == null) {
	    toReturn = "null";
	} else {
	    toReturn = "" + element.length;
	}
	return toReturn;
    }

    public static <T> String toLength(T[] element) {
	String toReturn = null;
	if (element == null) {
	    toReturn = "null";
	} else {
	    toReturn = "" + element.length;
	}
	return toReturn;
    }

    public static String toLength(Collection<?> element) {
	String toReturn = null;
	if (element == null) {
	    toReturn = "null";
	} else {
	    toReturn = "" + element.size();
	}
	return toReturn;
    }

    public static String toLength(Map<?, ?> element) {
	String toReturn = null;
	if (element == null) {
	    toReturn = "null";
	} else {
	    toReturn = "" + element.size();
	}
	return toReturn;
    }
}
