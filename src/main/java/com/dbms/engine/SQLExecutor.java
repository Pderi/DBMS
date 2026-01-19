package com.dbms.engine;

import com.dbms.model.Field;
import com.dbms.model.User;
import com.dbms.parser.SQLParser;
import com.dbms.parser.SQLParser.*;
import com.dbms.util.SQLException;
import com.dbms.util.TransactionManager;
import com.dbms.util.UserManager;
import com.dbms.engine.QueryExecutor.QueryResult;

/**
 * SQL执行器 - 连接SQL解析器和执行引擎
 */
public class SQLExecutor {
    
    private DDLExecutor ddlExecutor;
    private DMLExecutor dmlExecutor;
    private QueryExecutor queryExecutor;
    private SQLParser parser;
    private UserManager userManager;
    private TransactionManager transactionManager;
    
    public SQLExecutor(DDLExecutor ddlExecutor, DMLExecutor dmlExecutor, QueryExecutor queryExecutor) {
        this.ddlExecutor = ddlExecutor;
        this.dmlExecutor = dmlExecutor;
        this.queryExecutor = queryExecutor;
        this.parser = new SQLParser();
        this.userManager = new UserManager();
        this.transactionManager = new TransactionManager();
    }
    
    public UserManager getUserManager() {
        return userManager;
    }
    
    public TransactionManager getTransactionManager() {
        return transactionManager;
    }
    
