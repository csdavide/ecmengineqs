package it.doqui.index.ecmengineqs.utils;

import java.sql.ResultSet;
import java.sql.SQLException;

public class DBUtils {

    public static Boolean getBoolean(ResultSet rs, String columnName) throws SQLException {
        boolean value = rs.getBoolean(columnName);
        return rs.wasNull() ? null : value;
    }

    public static Byte getByte(ResultSet rs, String columnName) throws SQLException {
        byte value = rs.getByte(columnName);
        return rs.wasNull() ? null : value;
    }

    public static Long getLong(ResultSet rs, String columnName) throws SQLException {
        long value = rs.getLong(columnName);
        return rs.wasNull() ? null : value;
    }

    public static Integer getInt(ResultSet rs, String columnName) throws SQLException {
        int value = rs.getInt(columnName);
        return rs.wasNull() ? null : value;
    }

    public static Short getShort(ResultSet rs, String columnName) throws SQLException {
        short value = rs.getShort(columnName);
        return rs.wasNull() ? null : value;
    }

    public static Float getFloat(ResultSet rs, String columnName) throws SQLException {
        float value = rs.getFloat(columnName);
        return rs.wasNull() ? null : value;
    }

    public static Double getDouble(ResultSet rs, String columnName) throws SQLException {
        double value = rs.getFloat(columnName);
        return rs.wasNull() ? null : value;
    }

}
