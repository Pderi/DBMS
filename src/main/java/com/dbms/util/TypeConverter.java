package com.dbms.util;

import com.dbms.model.FieldType;

/**
 * 类型转换工具类
 */
public class TypeConverter {
    
    /**
     * 将字符串转换为指定类型的对象
     */
    public static Object convertValue(String value, FieldType type) {
        if (value == null || value.trim().isEmpty() || value.equalsIgnoreCase("NULL")) {
            return null;
        }
        
        value = value.trim();
        
        try {
            switch (type) {
                case INT:
                    return Integer.parseInt(value);
                case FLOAT:
                    return Double.parseDouble(value);
                case VARCHAR:
                case CHAR:
                    return value;
                case DATE:
                    // 简单日期处理，实际可以扩展为Date对象
                    return value;
                default:
                    return value;
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Cannot convert '" + value + "' to type " + type);
        }
    }
    
    /**
     * 验证值是否符合字段类型
     */
    public static boolean validateValue(Object value, FieldType type, int maxLength) {
        if (value == null) {
            return true; // NULL值由nullable标志控制
        }
        
        switch (type) {
            case INT:
                return value instanceof Integer;
            case FLOAT:
                return value instanceof Double || value instanceof Float;
            case VARCHAR:
            case CHAR:
                if (value instanceof String) {
                    String str = (String) value;
                    if (type == FieldType.CHAR && str.length() != maxLength) {
                        return false;
                    }
                    return str.length() <= maxLength;
                }
                return false;
            case DATE:
                return value instanceof String; // 简化处理
            default:
                return true;
        }
    }
    
    /**
     * 将对象转换为字符串（用于存储）
     */
    public static String valueToString(Object value) {
        if (value == null) {
            return "";
        }
        return value.toString();
    }
}

