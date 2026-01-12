package com.dbms.engine;

import com.dbms.model.Database;
import com.dbms.model.Field;
import com.dbms.model.FieldType;
import com.dbms.model.Table;
import com.dbms.storage.DBFFileManager;
import com.dbms.util.DBMSException;
import com.dbms.util.Validator;

import java.io.File;
import java.io.IOException;

/**
 * DDL执行器 - 处理数据定义语言（CREATE, ALTER, DROP等）
 */
public class DDLExecutor {
    
    private Database database;
    private String dbFilePath;
    private String datFilePath;
    
    public DDLExecutor(Database database, String dbFilePath, String datFilePath) {
        this.database = database;
        this.dbFilePath = dbFilePath;
        this.datFilePath = datFilePath;
    }
    
    /**
     * 创建表
     */
    public void createTable(String tableName, java.util.List<Field> fields) {
        if (!Validator.isValidTableName(tableName)) {
            throw new DBMSException("Invalid table name: " + tableName);
        }
        
        if (database.hasTable(tableName)) {
            throw new DBMSException("Table " + tableName + " already exists");
        }
        
        Table table = new Table(tableName);
        for (Field field : fields) {
            if (!Validator.isValidFieldName(field.getName())) {
                throw new DBMSException("Invalid field name: " + field.getName());
            }
            table.addField(field);
        }
        
        database.addTable(table);
        
        try {
            // 保存到文件
            DBFFileManager.createDatabaseFile(dbFilePath, database);
            // 为每个表创建独立的数据文件
            String tableDataFile = com.dbms.storage.DATFileManager.getTableDataFilePath(datFilePath, tableName);
            File datFile = new File(tableDataFile);
            if (!datFile.exists()) {
                datFile.createNewFile();
            }
        } catch (IOException e) {
            throw new DBMSException("Failed to create table: " + e.getMessage(), e);
        }
    }
    
    /**
     * 删除表
     */
    public void dropTable(String tableName) {
        if (!database.hasTable(tableName)) {
            throw new DBMSException("Table " + tableName + " does not exist");
        }
        
        // 删除表的数据文件
        String tableDataFile = com.dbms.storage.DATFileManager.getTableDataFilePath(datFilePath, tableName);
        File dataFile = new File(tableDataFile);
        if (dataFile.exists()) {
            boolean deleted = dataFile.delete();
            if (!deleted) {
                throw new DBMSException("Failed to delete data file for table: " + tableName);
            }
        }
        
        // 从数据库中移除表
        database.removeTable(tableName);
        
        try {
            // 更新数据库文件
            DBFFileManager.createDatabaseFile(dbFilePath, database);
        } catch (IOException e) {
            throw new DBMSException("Failed to drop table: " + e.getMessage(), e);
        }
    }
    
    /**
     * 重命名表
     */
    public void renameTable(String oldName, String newName) {
        if (!database.hasTable(oldName)) {
            throw new DBMSException("Table " + oldName + " does not exist");
        }
        
        if (!Validator.isValidTableName(newName)) {
            throw new DBMSException("Invalid new table name: " + newName);
        }
        
        if (database.hasTable(newName)) {
            throw new DBMSException("Table " + newName + " already exists");
        }
        
        Table table = database.getTable(oldName);
        table.setName(newName);
        database.removeTable(oldName);
        database.addTable(table);
        
        try {
            DBFFileManager.createDatabaseFile(dbFilePath, database);
        } catch (IOException e) {
            throw new DBMSException("Failed to rename table: " + e.getMessage(), e);
        }
    }
    
    /**
     * 添加字段
     */
    public void addColumn(String tableName, Field field) {
        Table table = database.getTable(tableName);
        if (table == null) {
            throw new DBMSException("Table " + tableName + " does not exist");
        }
        
        if (!Validator.isValidFieldName(field.getName())) {
            throw new DBMSException("Invalid field name: " + field.getName());
        }
        
        if (table.getFieldByName(field.getName()) != null) {
            throw new DBMSException("Field " + field.getName() + " already exists");
        }
        
        table.addField(field);
        
        try {
            DBFFileManager.updateTableInFile(dbFilePath, table);
        } catch (IOException e) {
            throw new DBMSException("Failed to add column: " + e.getMessage(), e);
        }
    }
    
    /**
     * 删除字段
     */
    public void dropColumn(String tableName, String columnName) {
        Table table = database.getTable(tableName);
        if (table == null) {
            throw new DBMSException("Table " + tableName + " does not exist");
        }
        
        Field field = table.getFieldByName(columnName);
        if (field == null) {
            throw new DBMSException("Column " + columnName + " does not exist");
        }
        
        if (field.isKey()) {
            throw new DBMSException("Cannot drop primary key column: " + columnName);
        }
        
        table.removeField(columnName);
        
        try {
            DBFFileManager.updateTableInFile(dbFilePath, table);
        } catch (IOException e) {
            throw new DBMSException("Failed to drop column: " + e.getMessage(), e);
        }
    }
    
    /**
     * 修改字段名
     */
    public void renameColumn(String tableName, String oldName, String newName) {
        Table table = database.getTable(tableName);
        if (table == null) {
            throw new DBMSException("Table " + tableName + " does not exist");
        }
        
        Field field = table.getFieldByName(oldName);
        if (field == null) {
            throw new DBMSException("Column " + oldName + " does not exist");
        }
        
        if (!Validator.isValidFieldName(newName)) {
            throw new DBMSException("Invalid new field name: " + newName);
        }
        
        if (table.getFieldByName(newName) != null) {
            throw new DBMSException("Column " + newName + " already exists");
        }
        
        field.setName(newName);
        
        try {
            DBFFileManager.updateTableInFile(dbFilePath, table);
        } catch (IOException e) {
            throw new DBMSException("Failed to rename column: " + e.getMessage(), e);
        }
    }
    
    /**
     * 修改字段类型
     */
    public void modifyColumn(String tableName, String columnName, FieldType newType, Integer newLength) {
        Table table = database.getTable(tableName);
        if (table == null) {
            throw new DBMSException("Table " + tableName + " does not exist");
        }
        
        Field field = table.getFieldByName(columnName);
        if (field == null) {
            throw new DBMSException("Column " + columnName + " does not exist");
        }
        
        field.setType(newType);
        if (newLength != null) {
            field.setLength(newLength);
        } else if (newType.getDefaultLength() > 0) {
            field.setLength(newType.getDefaultLength());
        }
        
        try {
            DBFFileManager.updateTableInFile(dbFilePath, table);
        } catch (IOException e) {
            throw new DBMSException("Failed to modify column: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取表结构
     */
    public Table getTable(String tableName) {
        return database.getTable(tableName);
    }
    
    /**
     * 获取所有表名
     */
    public java.util.List<String> getTableNames() {
        return database.getTableNames();
    }
}

