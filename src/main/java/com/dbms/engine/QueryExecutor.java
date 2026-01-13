package com.dbms.engine;

import com.dbms.model.Field;
import com.dbms.model.Record;
import com.dbms.model.Table;
import com.dbms.parser.SQLParser;
import com.dbms.storage.DATFileManager;
import com.dbms.util.DBMSException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 查询执行器 - 处理SELECT查询
 */
public class QueryExecutor {
    
    private DDLExecutor ddlExecutor;
    private String baseDatFilePath;  // 基础数据文件路径（用于生成表特定的路径）
    
    public QueryExecutor(DDLExecutor ddlExecutor, String baseDatFilePath) {
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
     * 单表查询
     */
    public QueryResult select(String tableName, List<String> columnNames, 
                             SQLParser.WhereCondition whereCondition,
                             List<String> groupByColumns) {
        Table table = ddlExecutor.getTable(tableName);
        if (table == null) {
            throw new DBMSException("Table " + tableName + " does not exist");
        }
        
        // 如果columnNames为空或包含"*"，选择所有字段
        List<String> selectedColumns = columnNames;
        if (selectedColumns == null || selectedColumns.isEmpty() || 
            selectedColumns.contains("*")) {
            selectedColumns = table.getFields().stream()
                .map(Field::getName)
                .collect(Collectors.toList());
        }
        
        // 检查是否有聚合函数
        boolean hasAggregate = false;
        for (String colName : selectedColumns) {
            if (colName.toUpperCase().startsWith("COUNT(") || 
                colName.toUpperCase().startsWith("SUM(") ||
                colName.toUpperCase().startsWith("AVG(") ||
                colName.toUpperCase().startsWith("MAX(") ||
                colName.toUpperCase().startsWith("MIN(")) {
                hasAggregate = true;
                break;
            }
        }
        
        // 验证字段名（跳过聚合函数）
        for (String colName : selectedColumns) {
            // 跳过聚合函数
            if (colName.toUpperCase().startsWith("COUNT(") || 
                colName.toUpperCase().startsWith("SUM(") ||
                colName.toUpperCase().startsWith("AVG(") ||
                colName.toUpperCase().startsWith("MAX(") ||
                colName.toUpperCase().startsWith("MIN(")) {
                continue;
            }
            // 支持 table.column 格式
            String actualColName = colName;
            if (colName.contains(".")) {
                String[] parts = colName.split("\\.", 2);
                actualColName = parts[1];
            }
            if (table.getFieldByName(actualColName) == null) {
                throw new DBMSException("Column " + colName + " does not exist");
            }
        }
        
        try {
            String tableDataFile = getTableDataFilePath(tableName);
            System.out.println("SELECT: 从文件读取数据: " + tableDataFile);
            List<Record> allRecords = DATFileManager.readAllRecords(tableDataFile, table);
            System.out.println("SELECT: 读取到 " + allRecords.size() + " 条记录");
            
            // 过滤记录
            List<Record> filteredRecords = new ArrayList<>();
            for (Record record : allRecords) {
                // 将Record转换为List<Object>以便使用matchesWhereCondition
                List<Object> row = new ArrayList<>();
                for (Field field : table.getFields()) {
                    row.add(record.getValue(table, field.getName()));
                }
                
                // 创建单表列表用于条件匹配
                List<Table> tables = new ArrayList<>();
                tables.add(table);
                
                if (whereCondition == null || matchesWhereCondition(row, tables, whereCondition)) {
                    filteredRecords.add(record);
                }
            }
            
            // 如果有聚合函数或GROUP BY，执行聚合查询
            if (hasAggregate || (groupByColumns != null && !groupByColumns.isEmpty())) {
                return executeAggregateQuery(table, filteredRecords, selectedColumns, groupByColumns);
            }
            
            // 投影（选择指定字段）
            List<List<Object>> resultData = new ArrayList<>();
            for (Record record : filteredRecords) {
                List<Object> row = new ArrayList<>();
                for (String colName : selectedColumns) {
                    // 支持 table.column 格式
                    String actualColName = colName;
                    if (colName.contains(".")) {
                        String[] parts = colName.split("\\.", 2);
                        actualColName = parts[1];
                    }
                    row.add(record.getValue(table, actualColName));
                }
                resultData.add(row);
            }
            
            return new QueryResult(selectedColumns, resultData);
            
        } catch (IOException e) {
            throw new DBMSException("Failed to execute query: " + e.getMessage(), e);
        }
    }
    
    /**
     * 多表连接查询（内连接）
     */
    public QueryResult join(List<String> tableNames, List<String> columnNames,
                           List<JoinCondition> joinConditions,
                           SQLParser.WhereCondition whereCondition,
                           List<String> groupByColumns,
                           java.util.Map<String, String> tableAliases) {
        if (tableNames.size() < 2) {
            throw new DBMSException("Join requires at least 2 tables");
        }
        
        // 读取所有表
        List<Table> tables = new ArrayList<>();
        List<List<Record>> tableRecords = new ArrayList<>();
        
        for (String tableName : tableNames) {
            Table table = ddlExecutor.getTable(tableName);
            if (table == null) {
                throw new DBMSException("Table " + tableName + " does not exist");
            }
            tables.add(table);
            
            try {
                String tableDataFile = getTableDataFilePath(tableName);
                List<Record> records = DATFileManager.readAllRecords(tableDataFile, table);
                tableRecords.add(records);
            } catch (IOException e) {
                throw new DBMSException("Failed to read table " + tableName + ": " + e.getMessage(), e);
            }
        }
        
        // 处理隐式JOIN：如果没有显式JOIN条件，从WHERE子句中提取连接条件
        List<JoinCondition> effectiveJoinConditions = new ArrayList<>(joinConditions != null ? joinConditions : new ArrayList<>());
        if (effectiveJoinConditions.isEmpty() && whereCondition != null) {
            // 从WHERE条件中提取等值连接条件（table1.column1 = table2.column2）
            extractJoinConditionsFromWhere(whereCondition, tableNames, effectiveJoinConditions);
        }
        
        // 执行连接
        List<List<Object>> fullResultData = performJoin(tables, tableRecords, effectiveJoinConditions, whereCondition);
        
        // 检查是否有聚合函数
        boolean hasAggregate = false;
        for (String colName : columnNames) {
            if (colName.toUpperCase().startsWith("COUNT(") || 
                colName.toUpperCase().startsWith("SUM(") ||
                colName.toUpperCase().startsWith("AVG(") ||
                colName.toUpperCase().startsWith("MAX(") ||
                colName.toUpperCase().startsWith("MIN(")) {
                hasAggregate = true;
                break;
            }
        }
        
        // 如果有聚合函数或GROUP BY，执行聚合查询
        if (hasAggregate || (groupByColumns != null && !groupByColumns.isEmpty())) {
            return executeAggregateQueryForJoin(tables, fullResultData, columnNames, groupByColumns, tableAliases);
        }
        
        // 处理列名（支持 alias.column 格式）
        List<String> selectedColumns = columnNames;
        if (selectedColumns == null || selectedColumns.isEmpty() || selectedColumns.contains("*")) {
            selectedColumns = new ArrayList<>();
            for (int i = 0; i < tables.size(); i++) {
                Table table = tables.get(i);
                for (Field field : table.getFields()) {
                    selectedColumns.add(table.getName() + "." + field.getName());
                }
            }
            // 如果使用*，返回所有列
            return new QueryResult(selectedColumns, fullResultData);
        }
        
        // 投影：根据选择的列名提取数据
        List<List<Object>> projectedData = new ArrayList<>();
        for (List<Object> fullRow : fullResultData) {
            List<Object> projectedRow = new ArrayList<>();
            for (String colName : selectedColumns) {
                Object value = extractColumnValue(colName, fullRow, tables);
                projectedRow.add(value);
            }
            projectedData.add(projectedRow);
        }
        
        return new QueryResult(selectedColumns, projectedData);
    }
    
    /**
     * 从WHERE条件中提取等值连接条件（用于隐式JOIN）
     * 识别形如 table1.column1 = table2.column2 的条件
     */
    private void extractJoinConditionsFromWhere(SQLParser.WhereCondition whereCondition, 
                                                 List<String> tableNames,
                                                 List<JoinCondition> joinConditions) {
        if (whereCondition == null) {
            return;
        }
        
        if (whereCondition.isLeaf) {
            // 单个条件：检查是否为等值连接条件（table1.column1 = table2.column2）
            DMLExecutor.QueryCondition cond = whereCondition.condition;
            if (cond != null && cond.operator.equals("=")) {
                // 检查列名和值是否都是 table.column 格式
                String leftCol = cond.columnName;
                Object rightValue = cond.value;
                
                // 如果值是字符串且包含点号，可能是列名（不是字符串字面量）
                if (rightValue instanceof String) {
                    String rightCol = (String) rightValue;
                    // 排除字符串字面量（用引号包围的）
                    if (rightCol.contains(".") && !rightCol.startsWith("'") && !rightCol.endsWith("'")) {
                        // 检查左右两边是否都是 table.column 格式
                        if (leftCol.contains(".")) {
                            String[] leftParts = leftCol.split("\\.", 2);
                            String[] rightParts = rightCol.split("\\.", 2);
                            if (leftParts.length == 2 && rightParts.length == 2) {
                                String leftTable = leftParts[0];
                                String leftColumn = leftParts[1];
                                String rightTable = rightParts[0];
                                String rightColumn = rightParts[1];
                                
                                // 检查两个表是否都在查询中（支持表名或别名）
                                boolean leftTableFound = tableNames.contains(leftTable);
                                boolean rightTableFound = tableNames.contains(rightTable);
                                
                                if (leftTableFound && rightTableFound && !leftTable.equals(rightTable)) {
                                    // 这是一个等值连接条件
                                    joinConditions.add(new JoinCondition(leftTable, leftColumn, rightTable, rightColumn));
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // 组合条件：递归处理左右子树
            extractJoinConditionsFromWhere(whereCondition.left, tableNames, joinConditions);
            extractJoinConditionsFromWhere(whereCondition.right, tableNames, joinConditions);
        }
    }
    
    /**
     * 执行连接操作
     */
    private List<List<Object>> performJoin(List<Table> tables, List<List<Record>> tableRecords,
                                           List<JoinCondition> joinConditions,
                                           SQLParser.WhereCondition whereCondition) {
        List<List<Object>> result = new ArrayList<>();
        
        // 简单的嵌套循环连接（可以优化为哈希连接等）
        if (tables.size() == 2) {
            // 两表连接
            Table table1 = tables.get(0);
            Table table2 = tables.get(1);
            List<Record> records1 = tableRecords.get(0);
            List<Record> records2 = tableRecords.get(1);
            
            for (Record r1 : records1) {
                for (Record r2 : records2) {
                    // 检查连接条件
                    boolean joinMatch = true;
                    if (joinConditions != null && !joinConditions.isEmpty()) {
                        for (JoinCondition jc : joinConditions) {
                            Object v1 = r1.getValue(table1, jc.leftColumn);
                            Object v2 = r2.getValue(table2, jc.rightColumn);
                            if (v1 == null || v2 == null || !v1.equals(v2)) {
                                joinMatch = false;
                                break;
                            }
                        }
                    }
                    
                    if (joinMatch) {
                        // 组合记录
                        List<Object> row = new ArrayList<>();
                        for (Field f : table1.getFields()) {
                            row.add(r1.getValue(table1, f.getName()));
                        }
                        for (Field f : table2.getFields()) {
                            row.add(r2.getValue(table2, f.getName()));
                        }
                        
                        // 检查WHERE条件
                        if (whereCondition == null || matchesWhereCondition(row, tables, whereCondition)) {
                            result.add(row);
                        }
                    }
                }
            }
        } else {
            // 多表连接（递归处理）
            // 先连接前两个表，再与后续表连接
            List<List<Object>> intermediateResult = new ArrayList<>();
            
            // 连接前两个表
            Table table1 = tables.get(0);
            Table table2 = tables.get(1);
            List<Record> records1 = tableRecords.get(0);
            List<Record> records2 = tableRecords.get(1);
            
            // 找到前两个表的连接条件
            List<JoinCondition> firstJoinConditions = new ArrayList<>();
            if (joinConditions != null) {
                for (JoinCondition jc : joinConditions) {
                    if ((jc.leftTable.equals(table1.getName()) || jc.rightTable.equals(table1.getName())) &&
                        (jc.leftTable.equals(table2.getName()) || jc.rightTable.equals(table2.getName()))) {
                        firstJoinConditions.add(jc);
                    }
                }
            }
            
            // 执行前两个表的连接
            for (Record r1 : records1) {
                for (Record r2 : records2) {
                    boolean joinMatch = true;
                    if (!firstJoinConditions.isEmpty()) {
                        for (JoinCondition jc : firstJoinConditions) {
                            Object v1, v2;
                            if (jc.leftTable.equals(table1.getName())) {
                                v1 = r1.getValue(table1, jc.leftColumn);
                                v2 = r2.getValue(table2, jc.rightColumn);
                            } else {
                                v1 = r1.getValue(table1, jc.rightColumn);
                                v2 = r2.getValue(table2, jc.leftColumn);
                            }
                            if (v1 == null || v2 == null || !v1.equals(v2)) {
                                joinMatch = false;
                                break;
                            }
                        }
                    }
                    
                    if (joinMatch) {
                        List<Object> row = new ArrayList<>();
                        for (Field f : table1.getFields()) {
                            row.add(r1.getValue(table1, f.getName()));
                        }
                        for (Field f : table2.getFields()) {
                            row.add(r2.getValue(table2, f.getName()));
                        }
                        intermediateResult.add(row);
                    }
                }
            }
            
            // 与后续表连接
            for (int i = 2; i < tables.size(); i++) {
                Table nextTable = tables.get(i);
                List<Record> nextRecords = tableRecords.get(i);
                List<List<Object>> newResult = new ArrayList<>();
                
                // 找到与当前表的连接条件
                List<JoinCondition> nextJoinConditions = new ArrayList<>();
                if (joinConditions != null) {
                    for (JoinCondition jc : joinConditions) {
                        if (jc.leftTable.equals(nextTable.getName()) || jc.rightTable.equals(nextTable.getName())) {
                            nextJoinConditions.add(jc);
                        }
                    }
                }
                
                // 执行连接
                for (List<Object> intermediateRow : intermediateResult) {
                    for (Record nextRecord : nextRecords) {
                        boolean joinMatch = true;
                        if (!nextJoinConditions.isEmpty()) {
                            for (JoinCondition jc : nextJoinConditions) {
                                // 找到连接条件中涉及的列
                                String joinColumn = jc.leftTable.equals(nextTable.getName()) ? 
                                    jc.leftColumn : jc.rightColumn;
                                Object nextValue = nextRecord.getValue(nextTable, joinColumn);
                                
                                // 在中间结果中找到对应的值
                                String otherTableName = jc.leftTable.equals(nextTable.getName()) ? 
                                    jc.rightTable : jc.leftTable;
                                String otherColumn = jc.leftTable.equals(nextTable.getName()) ? 
                                    jc.rightColumn : jc.leftColumn;
                                
                                // 在已连接的表列表中找到对应的表和列
                                int tableIndex = -1;
                                for (int j = 0; j < i; j++) {
                                    if (tables.get(j).getName().equals(otherTableName)) {
                                        tableIndex = j;
                                        break;
                                    }
                                }
                                
                                if (tableIndex >= 0) {
                                    Table otherTable = tables.get(tableIndex);
                                    int colOffset = 0;
                                    for (int j = 0; j < tableIndex; j++) {
                                        colOffset += tables.get(j).getFieldCount();
                                    }
                                    Field otherField = otherTable.getFieldByName(otherColumn);
                                    if (otherField != null) {
                                        int fieldIndex = otherTable.getFields().indexOf(otherField);
                                        int resultIndex = colOffset + fieldIndex;
                                        if (resultIndex < intermediateRow.size()) {
                                            Object otherValue = intermediateRow.get(resultIndex);
                                            if (nextValue == null || otherValue == null || !nextValue.equals(otherValue)) {
                                                joinMatch = false;
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                        if (joinMatch) {
                            List<Object> newRow = new ArrayList<>(intermediateRow);
                            for (Field f : nextTable.getFields()) {
                                newRow.add(nextRecord.getValue(nextTable, f.getName()));
                            }
                            newResult.add(newRow);
                        }
                    }
                }
                
                intermediateResult = newResult;
            }
            
            // 应用WHERE条件
            if (whereCondition != null) {
                List<List<Object>> filteredResult = new ArrayList<>();
                for (List<Object> row : intermediateResult) {
                    if (matchesWhereCondition(row, tables, whereCondition)) {
                        filteredResult.add(row);
                    }
                }
                result = filteredResult;
            } else {
                result = intermediateResult;
            }
        }
        
        return result;
    }
    
    /**
     * 从完整行中提取指定列的值（支持 alias.column 格式）
     */
    private Object extractColumnValue(String colName, List<Object> fullRow, List<Table> tables) {
        if (colName.contains(".")) {
            // 格式：alias.column 或 table.column
            String[] parts = colName.split("\\.", 2);
            String tableRef = parts[0];
            String columnName = parts[1];
            
            // 查找表
            Table targetTable = null;
            int colOffset = 0;
            for (Table table : tables) {
                if (table.getName().equals(tableRef) || table.getName().equals(tableRef)) {
                    targetTable = table;
                    break;
                }
                colOffset += table.getFieldCount();
            }
            
            if (targetTable == null) {
                // 可能是别名，尝试在所有表中查找
                for (Table table : tables) {
                    Field field = table.getFieldByName(columnName);
                    if (field != null) {
                        targetTable = table;
                        colOffset = 0;
                        for (int i = 0; i < tables.indexOf(table); i++) {
                            colOffset += tables.get(i).getFieldCount();
                        }
                        break;
                    }
                }
            }
            
            if (targetTable != null) {
                Field field = targetTable.getFieldByName(columnName);
                if (field != null) {
                    int fieldIndex = targetTable.getFields().indexOf(field);
                    int resultIndex = colOffset + fieldIndex;
                    if (resultIndex < fullRow.size()) {
                        return fullRow.get(resultIndex);
                    }
                }
            }
        } else {
            // 格式：column（无表前缀）
            // 在第一个找到该列名的表中查找
            int colOffset = 0;
            for (Table table : tables) {
                Field field = table.getFieldByName(colName);
                if (field != null) {
                    int fieldIndex = table.getFields().indexOf(field);
                    int resultIndex = colOffset + fieldIndex;
                    if (resultIndex < fullRow.size()) {
                        return fullRow.get(resultIndex);
                    }
                }
                colOffset += table.getFieldCount();
            }
        }
        return null;
    }
    
    /**
     * 检查WHERE条件（支持多个AND/OR条件）
     */
    private boolean matchesWhereCondition(List<Object> row, List<Table> tables, 
                                         SQLParser.WhereCondition whereCondition) {
        if (whereCondition == null) {
            return true;
        }
        
        if (whereCondition.isLeaf) {
            // 单个条件
            return matchesSingleCondition(row, tables, whereCondition.condition);
        } else {
            // 组合条件
            boolean leftResult = matchesWhereCondition(row, tables, whereCondition.left);
            boolean rightResult = matchesWhereCondition(row, tables, whereCondition.right);
            
            if (whereCondition.logicOp == SQLParser.WhereCondition.LogicOp.AND) {
                return leftResult && rightResult;
            } else {  // OR
                return leftResult || rightResult;
            }
        }
    }
    
    /**
     * 检查单个WHERE条件（支持 table.column 或 alias.column 格式）
     */
    private boolean matchesSingleCondition(List<Object> row, List<Table> tables, 
                                          DMLExecutor.QueryCondition condition) {
        if (condition == null) {
            return true;
        }
        
        String columnName = condition.columnName;
        Object conditionValue = condition.value;
        
        // 处理 table.column 或 alias.column 格式
        Object rowValue;
        if (columnName.contains(".")) {
            // 使用extractColumnValue来获取值
            rowValue = extractColumnValue(columnName, row, tables);
        } else {
            // 普通列名：查找字段所在的表
            int colIndex = 0;
            Table targetTable = null;
            
            for (Table table : tables) {
                if (table.getFieldByName(columnName) != null) {
                    targetTable = table;
                    break;
                }
                colIndex += table.getFieldCount();
            }
            
            if (targetTable == null) {
                return false;
            }
            
            // 找到列在结果中的索引
            int resultIndex = colIndex;
            for (Field f : targetTable.getFields()) {
                if (f.getName().equals(columnName)) {
                    break;
                }
                resultIndex++;
            }
            
            if (resultIndex >= row.size()) {
                return false;
            }
            
            rowValue = row.get(resultIndex);
        }
        
        // 处理条件值可能是另一个表的列的情况（table.column格式）
        Object actualConditionValue = conditionValue;
        if (conditionValue instanceof String) {
            String valueStr = (String) conditionValue;
            if (valueStr.contains(".") && !valueStr.startsWith("'") && !valueStr.endsWith("'")) {
                // 可能是另一个表的列，尝试提取值
                actualConditionValue = extractColumnValue(valueStr, row, tables);
            }
        }
        
        // 简单的条件匹配
        if (rowValue == null || actualConditionValue == null) {
            return condition.operator.equals("=") && rowValue == actualConditionValue;
        }
        
        switch (condition.operator) {
            case "=":
                // 对于数字类型，使用数值比较而不是equals
                if (rowValue instanceof Number && actualConditionValue instanceof Number) {
                    return compareValues(rowValue, actualConditionValue) == 0;
                }
                return rowValue.equals(actualConditionValue);
            case "!=":
            case "<>":
                // 对于数字类型，使用数值比较而不是equals
                if (rowValue instanceof Number && actualConditionValue instanceof Number) {
                    return compareValues(rowValue, actualConditionValue) != 0;
                }
                return !rowValue.equals(actualConditionValue);
            case "<":
                return compareValues(rowValue, actualConditionValue) < 0;
            case ">":
                return compareValues(rowValue, actualConditionValue) > 0;
            case "<=":
                return compareValues(rowValue, actualConditionValue) <= 0;
            case ">=":
                return compareValues(rowValue, actualConditionValue) >= 0;
            case "LIKE":
                if (rowValue instanceof String && actualConditionValue instanceof String) {
                    String pattern = ((String) actualConditionValue).replace("%", ".*").replace("_", ".");
                    return ((String) rowValue).matches(pattern);
                }
                return false;
            default:
                return false;
        }
    }
    
    /**
     * 比较两个值（支持数字类型转换）
     */
    private int compareValues(Object a, Object b) {
        // 处理数字类型比较：统一转换为Double进行比较
        if (a instanceof Number && b instanceof Number) {
            double aDouble = ((Number) a).doubleValue();
            double bDouble = ((Number) b).doubleValue();
            return Double.compare(aDouble, bDouble);
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
    
    /**
     * 执行聚合查询（单表）
     */
    private QueryResult executeAggregateQuery(Table table, List<Record> records, 
                                            List<String> columnNames, List<String> groupByColumns) {
        // 如果没有GROUP BY，整个结果集作为一个分组
        java.util.Map<String, List<Record>> groups;
        if (groupByColumns == null || groupByColumns.isEmpty()) {
            groups = new java.util.HashMap<>();
            groups.put("", records);  // 使用空字符串作为键
        } else {
            // 按GROUP BY列分组
            groups = new java.util.HashMap<>();
            for (Record record : records) {
                StringBuilder keyBuilder = new StringBuilder();
                for (String groupCol : groupByColumns) {
                    String actualColName = groupCol;
                    if (groupCol.contains(".")) {
                        String[] parts = groupCol.split("\\.", 2);
                        actualColName = parts[1];
                    }
                    Object value = record.getValue(table, actualColName);
                    keyBuilder.append(value != null ? value.toString() : "NULL").append("|");
                }
                String key = keyBuilder.toString();
                groups.computeIfAbsent(key, k -> new ArrayList<>()).add(record);
            }
        }
        
        // 对每个分组计算聚合函数
        List<List<Object>> resultData = new ArrayList<>();
        for (java.util.Map.Entry<String, List<Record>> entry : groups.entrySet()) {
            List<Record> groupRecords = entry.getValue();
            List<Object> row = new ArrayList<>();
            
            for (String colName : columnNames) {
                String upperColName = colName.toUpperCase();
                if (upperColName.startsWith("COUNT(")) {
                    // COUNT(*)
                    row.add((long) groupRecords.size());
                } else if (upperColName.startsWith("SUM(")) {
                    // SUM(column)
                    String param = extractFunctionParam(colName);
                    String actualColName = param.contains(".") ? param.split("\\.", 2)[1] : param;
                    double sum = 0.0;
                    for (Record rec : groupRecords) {
                        Object value = rec.getValue(table, actualColName);
                        if (value instanceof Number) {
                            sum += ((Number) value).doubleValue();
                        }
                    }
                    row.add(sum);
                } else if (upperColName.startsWith("AVG(")) {
                    // AVG(column)
                    String param = extractFunctionParam(colName);
                    String actualColName = param.contains(".") ? param.split("\\.", 2)[1] : param;
                    double sum = 0.0;
                    int count = 0;
                    for (Record rec : groupRecords) {
                        Object value = rec.getValue(table, actualColName);
                        if (value instanceof Number) {
                            sum += ((Number) value).doubleValue();
                            count++;
                        }
                    }
                    row.add(count > 0 ? sum / count : 0.0);
                } else if (upperColName.startsWith("MAX(")) {
                    // MAX(column)
                    String param = extractFunctionParam(colName);
                    String actualColName = param.contains(".") ? param.split("\\.", 2)[1] : param;
                    Object max = null;
                    for (Record rec : groupRecords) {
                        Object value = rec.getValue(table, actualColName);
                        if (value != null && (max == null || compareValues(value, max) > 0)) {
                            max = value;
                        }
                    }
                    row.add(max);
                } else if (upperColName.startsWith("MIN(")) {
                    // MIN(column)
                    String param = extractFunctionParam(colName);
                    String actualColName = param.contains(".") ? param.split("\\.", 2)[1] : param;
                    Object min = null;
                    for (Record rec : groupRecords) {
                        Object value = rec.getValue(table, actualColName);
                        if (value != null && (min == null || compareValues(value, min) < 0)) {
                            min = value;
                        }
                    }
                    row.add(min);
                } else {
                    // 普通列（GROUP BY列）
                    String actualColName = colName;
                    if (colName.contains(".")) {
                        String[] parts = colName.split("\\.", 2);
                        actualColName = parts[1];
                    }
                    // 对于GROUP BY列，取第一行的值（同一分组中值应该相同）
                    if (!groupRecords.isEmpty()) {
                        row.add(groupRecords.get(0).getValue(table, actualColName));
                    } else {
                        row.add(null);
                    }
                }
            }
            resultData.add(row);
        }
        
        return new QueryResult(columnNames, resultData);
    }
    
    /**
     * 从聚合函数中提取参数（如 COUNT(*) -> *, SUM(age) -> age）
     */
    private String extractFunctionParam(String functionCall) {
        int openParen = functionCall.indexOf('(');
        int closeParen = functionCall.lastIndexOf(')');
        if (openParen >= 0 && closeParen > openParen) {
            return functionCall.substring(openParen + 1, closeParen);
        }
        return "";
    }
    
    /**
     * 执行聚合查询（多表JOIN）
     */
    private QueryResult executeAggregateQueryForJoin(List<Table> tables, List<List<Object>> joinedData,
                                                     List<String> columnNames, List<String> groupByColumns,
                                                     java.util.Map<String, String> tableAliases) {
        // 如果没有GROUP BY，整个结果集作为一个分组
        java.util.Map<String, List<List<Object>>> groups;
        if (groupByColumns == null || groupByColumns.isEmpty()) {
            groups = new java.util.HashMap<>();
            groups.put("", joinedData);
        } else {
            // 按GROUP BY列分组
            groups = new java.util.HashMap<>();
            for (List<Object> row : joinedData) {
                StringBuilder keyBuilder = new StringBuilder();
                for (String groupCol : groupByColumns) {
                    // 找到GROUP BY列在所有表中的位置
                    Object value = getColumnValueFromJoinedRow(tables, row, groupCol, tableAliases);
                    keyBuilder.append(value != null ? value.toString() : "NULL").append("|");
                }
                String key = keyBuilder.toString();
                groups.computeIfAbsent(key, k -> new ArrayList<>()).add(row);
            }
        }
        
        // 对每个分组计算聚合函数
        List<List<Object>> resultData = new ArrayList<>();
        for (java.util.Map.Entry<String, List<List<Object>>> entry : groups.entrySet()) {
            List<List<Object>> groupRows = entry.getValue();
            List<Object> row = new ArrayList<>();
            
            for (String colName : columnNames) {
                String upperColName = colName.toUpperCase();
                if (upperColName.startsWith("COUNT(")) {
                    row.add((long) groupRows.size());
                } else if (upperColName.startsWith("SUM(")) {
                    String param = extractFunctionParam(colName);
                    double sum = 0.0;
                    for (List<Object> joinedRow : groupRows) {
                        Object value = getColumnValueFromJoinedRow(tables, joinedRow, param, tableAliases);
                        if (value instanceof Number) {
                            sum += ((Number) value).doubleValue();
                        }
                    }
                    row.add(sum);
                } else if (upperColName.startsWith("AVG(")) {
                    String param = extractFunctionParam(colName);
                    double sum = 0.0;
                    int count = 0;
                    for (List<Object> joinedRow : groupRows) {
                        Object value = getColumnValueFromJoinedRow(tables, joinedRow, param, tableAliases);
                        if (value instanceof Number) {
                            sum += ((Number) value).doubleValue();
                            count++;
                        }
                    }
                    row.add(count > 0 ? sum / count : 0.0);
                } else if (upperColName.startsWith("MAX(")) {
                    String param = extractFunctionParam(colName);
                    Object max = null;
                    for (List<Object> joinedRow : groupRows) {
                        Object value = getColumnValueFromJoinedRow(tables, joinedRow, param, tableAliases);
                        if (value != null && (max == null || compareValues(value, max) > 0)) {
                            max = value;
                        }
                    }
                    row.add(max);
                } else if (upperColName.startsWith("MIN(")) {
                    String param = extractFunctionParam(colName);
                    Object min = null;
                    for (List<Object> joinedRow : groupRows) {
                        Object value = getColumnValueFromJoinedRow(tables, joinedRow, param, tableAliases);
                        if (value != null && (min == null || compareValues(value, min) < 0)) {
                            min = value;
                        }
                    }
                    row.add(min);
                } else {
                    // 普通列（GROUP BY列）
                    Object value = getColumnValueFromJoinedRow(tables, groupRows.get(0), colName, tableAliases);
                    row.add(value);
                }
            }
            resultData.add(row);
        }
        
        return new QueryResult(columnNames, resultData);
    }
    
    /**
     * 从JOIN结果行中获取指定列的值（支持 alias.column 格式）
     */
    private Object getColumnValueFromJoinedRow(List<Table> tables, List<Object> row, String columnName,
                                               java.util.Map<String, String> tableAliases) {
        // 支持 alias.column 格式
        String tableRef = null;
        String actualColName = columnName;
        if (columnName.contains(".")) {
            String[] parts = columnName.split("\\.", 2);
            tableRef = parts[0];
            actualColName = parts[1];
        }
        
        int colOffset = 0;
        for (int i = 0; i < tables.size(); i++) {
            Table table = tables.get(i);
            // 检查表名或别名是否匹配
            boolean matches = false;
            if (tableRef == null) {
                // 没有表前缀，在所有表中查找
                matches = true;
            } else {
                // 有表前缀，检查是否匹配表名或别名
                if (tableRef.equals(table.getName())) {
                    matches = true;
                } else if (tableAliases != null) {
                    // 检查是否是别名，如果是，获取实际表名
                    String actualTableName = tableAliases.get(tableRef);
                    if (actualTableName != null && actualTableName.equals(table.getName())) {
                        matches = true;
                    }
                }
            }
            
            if (matches) {
                Field field = table.getFieldByName(actualColName);
                if (field != null) {
                    // 找到字段在表中的索引
                    int fieldIndex = -1;
                    for (int j = 0; j < table.getFields().size(); j++) {
                        if (table.getFields().get(j).getName().equalsIgnoreCase(actualColName)) {
                            fieldIndex = j;
                            break;
                        }
                    }
                    if (fieldIndex >= 0) {
                        return row.get(colOffset + fieldIndex);
                    }
                }
            }
            colOffset += table.getFieldCount();
        }
        return null;
    }
    
    /**
     * 连接条件
     */
    public static class JoinCondition {
        String leftTable;
        String leftColumn;
        String rightTable;
        String rightColumn;
        
        public JoinCondition(String leftTable, String leftColumn, 
                           String rightTable, String rightColumn) {
            this.leftTable = leftTable;
            this.leftColumn = leftColumn;
            this.rightTable = rightTable;
            this.rightColumn = rightColumn;
        }
    }
    
    /**
     * 查询结果
     */
    public static class QueryResult {
        private List<String> columnNames;
        private List<List<Object>> data;
        
        public QueryResult(List<String> columnNames, List<List<Object>> data) {
            this.columnNames = columnNames;
            this.data = data;
        }
        
        public List<String> getColumnNames() {
            return columnNames;
        }
        
        public List<List<Object>> getData() {
            return data;
        }
        
        public int getRowCount() {
            return data.size();
        }
        
        public int getColumnCount() {
            return columnNames.size();
        }
    }
}