    /**
     * 检查当前用户是否有指定权限
     * @param permission 需要的权限
     * @param allowNoLogin 如果为true，未登录时允许执行（用于某些特殊操作）
     * @throws SQLException 如果没有权限
     */
    private void checkPermission(String permission, boolean allowNoLogin) {
        User currentUser = userManager.getCurrentUser();
        if (currentUser == null) {
            if (allowNoLogin) {
                return; // 允许未登录用户执行
            } else {
                throw new SQLException("Permission denied: User not logged in. Please login first.");
            }
        }
        
        if (!userManager.hasPermission(permission) && !userManager.hasPermission("ALL")) {
            throw new SQLException("Permission denied: " + permission + " privilege required. Current user: " + currentUser.getUsername());
        }
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
                case CREATE_INDEX:
                    return executeCreateIndex((CreateIndexStatement) stmt);
                case CREATE_USER:
                    return executeCreateUser((CreateUserStatement) stmt);
                case ALTER_TABLE:
                    return executeAlterTable((AlterTableStatement) stmt);
                case DROP_TABLE:
                    return executeDropTable((DropTableStatement) stmt);
                case DROP_USER:
                    return executeDropUser((DropUserStatement) stmt);
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
                case GRANT:
                    return executeGrant((GrantStatement) stmt);
                case REVOKE:
                    return executeRevoke((RevokeStatement) stmt);
                case BEGIN:
                    return executeBegin((BeginStatement) stmt);
                case COMMIT:
                    return executeCommit((CommitStatement) stmt);
                case ROLLBACK:
                    return executeRollback((RollbackStatement) stmt);
                default:
                    throw new SQLException("Unsupported statement type: " + stmt.type);
            }
        } catch (Exception e) {
            throw new SQLException("SQL execution error: " + e.getMessage(), e);
        }
    }
    
    private String executeCreateTable(CreateTableStatement stmt) {
        checkPermission("CREATE_TABLE", false);
        java.util.List<Field> fields = new java.util.ArrayList<>();
        for (FieldDefinition fieldDef : stmt.fields) {
            fields.add(fieldDef.toField());
        }
        ddlExecutor.createTable(stmt.tableName, fields);
        return "Table '" + stmt.tableName + "' created successfully";
    }
    
    private String executeCreateIndex(CreateIndexStatement stmt) {
        checkPermission("CREATE_INDEX", false);
        ddlExecutor.createIndex(stmt.indexName, stmt.tableName, stmt.columnName, stmt.unique);
        return "Index '" + stmt.indexName + "' created successfully on " + stmt.tableName + "(" + stmt.columnName + ")";
    }
    
    private String executeCreateUser(CreateUserStatement stmt) {
        checkPermission("CREATE_USER", true); // 允许未登录用户创建第一个用户
        userManager.createUser(stmt.username, stmt.password);
        return "User '" + stmt.username + "' created successfully";
    }
    
    private String executeAlterTable(AlterTableStatement stmt) {
        checkPermission("ALTER_TABLE", false);
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
        checkPermission("DROP_TABLE", false);
        ddlExecutor.dropTable(stmt.tableName);
        return "Table '" + stmt.tableName + "' dropped successfully";
    }
    
    private String executeDropUser(DropUserStatement stmt) {
        checkPermission("DROP_USER", false);
        userManager.deleteUser(stmt.username);
        return "User '" + stmt.username + "' dropped successfully";
    }
    
    private String executeRenameTable(RenameTableStatement stmt) {
        checkPermission("ALTER_TABLE", false);
        ddlExecutor.renameTable(stmt.oldName, stmt.newName);
        return "Table renamed from '" + stmt.oldName + "' to '" + stmt.newName + "'";
    }
    
    private String executeInsert(InsertStatement stmt) {
        checkPermission("INSERT", false);
        if (stmt.columnNames.isEmpty()) {
            dmlExecutor.insert(stmt.tableName, stmt.values);
        } else {
            dmlExecutor.insert(stmt.tableName, stmt.columnNames, stmt.values);
        }
        return "1 row inserted";
    }
    
    private String executeUpdate(UpdateStatement stmt) {
        checkPermission("UPDATE", false);
        // 将表达式列表转换为值列表（在执行时计算表达式）
        java.util.List<Object> values = new java.util.ArrayList<>();
        for (SQLParser.UpdateExpression expr : stmt.expressions) {
            if (expr.isExpression) {
                // 表达式将在DMLExecutor中计算，这里先传递表达式对象
                values.add(expr);
            } else {
                values.add(expr.value);
            }
        }
        int count = dmlExecutor.update(stmt.tableName, stmt.columnNames, values, stmt.whereCondition);
        return count + " row(s) updated";
    }
    
    private String executeDelete(DeleteStatement stmt) {
        checkPermission("DELETE", false);
        int count = dmlExecutor.delete(stmt.tableName, stmt.whereCondition);
        return count + " row(s) deleted";
    }
    
    private QueryResult executeSelect(SelectStatement stmt) {
        checkPermission("SELECT", false);
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
        
        // 使用列别名（如果有）或原始列名作为显示名称
        java.util.List<String> displayColumnNames = new java.util.ArrayList<>();
        for (int i = 0; i < processedColumnNames.size(); i++) {
            String displayName = (stmt.columnAliases != null && i < stmt.columnAliases.size() && 
                                 stmt.columnAliases.get(i) != null) 
                                 ? stmt.columnAliases.get(i) 
                                 : processedColumnNames.get(i);
            displayColumnNames.add(displayName);
        }
        
        QueryResult result;
        if (stmt.tableNames.size() == 1) {
            // 单表查询
            result = queryExecutor.select(stmt.tableNames.get(0), processedColumnNames, 
                                         stmt.whereCondition, stmt.groupByColumns, stmt.orderByColumns,
                                         stmt.subqueryColumns, stmt.tableAliases);
        } else if (stmt.tableNames.size() >= 2) {
            // 多表连接（支持2个或更多表）
            result = queryExecutor.join(stmt.tableNames, processedColumnNames, 
                stmt.joinConditions, stmt.whereCondition, stmt.groupByColumns, stmt.tableAliases, stmt.orderByColumns,
                stmt.subqueryColumns);
        } else {
            throw new SQLException("No tables specified in SELECT statement");
        }
        
        // 如果使用了列别名，更新结果中的列名
        if (stmt.columnAliases != null && !stmt.columnAliases.isEmpty()) {
            return new QueryResult(displayColumnNames, result.getData());
        }
        
        return result;
    }
    
    private String executeGrant(GrantStatement stmt) {
        checkPermission("GRANT", true); // 允许未登录用户执行（用于初始化）
        
        for (String permission : stmt.permissions) {
            userManager.grantPermission(stmt.username, permission);
        }
        return "Permissions granted to " + stmt.username + " successfully";
    }
    
    private String executeRevoke(RevokeStatement stmt) {
        checkPermission("REVOKE", true); // 允许未登录用户执行（用于初始化）
        
        for (String permission : stmt.permissions) {
            userManager.revokePermission(stmt.username, permission);
        }
        return "Permissions revoked from " + stmt.username + " successfully";
    }
    
    private String executeBegin(BeginStatement stmt) {
        transactionManager.beginTransaction();
        return "Transaction started";
    }
    
    private String executeCommit(CommitStatement stmt) {
        try {
            transactionManager.commit(transactionManager.getCurrentTransaction());
            return "Transaction committed successfully";
        } catch (Exception e) {
            throw new SQLException("Failed to commit transaction: " + e.getMessage(), e);
        }
    }
    
    private String executeRollback(RollbackStatement stmt) {
        try {
            transactionManager.rollback(transactionManager.getCurrentTransaction());
            return "Transaction rolled back successfully";
        } catch (Exception e) {
            throw new SQLException("Failed to rollback transaction: " + e.getMessage(), e);
        }
    }
}

