package it.doqui.index.ecmengineqs.utils;

import java.util.Arrays;
import java.util.List;

public class ObjectUtils {

    public static String getAsString(Object obj) {
        return obj == null ? null : obj.toString();
    }

    public static boolean getAsBoolean(Object obj, boolean defaultValue) {
        if (obj != null && obj instanceof Boolean) {
            return ((Boolean) obj).booleanValue();
        }

        return defaultValue;
    }

    public static <T> List<T> asList(T... objects) {
        return objects == null ? null : Arrays.asList(objects);
    }

    public static <T> T coalesce(T... objects) {
        for (T obj : objects) {
            if (obj != null) {
                return obj;
            }
        }

        return null;
    }
}
