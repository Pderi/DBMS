package com.dbms.model;

import java.io.Serializable;

/**
 * 字段定义类
 */
public class Field implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String name;           // 字段名
    private FieldType type;        // 字段类型
    private int length;            // 字段长度
    private boolean isKey;         // 是否为主键
    private boolean nullable;      // 是否允许为NULL
    private String defaultValue;   // 默认值（预留）
    
    public Field() {
    }
    
    public Field(String name, FieldType type, int length, boolean isKey, boolean nullable) {
        this.name = name;
        this.type = type;
        this.length = length;
        this.isKey = isKey;
        this.nullable = nullable;
        this.defaultValue = null;
    }
    
    // Getters and Setters
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public FieldType getType() {
        return type;
    }
    
    public void setType(FieldType type) {
        this.type = type;
    }
    
    public int getLength() {
        return length;
    }
    
    public void setLength(int length) {
        this.length = length;
    }
    
    public boolean isKey() {
        return isKey;
    }
    
    public void setKey(boolean key) {
        isKey = key;
    }
    
    public boolean isNullable() {
        return nullable;
    }
    
    public void setNullable(boolean nullable) {
        this.nullable = nullable;
    }
    
    public String getDefaultValue() {
        return defaultValue;
    }
    
    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }
    
    /**
     * 获取字段的实际存储大小（字节）
     */
    public int getStorageSize() {
        if (type == FieldType.VARCHAR) {
            return length + 4; // 变长字段：4字节长度前缀 + 数据
        } else if (type == FieldType.INT) {
            return 4;
        } else if (type == FieldType.FLOAT) {
            return 8;
        } else if (type == FieldType.DATE) {
            return 8;
        } else {
            return length; // CHAR类型
        }
    }
    
    @Override
    public String toString() {
        return String.format("%s %s(%d) %s %s", 
            name, 
            type.getSqlName(), 
            length,
            isKey ? "PRIMARY KEY" : "",
            nullable ? "NULL" : "NOT NULL");
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Field field = (Field) obj;
        return name.equals(field.name);
    }
    
    @Override
    public int hashCode() {
        return name.hashCode();
    }
}

