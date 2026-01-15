package com.dbms.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 索引定义类
 */
public class Index implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String indexName;      // 索引名
    private String tableName;      // 表名
    private String columnName;     // 字段名
    private boolean unique;        // 是否唯一索引
    private Map<Object, List<Long>> indexMap;  // 索引映射：值 -> 记录位置列表
    
    public Index() {
        this.indexMap = new HashMap<>();
    }
    
    public Index(String indexName, String tableName, String columnName, boolean unique) {
        this();
        this.indexName = indexName;
        this.tableName = tableName;
        this.columnName = columnName;
        this.unique = unique;
    }
    
    // Getters and Setters
    public String getIndexName() {
        return indexName;
    }
    
    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }
    
    public String getTableName() {
        return tableName;
    }
    
    public void setTableName(String tableName) {
        this.tableName = tableName;
    }
    
    public String getColumnName() {
        return columnName;
    }
    
    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }
    
    public boolean isUnique() {
        return unique;
    }
    
    public void setUnique(boolean unique) {
        this.unique = unique;
    }
    
    public Map<Object, List<Long>> getIndexMap() {
        return indexMap;
    }
    
    public void setIndexMap(Map<Object, List<Long>> indexMap) {
        this.indexMap = indexMap;
    }
    
    /**
     * 添加索引项
     */
    public void addIndexEntry(Object value, Long position) {
        indexMap.computeIfAbsent(value, k -> new java.util.ArrayList<>()).add(position);
    }
    
    /**
     * 删除索引项
     */
    public void removeIndexEntry(Object value, Long position) {
        List<Long> positions = indexMap.get(value);
        if (positions != null) {
            positions.remove(position);
            if (positions.isEmpty()) {
                indexMap.remove(value);
            }
        }
    }
    
    /**
     * 查找索引项
     */
    public List<Long> find(Object value) {
        return indexMap.getOrDefault(value, new java.util.ArrayList<>());
    }
    
    @Override
    public String toString() {
        return String.format("Index: %s ON %s(%s) %s", 
            indexName, tableName, columnName, unique ? "UNIQUE" : "");
    }
}

