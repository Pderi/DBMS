package com.dbms.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据库类，管理多个表
 */
public class Database implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String name;                    // 数据库名称
    private Map<String, Table> tables;      // 表名 -> 表结构映射
    private String dbFilePath;              // .dbf文件路径
    private String datFilePath;              // .dat文件路径
    
    public Database() {
        this.tables = new HashMap<>();
    }
    
    public Database(String name) {
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
    
    public Map<String, Table> getTables() {
        return tables;
    }
    
    public void setTables(Map<String, Table> tables) {
        this.tables = tables;
    }
    
    public String getDbFilePath() {
        return dbFilePath;
    }
    
    public void setDbFilePath(String dbFilePath) {
        this.dbFilePath = dbFilePath;
    }
    
    public String getDatFilePath() {
        return datFilePath;
    }
    
    public void setDatFilePath(String datFilePath) {
        this.datFilePath = datFilePath;
    }
    
    /**
     * 添加表
     */
    public void addTable(Table table) {
        if (tables.containsKey(table.getName().toLowerCase())) {
            throw new IllegalArgumentException("Table " + table.getName() + " already exists");
        }
        tables.put(table.getName().toLowerCase(), table);
    }
    
    /**
     * 获取表
     */
    public Table getTable(String tableName) {
        return tables.get(tableName.toLowerCase());
    }
    
    /**
     * 删除表
     */
    public boolean removeTable(String tableName) {
        return tables.remove(tableName.toLowerCase()) != null;
    }
    
    /**
     * 检查表是否存在
     */
    public boolean hasTable(String tableName) {
        return tables.containsKey(tableName.toLowerCase());
    }
    
    /**
     * 获取所有表名
     */
    public List<String> getTableNames() {
        return new ArrayList<>(tables.keySet());
    }
    
    /**
     * 获取表数量
     */
    public int getTableCount() {
        return tables.size();
    }
}

