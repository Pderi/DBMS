package com.dbms.engine;

import com.dbms.model.Field;
import com.dbms.parser.SQLParser;
import com.dbms.parser.SQLParser.*;
import com.dbms.util.SQLException;
import com.dbms.engine.QueryExecutor.QueryResult;

/**
 * SQL执行器 - 连接SQL解析器和执行引擎
 */
public class SQLExecutor {
    
    private DDLExecutor ddlExecutor;
    private DMLExecutor dmlExecutor;
    private QueryExecutor queryExecutor;
    private SQLParser parser;
    
    public SQLExecutor(DDLExecutor ddlExecutor, DMLExecutor dmlExecutor, QueryExecutor queryExecutor) {
        this.ddlExecutor = ddlExecutor;
        this.dmlExecutor = dmlExecutor;
        this.queryExecutor = queryExecutor;
        this.parser = new SQLParser();
    }
    
    /**
     * 执行SQL语句
     */
    public Object execute(String sql) {
        try {
            SQLStatement stmt = parser.parse(sql);
            
            switch (stmt.type) {
                case CREATE_TABLE:
                    return executeCreateTable((CreateTableStatement) stmt);
                case ALTER_TABLE:
                    return executeAlterTable((AlterTableStatement) stmt);
                case DROP_TABLE:
                    return executeDropTable((DropTableStatement) stmt);
                case RENAME_TABLE:
                    return executeRenameTable((RenameTableStatement) stmt);
                case INSERT:
                    return executeInsert((InsertStatement) stmt);
                case UPDATE:
                    return executeUpdate((UpdateStatement) stmt);
                case DELETE:
                    return executeDelete((DeleteStatement) stmt);
                case SELECT:
                    return executeSelect((SelectStatement) stmt);
                default:
                    throw new SQLException("Unsupported statement type: " + stmt.type);
            }
        } catch (Exception e) {
            throw new SQLException("SQL execution error: " + e.getMessage(), e);
        }
    }
    
    private String executeCreateTable(CreateTableStatement stmt) {
        java.util.List<Field> fields = new java.util.ArrayList<>();
        for (FieldDefinition fieldDef : stmt.fields) {
            fields.add(fieldDef.toField());
        }
        ddlExecutor.createTable(stmt.tableName, fields);
        return "Table '" + stmt.tableName + "' created successfully";
    }
    
    private String executeAlterTable(AlterTableStatement stmt) {
        switch (stmt.alterType) {
            case "ADD":
                ddlExecutor.addColumn(stmt.tableName, stmt.fieldDef.toField());
                return "Column added successfully";
            case "DROP":
                ddlExecutor.dropColumn(stmt.tableName, stmt.columnName);
                return "Column dropped successfully";
            case "MODIFY":
                ddlExecutor.modifyColumn(stmt.tableName, stmt.columnName, 
                    stmt.fieldDef.type, stmt.fieldDef.length);
                return "Column modified successfully";
            case "RENAME":
                ddlExecutor.renameColumn(stmt.tableName, stmt.columnName, stmt.newColumnName);
                return "Column renamed successfully";
            default:
                throw new SQLException("Unknown ALTER type: " + stmt.alterType);
        }
    }
    
    private String executeDropTable(DropTableStatement stmt) {
        ddlExecutor.dropTable(stmt.tableName);
        return "Table '" + stmt.tableName + "' dropped successfully";
    }
    
    private String executeRenameTable(RenameTableStatement stmt) {
        ddlExecutor.renameTable(stmt.oldName, stmt.newName);
        return "Table renamed from '" + stmt.oldName + "' to '" + stmt.newName + "'";
    }
    
    private String executeInsert(InsertStatement stmt) {
        if (stmt.columnNames.isEmpty()) {
            dmlExecutor.insert(stmt.tableName, stmt.values);
        } else {
            dmlExecutor.insert(stmt.tableName, stmt.columnNames, stmt.values);
        }
        return "1 row inserted";
    }
    
    private String executeUpdate(UpdateStatement stmt) {
        int count = dmlExecutor.update(stmt.tableName, stmt.columnNames, stmt.values, stmt.whereCondition);
        return count + " row(s) updated";
    }
    
    private String executeDelete(DeleteStatement stmt) {
        int count = dmlExecutor.delete(stmt.tableName, stmt.whereCondition);
        return count + " row(s) deleted";
    }
    
    private QueryResult executeSelect(SelectStatement stmt) {
        // 处理列名：将 alias.column 转换为 column（保留别名信息用于显示）
        java.util.List<String> processedColumnNames = new java.util.ArrayList<>();
        for (String colName : stmt.columnNames) {
            if (colName.contains(".")) {
                // 保留 alias.column 格式用于显示，但执行时需要知道真实表名
                processedColumnNames.add(colName);
            } else {
                processedColumnNames.add(colName);
            }
        }
        
        if (stmt.tableNames.size() == 1) {
            // 单表查询
            return queryExecutor.select(stmt.tableNames.get(0), processedColumnNames, stmt.whereCondition);
        } else if (stmt.tableNames.size() >= 2) {
            // 多表连接（支持2个或更多表）
            return queryExecutor.join(stmt.tableNames, processedColumnNames, 
                stmt.joinConditions, stmt.whereCondition);
        } else {
            throw new SQLException("No tables specified in SELECT statement");
        }
    }
}

