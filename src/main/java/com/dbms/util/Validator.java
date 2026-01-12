package com.dbms.util;

import com.dbms.model.Field;
import com.dbms.model.Record;
import com.dbms.model.Table;

/**
 * 数据验证工具类
 */
public class Validator {
    
    /**
     * 验证记录是否符合表结构要求
     */
    public static void validateRecord(Record record, Table table) {
        if (record.getValueCount() != table.getFieldCount()) {
            throw new IllegalArgumentException(
                "Record field count (" + record.getValueCount() + 
                ") does not match table field count (" + table.getFieldCount() + ")");
        }
        
        for (int i = 0; i < table.getFieldCount(); i++) {
            Field field = table.getFieldByIndex(i);
            Object value = record.getValue(i);
            
            // 检查NULL约束
            if (value == null && !field.isNullable()) {
                throw new IllegalArgumentException(
                    "Field '" + field.getName() + "' cannot be NULL");
            }
            
            // 检查类型和长度
            if (value != null) {
                if (!TypeConverter.validateValue(value, field.getType(), field.getLength())) {
                    throw new IllegalArgumentException(
                        "Invalid value for field '" + field.getName() + 
                        "': type mismatch or length exceeded");
                }
            }
        }
    }
    
    /**
     * 验证字段名是否合法
     */
    public static boolean isValidFieldName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        // 字段名只能包含字母、数字和下划线，且不能以数字开头
        return name.matches("^[a-zA-Z_][a-zA-Z0-9_]*$");
    }
    
    /**
     * 验证表名是否合法
     */
    public static boolean isValidTableName(String name) {
        return isValidFieldName(name);
    }
}

