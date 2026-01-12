package com.dbms.engine;

import com.dbms.model.Field;
import com.dbms.model.Record;
import com.dbms.model.Table;
import com.dbms.storage.DATFileManager;
import com.dbms.util.DBMSException;
import com.dbms.util.TypeConverter;
import com.dbms.util.Validator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * DML执行器 - 处理数据操纵语言（INSERT, UPDATE, DELETE）
 */
public class DMLExecutor {
    
    private DDLExecutor ddlExecutor;
    private String baseDatFilePath;  // 基础数据文件路径（用于生成表特定的路径）
    
    public DMLExecutor(DDLExecutor ddlExecutor, String baseDatFilePath) {
        this.ddlExecutor = ddlExecutor;
        this.baseDatFilePath = baseDatFilePath;
    }
    
    /**
     * 获取表的数据文件路径
     */
    private String getTableDataFilePath(String tableName) {
        return com.dbms.storage.DATFileManager.getTableDataFilePath(baseDatFilePath, tableName);
    }
    
    /**
     * 插入记录
     */
    public void insert(String tableName, List<Object> values) {
        Table table = ddlExecutor.getTable(tableName);
        if (table == null) {
            throw new DBMSException("Table " + tableName + " does not exist");
        }
        
        if (values.size() != table.getFieldCount()) {
            throw new DBMSException(
                "Value count (" + values.size() + ") does not match field count (" + 
                table.getFieldCount() + ")");
        }
        
        Record record = new Record(table.getFieldCount());
        for (int i = 0; i < values.size(); i++) {
            Field field = table.getFieldByIndex(i);
            Object value = values.get(i);
            
            // 转换值类型
            if (value instanceof String) {
                value = TypeConverter.convertValue((String) value, field.getType());
            }
            
            record.setValue(i, value);
        }
        
        // 验证记录
        Validator.validateRecord(record, table);
        
        // 写入文件
        try {
            String tableDataFile = getTableDataFilePath(tableName);
            DATFileManager.appendRecord(tableDataFile, record, table);
            // 更新表的记录计数
            table.setRecordCount(table.getRecordCount() + 1);
        } catch (IOException e) {
            throw new DBMSException("Failed to insert record: " + e.getMessage(), e);
        }
    }
    
    /**
     * 插入记录（指定字段名）
     */
    public void insert(String tableName, List<String> columnNames, List<Object> values) {
        Table table = ddlExecutor.getTable(tableName);
        if (table == null) {
            throw new DBMSException("Table " + tableName + " does not exist");
        }
        
        if (columnNames.size() != values.size()) {
            throw new DBMSException("Column count does not match value count");
        }
        
        Record record = new Record(table.getFieldCount());
        
        // 初始化所有字段为NULL
        for (int i = 0; i < table.getFieldCount(); i++) {
            record.setValue(i, null);
        }
        
        // 设置指定字段的值
        for (int i = 0; i < columnNames.size(); i++) {
            String columnName = columnNames.get(i);
            Field field = table.getFieldByName(columnName);
            if (field == null) {
                throw new DBMSException("Column " + columnName + " does not exist");
            }
            
            int fieldIndex = table.getFields().indexOf(field);
            Object value = values.get(i);
            
            // 转换值类型
            if (value instanceof String) {
                value = TypeConverter.convertValue((String) value, field.getType());
            }
            
            record.setValue(fieldIndex, value);
        }
        
        // 验证记录
        Validator.validateRecord(record, table);
        
        // 写入文件
        try {
            String tableDataFile = getTableDataFilePath(tableName);
            DATFileManager.appendRecord(tableDataFile, record, table);
            table.setRecordCount(table.getRecordCount() + 1);
        } catch (IOException e) {
            throw new DBMSException("Failed to insert record: " + e.getMessage(), e);
        }
    }
    
    /**
     * 删除记录（根据条件）
     */
    public int delete(String tableName, QueryCondition condition) {
        Table table = ddlExecutor.getTable(tableName);
        if (table == null) {
            throw new DBMSException("Table " + tableName + " does not exist");
        }
        
        try {
            String tableDataFile = getTableDataFilePath(tableName);
            List<Record> records = DATFileManager.readAllRecords(tableDataFile, table);
            List<Long> positions = getRecordPositions(tableDataFile, table);
            
            int deletedCount = 0;
            for (int i = 0; i < records.size(); i++) {
                Record record = records.get(i);
                if (!record.isDeleted() && (condition == null || condition.matches(record, table))) {
                    long position = positions.get(i);
                    DATFileManager.deleteRecord(tableDataFile, position);
                    deletedCount++;
                }
            }
            
            return deletedCount;
        } catch (IOException e) {
            throw new DBMSException("Failed to delete records: " + e.getMessage(), e);
        }
    }
    
