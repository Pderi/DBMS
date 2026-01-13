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
                case DOUBLE:
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
                // INT类型接受Integer，也接受可以转换为Integer的数字类型
                if (value instanceof Integer) {
                    return true;
                }
                // 允许Long、Short、Byte等整数类型
                if (value instanceof Long || value instanceof Short || value instanceof Byte) {
                    return true;
                }
                // 允许Double或Float，如果它们可以无损转换为Integer
                if (value instanceof Double || value instanceof Float) {
                    Number num = (Number) value;
                    double d = num.doubleValue();
                    // 检查是否在Integer范围内且是整数
                    return d >= Integer.MIN_VALUE && d <= Integer.MAX_VALUE && 
                           d == Math.floor(d);
                }
                return false;
            case FLOAT:
            case DOUBLE:
                return value instanceof Double || value instanceof Float || 
                       value instanceof Integer || value instanceof Long;
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

