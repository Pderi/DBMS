package com.dbms.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 表结构定义类
 */
public class Table implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String name;                    // 表名
    private List<Field> fields;             // 字段列表
    private int recordCount;                 // 记录数量（预留）
    private long lastModified;               // 最后修改时间（预留）
    
    public Table() {
        this.fields = new ArrayList<>();
        this.recordCount = 0;
        this.lastModified = System.currentTimeMillis();
    }
    
    public Table(String name) {
        this();
        this.name = name;
    }
    
    // Getters and Setters
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public List<Field> getFields() {
        return fields;
    }
    
    public void setFields(List<Field> fields) {
        this.fields = fields;
    }
    
    public int getRecordCount() {
        return recordCount;
    }
    
    public void setRecordCount(int recordCount) {
        this.recordCount = recordCount;
    }
    
    public long getLastModified() {
        return lastModified;
    }
    
    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }
    
    /**
     * 添加字段
     */
    public void addField(Field field) {
        if (getFieldByName(field.getName()) != null) {
            throw new IllegalArgumentException("Field " + field.getName() + " already exists");
        }
        fields.add(field);
    }
    
    /**
     * 根据名称获取字段
     */
    public Field getFieldByName(String fieldName) {
        for (Field field : fields) {
            if (field.getName().equalsIgnoreCase(fieldName)) {
                return field;
            }
        }
        return null;
    }
    
    /**
     * 根据索引获取字段
     */
    public Field getFieldByIndex(int index) {
        if (index < 0 || index >= fields.size()) {
            return null;
        }
        return fields.get(index);
    }
    
    /**
     * 删除字段
     */
    public boolean removeField(String fieldName) {
        Field field = getFieldByName(fieldName);
        if (field != null) {
            return fields.remove(field);
        }
        return false;
    }
    
    /**
     * 获取主键字段列表
     */
    public List<Field> getKeyFields() {
        List<Field> keys = new ArrayList<>();
        for (Field field : fields) {
            if (field.isKey()) {
                keys.add(field);
            }
        }
        return keys;
    }
    
    /**
     * 计算每条记录的大小（字节）
     */
    public int getRecordSize() {
        int size = 4; // 记录头：4字节状态标志（预留）
        for (Field field : fields) {
            size += field.getStorageSize();
        }
        return size;
    }
    
    /**
     * 获取字段数量
     */
    public int getFieldCount() {
        return fields.size();
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Table: ").append(name).append("\n");
        sb.append("Fields:\n");
        for (Field field : fields) {
            sb.append("  ").append(field.toString()).append("\n");
        }
        return sb.toString();
    }
}

