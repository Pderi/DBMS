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
     * 计算表达式值（如 column + 1, column - 1 等）
     */
    private Object evaluateExpression(Object columnValue, String operator, Object exprValue, 
                                      com.dbms.model.FieldType fieldType) {
        // 转换列值为数字类型
        Number colNum = convertToNumber(columnValue);
        if (colNum == null) {
            throw new DBMSException("Expression operands must be numeric, got: " + 
                (columnValue != null ? columnValue.getClass().getSimpleName() : "null"));
        }
        
        // 转换表达式值为数字类型
        Number exprNum = convertToNumber(exprValue);
        if (exprNum == null) {
            throw new DBMSException("Expression operands must be numeric, got: " + 
                (exprValue != null ? exprValue.getClass().getSimpleName() : "null"));
        }
        
        double colDouble = colNum.doubleValue();
        double exprDouble = exprNum.doubleValue();
        double result;
        
        switch (operator) {
            case "+":
                result = colDouble + exprDouble;
                break;
            case "-":
                result = colDouble - exprDouble;
                break;
            case "*":
                result = colDouble * exprDouble;
                break;
            case "/":
                if (exprDouble == 0) {
                    throw new DBMSException("Division by zero");
                }
                result = colDouble / exprDouble;
                break;
            default:
                throw new DBMSException("Unsupported operator: " + operator);
        }
        
        // 根据字段类型返回适当的结果类型
        if (fieldType == com.dbms.model.FieldType.INT) {
            return (int) Math.round(result);
        } else if (fieldType == com.dbms.model.FieldType.FLOAT || 
                   fieldType == com.dbms.model.FieldType.DOUBLE) {
            return result;
        } else {
            // 其他类型不支持表达式
            throw new DBMSException("Expression not supported for field type: " + fieldType);
        }
    }
    
    /**
     * 将对象转换为数字类型
     */
    private Number convertToNumber(Object value) {
        if (value == null) {
            return null;
        }
        
        if (value instanceof Number) {
            return (Number) value;
        }
        
        if (value instanceof String) {
            try {
                String str = (String) value;
                if (str.contains(".")) {
                    return Double.parseDouble(str);
                } else {
                    return Integer.parseInt(str);
                }
            } catch (NumberFormatException e) {
                return null;
            }
        }
        
        return null;
    }
    
    /**
     * 将值转换为字段类型（如果已经是正确类型则直接返回）
     */
    private Object convertToFieldType(Object value, com.dbms.model.FieldType fieldType) {
        if (value == null) {
            return null;
        }
        
        // 如果已经是正确类型，直接返回
        if (TypeConverter.validateValue(value, fieldType, Integer.MAX_VALUE)) {
            return value;
        }
        
        // 尝试转换
        switch (fieldType) {
            case INT:
                if (value instanceof Number) {
                    return ((Number) value).intValue();
                }
                if (value instanceof String) {
                    return TypeConverter.convertValue((String) value, fieldType);
                }
                break;
            case FLOAT:
            case DOUBLE:
                if (value instanceof Number) {
                    return ((Number) value).doubleValue();
                }
                if (value instanceof String) {
                    return TypeConverter.convertValue((String) value, fieldType);
                }
                break;
            case VARCHAR:
            case CHAR:
            case DATE:
                return value.toString();
            default:
                return value;
        }
        
        return value;
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
            } else {
                // 如果值已经是数字类型，但字段类型不匹配，需要转换
                // 例如：字段是INT，但值是Double，需要转换为Integer
                value = convertToFieldType(value, field.getType());
            }
            
            record.setValue(i, value);
        }
        
        // 验证记录
        Validator.validateRecord(record, table);
        
        // 写入文件
        try {
            String tableDataFile = getTableDataFilePath(tableName);
            System.out.println("INSERT: 写入数据到文件: " + tableDataFile);
            DATFileManager.appendRecord(tableDataFile, record, table);
            System.out.println("INSERT: 数据写入成功");
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
            } else {
                // 如果值已经是数字类型，但字段类型不匹配，需要转换
                value = convertToFieldType(value, field.getType());
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
            
            // 确保 positions 和 records 大小一致
            if (records.size() != positions.size()) {
                // 如果大小不一致，重新获取位置（可能文件状态发生了变化）
                positions = getRecordPositions(tableDataFile, table);
                // 如果仍然不一致，记录警告
                if (records.size() != positions.size()) {
                    System.err.println("Warning: Records size (" + records.size() + 
                        ") != Positions size (" + positions.size() + ")");
                }
            }
            
            int deletedCount = 0;
            // positions 和 records 现在应该一一对应（都只包含未删除的记录）
            // readAllRecords() 已经过滤掉了已删除的记录，所以 records 中的记录都是未删除的
            for (int i = 0; i < records.size(); i++) {
                Record record = records.get(i);
                // 检查条件：如果没有WHERE条件，或者记录匹配条件，则删除
                if (condition == null || condition.matches(record, table)) {
                    // 检查位置索引是否有效（防御性编程）
                    if (i < positions.size()) {
                        long position = positions.get(i);
                        DATFileManager.deleteRecord(tableDataFile, position);
                        deletedCount++;
                    } else {
                        // 这种情况不应该发生，但如果发生了，记录警告
                        System.err.println("Warning: Position index " + i + " out of bounds for record " + i + 
                            ". Records size: " + records.size() + ", Positions size: " + positions.size());
                    }
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
            
            // 确保 positions 列表大小与 records 列表一致
            if (positions.size() != records.size()) {
                // 重新计算位置
                positions = getRecordPositions(tableDataFile, table);
            }
            
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
                        
                        // 处理表达式（如 age + 1）
                        if (value instanceof com.dbms.parser.SQLParser.UpdateExpression) {
                            com.dbms.parser.SQLParser.UpdateExpression expr = 
                                (com.dbms.parser.SQLParser.UpdateExpression) value;
                            if (expr.isExpression) {
                                // 计算表达式：从当前记录获取列值，然后应用操作符
                                Field exprField = table.getFieldByName(expr.columnName);
                                if (exprField == null) {
                                    throw new DBMSException("Column '" + expr.columnName + "' not found in expression");
                                }
                                
                                // 检查表达式列的类型是否支持表达式
                                com.dbms.model.FieldType exprFieldType = exprField.getType();
                                
                                // 如果表达式列不是数字类型，尝试从值本身判断（可能是字符串数字）
                                boolean isNumericType = (exprFieldType == com.dbms.model.FieldType.INT ||
                                                         exprFieldType == com.dbms.model.FieldType.FLOAT ||
                                                         exprFieldType == com.dbms.model.FieldType.DOUBLE);
                                
                                if (!isNumericType) {
                                    // 检查列值本身是否是数字（可能字段类型被修改了，但值仍然是数字）
                                    Object columnValue = record.getValue(table, expr.columnName);
                                    if (columnValue != null && convertToNumber(columnValue) != null) {
                                        // 值本身是数字，可以继续（字段类型可能被错误修改了）
                                        // 不抛出错误，继续执行
                                    } else {
                                        throw new DBMSException("Expression column '" + expr.columnName + 
                                            "' must be numeric type (INT, FLOAT, or DOUBLE), but got: " + exprFieldType);
                                    }
                                }
                                
                                Object columnValue = record.getValue(table, expr.columnName);
                                if (columnValue == null) {
                                    throw new DBMSException("Cannot evaluate expression: column '" + 
                                        expr.columnName + "' is NULL");
                                }
                                
                                // 检查列值是否是数字类型（用于表达式计算）
                                Number columnNumber = convertToNumber(columnValue);
                                if (columnNumber == null) {
                                    throw new DBMSException("Expression column '" + expr.columnName + 
                                        "' must contain a numeric value, but got: " + 
                                        (columnValue != null ? columnValue.getClass().getSimpleName() : "NULL"));
                                }
                                
                                // 检查表达式值是否是数字类型
                                Number exprNumber = convertToNumber(expr.value);
                                if (exprNumber == null) {
                                    throw new DBMSException("Expression value must be numeric, but got: " + 
                                        (expr.value != null ? expr.value.getClass().getSimpleName() : "NULL"));
                                }
                                
                                // 执行表达式计算（使用 DOUBLE 类型进行计算，保证精度）
                                double colDouble = columnNumber.doubleValue();
                                double exprDouble = exprNumber.doubleValue();
                                double resultDouble;
                                
                                switch (expr.operator) {
                                    case "+":
                                        resultDouble = colDouble + exprDouble;
                                        break;
                                    case "-":
                                        resultDouble = colDouble - exprDouble;
                                        break;
                                    case "*":
                                        resultDouble = colDouble * exprDouble;
                                        break;
                                    case "/":
                                        if (exprDouble == 0) {
                                            throw new DBMSException("Division by zero");
                                        }
                                        resultDouble = colDouble / exprDouble;
                                        break;
                                    default:
                                        throw new DBMSException("Unsupported operator: " + expr.operator);
                                }
                                
                                // 根据目标字段类型转换结果
                                com.dbms.model.FieldType targetType = field.getType();
                                if (targetType == com.dbms.model.FieldType.INT) {
                                    value = (int) Math.round(resultDouble);
                                } else if (targetType == com.dbms.model.FieldType.FLOAT || 
                                           targetType == com.dbms.model.FieldType.DOUBLE) {
                                    value = resultDouble;
                                } else if (targetType == com.dbms.model.FieldType.VARCHAR || 
                                           targetType == com.dbms.model.FieldType.CHAR) {
                                    // 如果目标字段是字符串类型，将数字结果转换为字符串
                                    // 如果是整数，不显示小数点；如果是小数，显示小数
                                    if (resultDouble == Math.floor(resultDouble)) {
                                        value = String.valueOf((int) resultDouble);
                                    } else {
                                        value = String.valueOf(resultDouble);
                                    }
                                } else {
                                    throw new DBMSException("Expression result cannot be assigned to field '" + 
                                        columnName + "' of type " + targetType);
                                }
                            } else {
                                value = expr.value;
                            }
                        }
                        
                        // 转换值类型
                        if (value instanceof String) {
                            value = TypeConverter.convertValue((String) value, field.getType());
                        } else {
                            // 如果值已经是数字类型，但字段类型不匹配，需要转换
                            value = convertToFieldType(value, field.getType());
                        }
                        
                        record.setValue(fieldIndex, value);
                    }
                    
                    // 验证记录
                    Validator.validateRecord(record, table);
                    
                    // 写回文件（确保索引有效）
                    if (i < positions.size()) {
                        long position = positions.get(i);
                        DATFileManager.writeRecordAt(tableDataFile, position, record, table);
                    } else {
                        // 如果位置列表不匹配，追加记录
                        DATFileManager.appendRecord(tableDataFile, record, table);
                    }
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
     * 只返回未删除记录的位置，与 readAllRecords 保持一致
     */
    private List<Long> getRecordPositions(String filePath, Table table) throws IOException {
        List<Long> positions = new ArrayList<>();
        
        // 如果文件不存在，返回空列表
        java.io.File file = new java.io.File(filePath);
        if (!file.exists() || file.length() == 0) {
            return positions;
        }
        
        // 使用与 readAllRecords 相同的方式读取记录，确保逻辑一致
        // 对于 VARCHAR 等变长字段，不能简单地使用固定大小跳过
        java.io.RandomAccessFile raf = new java.io.RandomAccessFile(filePath, "r");
        long fileLength = raf.length();
        
        while (raf.getFilePointer() < fileLength) {
            try {
                long position = raf.getFilePointer();
                // 使用 readRecord 方法读取记录（与 readAllRecords 保持一致）
                // 这样可以正确处理变长字段（VARCHAR）
                com.dbms.model.Record record = com.dbms.storage.DATFileManager.readRecord(raf, table);
                // 只记录未删除的记录位置
                if (!record.isDeleted()) {
                    positions.add(position);
                }
            } catch (java.io.EOFException e) {
                break;
            }
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
        
        /**
         * 比较两个值（支持数字类型转换）
         */
        private int compare(Object a, Object b) {
            // 处理数字类型比较：统一转换为Double进行比较
            if (a instanceof Number && b instanceof Number) {
                double aDouble = ((Number) a).doubleValue();
                double bDouble = ((Number) b).doubleValue();
                return Double.compare(aDouble, bDouble);
            }
            
            // 如果一个是数字，另一个是字符串，尝试将字符串转换为数字
            if (a instanceof Number && b instanceof String) {
                try {
                    double bDouble = Double.parseDouble((String) b);
                    double aDouble = ((Number) a).doubleValue();
                    return Double.compare(aDouble, bDouble);
                } catch (NumberFormatException e) {
                    // 如果字符串不能转换为数字，使用字符串比较
                    return a.toString().compareTo(b.toString());
                }
            }
            
            if (a instanceof String && b instanceof Number) {
                try {
                    double aDouble = Double.parseDouble((String) a);
                    double bDouble = ((Number) b).doubleValue();
                    return Double.compare(aDouble, bDouble);
                } catch (NumberFormatException e) {
                    // 如果字符串不能转换为数字，使用字符串比较
                    return a.toString().compareTo(b.toString());
                }
            }
            
            // 处理相同类型的Comparable对象
            if (a != null && b != null && a.getClass().equals(b.getClass()) && a instanceof Comparable) {
                @SuppressWarnings("unchecked")
                Comparable<Object> comparableA = (Comparable<Object>) a;
                return comparableA.compareTo(b);
            }
            
            // 其他情况：转换为字符串比较
            return a.toString().compareTo(b.toString());
        }
    }
}