    /**
     * 更新记录
     */
    public int update(String tableName, List<String> columnNames, List<Object> values, 
                     QueryCondition condition) {
        Table table = ddlExecutor.getTable(tableName);
        if (table == null) {
            throw new DBMSException("Table " + tableName + " does not exist");
        }
        
        if (columnNames.size() != values.size()) {
            throw new DBMSException("Column count does not match value count");
        }
        
        try {
            String tableDataFile = getTableDataFilePath(tableName);
            List<Record> records = DATFileManager.readAllRecords(tableDataFile, table);
            List<Long> positions = getRecordPositions(tableDataFile, table);
            
            int updatedCount = 0;
            for (int i = 0; i < records.size(); i++) {
                Record record = records.get(i);
                if (!record.isDeleted() && (condition == null || condition.matches(record, table))) {
                    // 更新字段值
                    for (int j = 0; j < columnNames.size(); j++) {
                        String columnName = columnNames.get(j);
                        Field field = table.getFieldByName(columnName);
                        if (field == null) {
                            throw new DBMSException("Column " + columnName + " does not exist");
                        }
                        
                        int fieldIndex = table.getFields().indexOf(field);
                        Object value = values.get(j);
                        
                        // 转换值类型
                        if (value instanceof String) {
                            value = TypeConverter.convertValue((String) value, field.getType());
                        }
                        
                        record.setValue(fieldIndex, value);
                    }
                    
                    // 验证记录
                    Validator.validateRecord(record, table);
                    
                    // 写回文件
                    long position = positions.get(i);
                    DATFileManager.writeRecordAt(tableDataFile, position, record, table);
                    updatedCount++;
                }
            }
            
            return updatedCount;
        } catch (IOException e) {
            throw new DBMSException("Failed to update records: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取所有记录位置（用于更新和删除）
     */
    private List<Long> getRecordPositions(String filePath, Table table) throws IOException {
        List<Long> positions = new ArrayList<>();
        java.io.RandomAccessFile raf = new java.io.RandomAccessFile(filePath, "r");
        
        int recordSize = DATFileManager.getRecordSize(table);
        long fileLength = raf.length();
        
        while (raf.getFilePointer() < fileLength) {
            positions.add(raf.getFilePointer());
            raf.skipBytes(recordSize);
        }
        
        raf.close();
        return positions;
    }
    
    /**
     * 查询条件接口
     */
    public static class QueryCondition {
        public String columnName;
        public String operator; // =, <, >, <=, >=, !=, LIKE
        public Object value;
        
        public QueryCondition(String columnName, String operator, Object value) {
            this.columnName = columnName;
            this.operator = operator;
            this.value = value;
        }
        
        public String getColumnName() {
            return columnName;
        }
        
        public String getOperator() {
            return operator;
        }
        
        public Object getValue() {
            return value;
        }
        
        public boolean matches(Record record, Table table) {
            Field field = table.getFieldByName(columnName);
            if (field == null) {
                return false;
            }
            
            Object recordValue = record.getValue(table, columnName);
            
            if (recordValue == null || value == null) {
                return operator.equals("=") && recordValue == value;
            }
            
            switch (operator) {
                case "=":
                    return recordValue.equals(value);
                case "!=":
                case "<>":
                    return !recordValue.equals(value);
                case "<":
                    return compare(recordValue, value) < 0;
                case ">":
                    return compare(recordValue, value) > 0;
                case "<=":
                    return compare(recordValue, value) <= 0;
                case ">=":
                    return compare(recordValue, value) >= 0;
                case "LIKE":
                    if (recordValue instanceof String && value instanceof String) {
                        String pattern = ((String) value).replace("%", ".*").replace("_", ".");
                        return ((String) recordValue).matches(pattern);
                    }
                    return false;
                default:
                    return false;
            }
        }
        
        @SuppressWarnings("unchecked")
        private int compare(Object a, Object b) {
            if (a instanceof Comparable && b instanceof Comparable) {
                return ((Comparable<Object>) a).compareTo(b);
            }
            return a.toString().compareTo(b.toString());
        }
    }
}

