package com.dbms.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 记录类
 */
public class Record implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private List<Object> values;    // 字段值列表
    private boolean deleted;        // 是否已删除（用于逻辑删除）
    private long recordId;           // 记录ID（预留）
    
    public Record() {
        this.values = new ArrayList<>();
        this.deleted = false;
        this.recordId = -1;
    }
    
    public Record(int fieldCount) {
        this();
        for (int i = 0; i < fieldCount; i++) {
            values.add(null);
        }
    }
    
    // Getters and Setters
    public List<Object> getValues() {
        return values;
    }
    
    public void setValues(List<Object> values) {
        this.values = values;
    }
    
    public boolean isDeleted() {
        return deleted;
    }
    
    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }
    
    public long getRecordId() {
        return recordId;
    }
    
    public void setRecordId(long recordId) {
        this.recordId = recordId;
    }
    
    /**
     * 设置字段值
     */
    public void setValue(int index, Object value) {
        if (index < 0 || index >= values.size()) {
            throw new IndexOutOfBoundsException("Field index out of bounds: " + index);
        }
        values.set(index, value);
    }
    
    /**
     * 获取字段值
     */
    public Object getValue(int index) {
        if (index < 0 || index >= values.size()) {
            throw new IndexOutOfBoundsException("Field index out of bounds: " + index);
        }
        return values.get(index);
    }
    
    /**
     * 根据字段名获取值（需要传入表结构）
     */
    public Object getValue(Table table, String fieldName) {
        Field field = table.getFieldByName(fieldName);
        if (field == null) {
            throw new IllegalArgumentException("Field not found: " + fieldName);
        }
        int index = table.getFields().indexOf(field);
        return getValue(index);
    }
    
    /**
     * 添加值
     */
    public void addValue(Object value) {
        values.add(value);
    }
    
    /**
     * 获取值数量
     */
    public int getValueCount() {
        return values.size();
    }
    
    @Override
    public String toString() {
        return values.toString();
    }
    
    /**
     * 转换为字符串数组（用于表格显示）
     */
    public String[] toStringArray() {
        String[] result = new String[values.size()];
        for (int i = 0; i < values.size(); i++) {
            Object value = values.get(i);
            result[i] = value == null ? "NULL" : value.toString();
        }
        return result;
    }
}

