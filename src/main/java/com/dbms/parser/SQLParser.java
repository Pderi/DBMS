package com.dbms.parser;

import com.dbms.engine.DMLExecutor;
import com.dbms.engine.QueryExecutor;
import com.dbms.model.Field;
import com.dbms.model.FieldType;
import com.dbms.parser.SQLLexer.Token;
import com.dbms.parser.SQLLexer.TokenType;
import com.dbms.util.SQLException;

import java.util.ArrayList;
import java.util.List;

/**
 * SQL语法分析器
 */
public class SQLParser {
    
    private List<Token> tokens;
    private int currentPos;
    
    // SQL语句类型
    public enum StatementType {
        CREATE_TABLE, CREATE_INDEX, ALTER_TABLE, DROP_TABLE, RENAME_TABLE,
        CREATE_USER, DROP_USER, GRANT, REVOKE,
        BEGIN, COMMIT, ROLLBACK,
        INSERT, UPDATE, DELETE, SELECT, UNKNOWN
    }
    
    // SQL语句基类
    public static abstract class SQLStatement {
        public StatementType type;
    }
    
    // CREATE TABLE语句
    public static class CreateTableStatement extends SQLStatement {
        public String tableName;
        public List<FieldDefinition> fields;
        
        public CreateTableStatement() {
            this.type = StatementType.CREATE_TABLE;
            this.fields = new ArrayList<>();
        }
    }
    
    // ALTER TABLE语句
    public static class AlterTableStatement extends SQLStatement {
        public String tableName;
        public String alterType; // ADD, DROP, MODIFY, RENAME
        public String columnName;
        public String newColumnName;
        public FieldDefinition fieldDef;
        
        public AlterTableStatement() {
            this.type = StatementType.ALTER_TABLE;
        }
    }
    
    // DROP TABLE语句
    public static class DropTableStatement extends SQLStatement {
        public String tableName;
        
        public DropTableStatement() {
            this.type = StatementType.DROP_TABLE;
        }
    }
    
    // RENAME TABLE语句
    public static class RenameTableStatement extends SQLStatement {
        public String oldName;
        public String newName;
        
        public RenameTableStatement() {
            this.type = StatementType.RENAME_TABLE;
        }
    }
    
    // INSERT语句
    public static class InsertStatement extends SQLStatement {
        public String tableName;
        public List<String> columnNames;
        public List<Object> values;
        
        public InsertStatement() {
            this.type = StatementType.INSERT;
            this.columnNames = new ArrayList<>();
            this.values = new ArrayList<>();
        }
    }
    
    // UPDATE表达式（支持 column + number, column - number 等）
    public static class UpdateExpression {
        public String columnName;  // 列名（用于 column + number）
        public String operator;    // 操作符：+, -, *, /
        public Object value;       // 数值或null（如果是简单值，columnName为null）
        public boolean isExpression; // 是否为表达式
        
        public UpdateExpression(Object value) {
            this.value = value;
            this.isExpression = false;
        }
        
        public UpdateExpression(String columnName, String operator, Object value) {
            this.columnName = columnName;
            this.operator = operator;
            this.value = value;
            this.isExpression = true;
        }
    }
    
    // UPDATE语句
    public static class UpdateStatement extends SQLStatement {
        public String tableName;
        public List<String> columnNames;
        public List<UpdateExpression> expressions;  // 改为表达式列表
        public DMLExecutor.QueryCondition whereCondition;
        
        public UpdateStatement() {
            this.type = StatementType.UPDATE;
            this.columnNames = new ArrayList<>();
            this.expressions = new ArrayList<>();
        }
    }
    
    // DELETE语句
    public static class DeleteStatement extends SQLStatement {
        public String tableName;
        public DMLExecutor.QueryCondition whereCondition;
        
        public DeleteStatement() {
            this.type = StatementType.DELETE;
        }
    }
    
    // WHERE条件组合（支持AND/OR）
    public static class WhereCondition {
        public enum LogicOp {
            AND, OR
        }
        
        public DMLExecutor.QueryCondition condition;  // 单个条件
        public WhereCondition left;  // 左子树（用于组合条件）
        public WhereCondition right;  // 右子树（用于组合条件）
        public LogicOp logicOp;  // 逻辑运算符（AND/OR）
        public boolean isLeaf;  // 是否为叶子节点（单个条件）
        
        // 单个条件构造函数
        public WhereCondition(DMLExecutor.QueryCondition condition) {
            this.condition = condition;
            this.isLeaf = true;
        }
        
        // 组合条件构造函数
        public WhereCondition(WhereCondition left, LogicOp logicOp, WhereCondition right) {
            this.left = left;
            this.right = right;
            this.logicOp = logicOp;
            this.isLeaf = false;
        }
    }
    
    // ORDER BY项（列名和排序方向）
    public static class OrderByItem {
        public String columnName;
        public boolean ascending;  // true为ASC，false为DESC
        
        public OrderByItem(String columnName, boolean ascending) {
            this.columnName = columnName;
            this.ascending = ascending;
        }
    }
    
    // SELECT语句
    public static class SelectStatement extends SQLStatement {
        public List<String> columnNames;
        public List<String> columnAliases;  // 列别名列表，与columnNames一一对应
        public List<SelectStatement> subqueryColumns;  // 子查询列（标量子查询），与columnNames一一对应
        public List<String> tableNames;
        public java.util.Map<String, String> tableAliases; // 别名 -> 真实表名
        public List<QueryExecutor.JoinCondition> joinConditions;
        public WhereCondition whereCondition;  // 改为支持多个条件
        public List<String> groupByColumns;  // GROUP BY 列
        public List<OrderByItem> orderByColumns;  // ORDER BY 列
        
        public SelectStatement() {
            this.type = StatementType.SELECT;
            this.columnNames = new ArrayList<>();
            this.columnAliases = new ArrayList<>();
            this.subqueryColumns = new ArrayList<>();
            this.tableNames = new ArrayList<>();
            this.tableAliases = new java.util.HashMap<>();
            this.groupByColumns = new ArrayList<>();
            this.orderByColumns = new ArrayList<>();
        }
    }
    
    // 字段定义
    public static class FieldDefinition {
        public String name;
        public FieldType type;
        public int length;
        public boolean isKey;
        public boolean nullable;
        
        public Field toField() {
            return new Field(name, type, length, isKey, nullable);
        }
    }
    
    /**
     * 解析SQL语句
     */
    public SQLStatement parse(String sql) {
        SQLLexer lexer = new SQLLexer();
        this.tokens = lexer.tokenize(sql);
        this.currentPos = 0;
        
        if (tokens.isEmpty() || tokens.get(0).type == TokenType.EOF) {
            throw new SQLException("Empty SQL statement");
        }
        
        Token firstToken = tokens.get(0);
        if (firstToken.type != TokenType.KEYWORD) {
            throw new SQLException("SQL statement must start with a keyword");
        }
        
        String keyword = firstToken.value;
        currentPos++;
        
        switch (keyword) {
            case "CREATE":
                // 检查下一个关键字是TABLE、INDEX、USER还是UNIQUE INDEX
                if (peekKeyword("TABLE")) {
                    return parseCreateTable();
                } else if (peekKeyword("UNIQUE")) {
                    // CREATE UNIQUE INDEX
                    consume(); // 消费 UNIQUE
                    if (peekKeyword("INDEX")) {
                        return parseCreateIndex(true);
                    } else {
                        throw new SQLException("CREATE UNIQUE must be followed by INDEX");
                    }
                } else if (peekKeyword("INDEX")) {
                    return parseCreateIndex(false);
                } else if (peekKeyword("USER")) {
                    return parseCreateUser();
                } else {
                    throw new SQLException("CREATE must be followed by TABLE, INDEX or USER");
                }
            case "ALTER":
                return parseAlterTable();
            case "DROP":
                // 检查下一个关键字是TABLE还是USER
                if (peekKeyword("TABLE")) {
                    return parseDropTable();
                } else if (peekKeyword("USER")) {
                    return parseDropUser();
                } else {
                    throw new SQLException("DROP must be followed by TABLE or USER");
                }
            case "RENAME":
                return parseRenameTable();
            case "INSERT":
                return parseInsert();
            case "UPDATE":
                return parseUpdate();
            case "DELETE":
                return parseDelete();
            case "SELECT":
                return parseSelect();
            case "GRANT":
                return parseGrant();
            case "REVOKE":
                return parseRevoke();
            case "BEGIN":
                return parseBegin();
            case "COMMIT":
                return parseCommit();
            case "ROLLBACK":
                return parseRollback();
            default:
                throw new SQLException("Unknown SQL keyword: " + keyword);
        }
    }
    
    /**
     * 解析CREATE TABLE语句
     */
    private CreateTableStatement parseCreateTable() {
        expectKeyword("TABLE");
        CreateTableStatement stmt = new CreateTableStatement();
        stmt.tableName = expectIdentifier();
        expectPunctuation("(");
        
        while (!peekPunctuation(")")) {
            FieldDefinition fieldDef = parseFieldDefinition();
            stmt.fields.add(fieldDef);
            
            if (peekPunctuation(",")) {
                consume();
            } else {
                break;
            }
        }
        
        expectPunctuation(")");
        return stmt;
    }
    
    /**
     * 解析字段定义
     */
    private FieldDefinition parseFieldDefinition() {
        FieldDefinition fieldDef = new FieldDefinition();
        fieldDef.name = expectIdentifier();
        
        // 解析类型（类型可以是关键字，如INT、VARCHAR等）
        String typeName;
        Token typeToken = currentToken();
        if (typeToken.type == TokenType.KEYWORD) {
            // 类型关键字
            typeName = consume().value;
        } else if (typeToken.type == TokenType.IDENTIFIER) {
            // 类型标识符
            typeName = expectIdentifier();
        } else {
            throw new SQLException("Expected type name, got " + typeToken.type + " at " + typeToken);
        }
        fieldDef.type = FieldType.fromString(typeName);
        
        // 解析长度（如果有）
        if (peekPunctuation("(")) {
            consume();
            fieldDef.length = Integer.parseInt(expectToken(TokenType.NUMBER).value);
            expectPunctuation(")");
        } else {
            fieldDef.length = fieldDef.type.getDefaultLength();
            if (fieldDef.length < 0) {
                throw new SQLException("Length required for type " + typeName);
            }
        }
        
        // 解析约束
        fieldDef.isKey = false;
        fieldDef.nullable = true;
        
        while (peekKeyword("PRIMARY") || peekKeyword("KEY") || peekKeyword("NOT") || peekKeyword("NULL")) {
            if (peekKeyword("PRIMARY")) {
                consume();
                expectKeyword("KEY");
                fieldDef.isKey = true;
            } else if (peekKeyword("NOT")) {
                consume();
                expectKeyword("NULL");
                fieldDef.nullable = false;
            } else {
                break;
            }
        }
        
        return fieldDef;
    }
    
    /**
     * 解析字段类型（仅类型，用于ALTER TABLE MODIFY）
     */
    private FieldDefinition parseFieldTypeOnly() {
        FieldDefinition fieldDef = new FieldDefinition();
        fieldDef.name = ""; // MODIFY不需要字段名
        
        // 解析类型（类型可以是关键字，如INT、VARCHAR等）
        String typeName;
        Token typeToken = currentToken();
        if (typeToken.type == TokenType.KEYWORD) {
            // 类型关键字
            typeName = consume().value;
        } else if (typeToken.type == TokenType.IDENTIFIER) {
            // 类型标识符
            typeName = expectIdentifier();
        } else {
            throw new SQLException("Expected type name, got " + typeToken.type + " at " + typeToken);
        }
        fieldDef.type = FieldType.fromString(typeName);
        
        // 解析长度（如果有）
        if (peekPunctuation("(")) {
            consume();
            fieldDef.length = Integer.parseInt(expectToken(TokenType.NUMBER).value);
            expectPunctuation(")");
        } else {
            fieldDef.length = fieldDef.type.getDefaultLength();
            if (fieldDef.length < 0) {
                throw new SQLException("Length required for type " + typeName);
            }
        }
        
        // 解析约束
        fieldDef.isKey = false;
        fieldDef.nullable = true;
        
        while (peekKeyword("PRIMARY") || peekKeyword("KEY") || peekKeyword("NOT") || peekKeyword("NULL")) {
            if (peekKeyword("PRIMARY")) {
                consume();
                expectKeyword("KEY");
                fieldDef.isKey = true;
            } else if (peekKeyword("NOT")) {
                consume();
                expectKeyword("NULL");
                fieldDef.nullable = false;
            } else {
                break;
            }
        }
        
        return fieldDef;
    }
    
    /**
     * 解析ALTER TABLE语句
     */
    private AlterTableStatement parseAlterTable() {
        expectKeyword("TABLE");
        AlterTableStatement stmt = new AlterTableStatement();
        stmt.tableName = expectIdentifier();
        
        // ADD、DROP、MODIFY、RENAME都是关键字，需要检查并消费
        String alterType;
        if (peekKeyword("ADD")) {
            consume();
            alterType = "ADD";
        } else if (peekKeyword("DROP")) {
            consume();
            alterType = "DROP";
        } else if (peekKeyword("MODIFY")) {
            consume();
            alterType = "MODIFY";
        } else if (peekKeyword("RENAME")) {
            consume();
            alterType = "RENAME";
        } else {
            // 如果不是关键字，尝试作为标识符解析（向后兼容）
            alterType = expectIdentifier().toUpperCase();
        }
        
        stmt.alterType = alterType;
        
        switch (alterType) {
            case "ADD":
                if (peekKeyword("COLUMN")) consume();
                stmt.fieldDef = parseFieldDefinition();
                break;
            case "DROP":
                if (peekKeyword("COLUMN")) consume();
                stmt.columnName = expectIdentifier();
                break;
            case "MODIFY":
                if (peekKeyword("COLUMN")) consume();
                stmt.columnName = expectIdentifier();
                // MODIFY只需要类型定义，不需要字段名
                stmt.fieldDef = parseFieldTypeOnly();
                break;
            case "RENAME":
                if (peekKeyword("COLUMN")) consume();
                stmt.columnName = expectIdentifier();
                expectKeyword("TO");
                stmt.newColumnName = expectIdentifier();
                break;
            default:
                throw new SQLException("Unknown ALTER type: " + alterType);
        }
        
        return stmt;
    }
    
    /**
     * 解析CREATE INDEX语句
     */
    private CreateIndexStatement parseCreateIndex(boolean unique) {
        CreateIndexStatement stmt = new CreateIndexStatement();
        stmt.unique = unique;
        expectKeyword("INDEX");
        stmt.indexName = expectIdentifier();
        expectKeyword("ON");
        stmt.tableName = expectIdentifier();
        expectPunctuation("(");
        stmt.columnName = expectIdentifier();
        expectPunctuation(")");
        
        return stmt;
    }
    
    // CREATE INDEX语句
    public static class CreateIndexStatement extends SQLStatement {
        public String indexName;
        public String tableName;
        public String columnName;
        public boolean unique;
        
        public CreateIndexStatement() {
            this.type = StatementType.CREATE_INDEX;
            this.unique = false;
        }
    }
    
    /**
     * 解析CREATE USER语句
     */
    private CreateUserStatement parseCreateUser() {
        expectKeyword("USER");
        CreateUserStatement stmt = new CreateUserStatement();
        stmt.username = expectIdentifier();
        
        // 解析 IDENTIFIED BY 'password'（可选）
        if (peekKeyword("IDENTIFIED")) {
            consume();
            expectKeyword("BY");
            // 密码可以是字符串或标识符
            if (peekToken(TokenType.STRING)) {
                stmt.password = expectToken(TokenType.STRING).value;
            } else {
                stmt.password = expectIdentifier();
            }
        } else {
            // 如果没有指定密码，使用默认密码
            stmt.password = "password";
        }
        
        return stmt;
    }
    
    // CREATE USER语句
    public static class CreateUserStatement extends SQLStatement {
        public String username;
        public String password;
        
        public CreateUserStatement() {
            this.type = StatementType.CREATE_USER;
        }
    }
    
    /**
     * 解析DROP TABLE语句
     */
    private DropTableStatement parseDropTable() {
        expectKeyword("TABLE");
        DropTableStatement stmt = new DropTableStatement();
        stmt.tableName = expectIdentifier();
        return stmt;
    }
    
    /**
     * 解析DROP USER语句
     */
    private DropUserStatement parseDropUser() {
        expectKeyword("USER");
        DropUserStatement stmt = new DropUserStatement();
        stmt.username = expectIdentifier();
        return stmt;
    }
    
    // DROP USER语句
    public static class DropUserStatement extends SQLStatement {
        public String username;
        
        public DropUserStatement() {
            this.type = StatementType.DROP_USER;
        }
    }
    
    /**
     * 解析GRANT语句
     */
    private GrantStatement parseGrant() {
        GrantStatement stmt = new GrantStatement();
        
        // 解析权限列表（权限可以是关键字，如 SELECT, INSERT, UPDATE, DELETE）
        while (true) {
            String permission;
            if (peekToken(TokenType.KEYWORD)) {
                // 权限是关键字
                permission = consume().value.toUpperCase();
            } else {
                // 权限是标识符
                permission = expectIdentifier().toUpperCase();
            }
            stmt.permissions.add(permission);
            if (peekPunctuation(",")) {
                consume();
            } else {
                break;
            }
        }
        
        expectKeyword("TO");
        stmt.username = expectIdentifier();
        
        return stmt;
    }
    
    /**
     * 解析REVOKE语句
     */
    private RevokeStatement parseRevoke() {
        RevokeStatement stmt = new RevokeStatement();
        
        // 解析权限列表（权限可以是关键字，如 SELECT, INSERT, UPDATE, DELETE）
        while (true) {
            String permission;
            if (peekToken(TokenType.KEYWORD)) {
                // 权限是关键字
                permission = consume().value.toUpperCase();
            } else {
                // 权限是标识符
                permission = expectIdentifier().toUpperCase();
            }
            stmt.permissions.add(permission);
            if (peekPunctuation(",")) {
                consume();
            } else {
                break;
            }
        }
        
        expectKeyword("FROM");
        stmt.username = expectIdentifier();
        
        return stmt;
    }
    
    // GRANT语句
    public static class GrantStatement extends SQLStatement {
        public List<String> permissions;
        public String username;
        
        public GrantStatement() {
            this.type = StatementType.GRANT;
            this.permissions = new ArrayList<>();
        }
    }
    
    // REVOKE语句
    public static class RevokeStatement extends SQLStatement {
        public List<String> permissions;
        public String username;
        
        public RevokeStatement() {
            this.type = StatementType.REVOKE;
            this.permissions = new ArrayList<>();
        }
    }
    
    /**
     * 解析BEGIN语句
     */
    private BeginStatement parseBegin() {
        BeginStatement stmt = new BeginStatement();
        // BEGIN TRANSACTION 或 BEGIN（简化处理，只支持BEGIN）
        if (peekKeyword("TRANSACTION")) {
            consume();
        }
        return stmt;
    }
    
    /**
     * 解析COMMIT语句
     */
    private CommitStatement parseCommit() {
        CommitStatement stmt = new CommitStatement();
        // COMMIT TRANSACTION 或 COMMIT（简化处理）
        if (peekKeyword("TRANSACTION")) {
            consume();
        }
        return stmt;
    }
    
    /**
     * 解析ROLLBACK语句
     */
    private RollbackStatement parseRollback() {
        RollbackStatement stmt = new RollbackStatement();
        // ROLLBACK TRANSACTION 或 ROLLBACK（简化处理）
        if (peekKeyword("TRANSACTION")) {
            consume();
        }
        return stmt;
    }
    
    // BEGIN语句
    public static class BeginStatement extends SQLStatement {
        public BeginStatement() {
            this.type = StatementType.BEGIN;
        }
    }
    
    // COMMIT语句
    public static class CommitStatement extends SQLStatement {
        public CommitStatement() {
            this.type = StatementType.COMMIT;
        }
    }
    
    // ROLLBACK语句
    public static class RollbackStatement extends SQLStatement {
        public RollbackStatement() {
            this.type = StatementType.ROLLBACK;
        }
    }
    
    /**
     * 解析RENAME TABLE语句
     */
    private RenameTableStatement parseRenameTable() {
        expectKeyword("TABLE");
        RenameTableStatement stmt = new RenameTableStatement();
        stmt.oldName = expectIdentifier();
        expectKeyword("TO");
        stmt.newName = expectIdentifier();
        return stmt;
    }
    
    /**
     * 解析INSERT语句
     */
    private InsertStatement parseInsert() {
        expectKeyword("INTO");
        InsertStatement stmt = new InsertStatement();
        stmt.tableName = expectIdentifier();
        
        // 可选：指定列名
        if (peekPunctuation("(")) {
            consume();
            while (!peekPunctuation(")")) {
                stmt.columnNames.add(expectIdentifier());
                if (peekPunctuation(",")) {
                    consume();
                } else {
                    break;
                }
            }
            expectPunctuation(")");
        }
        
        expectKeyword("VALUES");
        expectPunctuation("(");
        
        while (!peekPunctuation(")")) {
            Object value = parseValue();
            stmt.values.add(value);
            if (peekPunctuation(",")) {
                consume();
            } else {
                break;
            }
        }
        
        expectPunctuation(")");
        return stmt;
    }
    
    /**
     * 解析UPDATE语句
     */
    private UpdateStatement parseUpdate() {
        UpdateStatement stmt = new UpdateStatement();
        stmt.tableName = expectIdentifier();
        expectKeyword("SET");
        
        while (true) {
            String columnName = expectIdentifier();
            expectOperator("=");
            
            // 解析UPDATE表达式（支持 column + number, column - number 等）
            UpdateExpression expr = parseUpdateExpression();
            stmt.columnNames.add(columnName);
            stmt.expressions.add(expr);
            
            if (peekKeyword("WHERE")) {
                break;
            }
            if (peekPunctuation(",")) {
                consume();
            } else {
                break;
            }
        }
        
        if (peekKeyword("WHERE")) {
            consume();
            stmt.whereCondition = parseSingleWhereConditionForDML();
        }
        
        return stmt;
    }
    
    /**
     * 解析DELETE语句
     */
    private DeleteStatement parseDelete() {
        expectKeyword("FROM");
        DeleteStatement stmt = new DeleteStatement();
        stmt.tableName = expectIdentifier();
        
        if (peekKeyword("WHERE")) {
            consume();
            stmt.whereCondition = parseSingleWhereConditionForDML();
        }
        
        return stmt;
    }
    
    /**
     * 解析SELECT语句
     */
    private SelectStatement parseSelect() {
        SelectStatement stmt = new SelectStatement();
        
        // 解析列名（支持 alias.column 格式、聚合函数、列别名）
        // *可能被识别为OPERATOR或PUNCTUATION，需要检查两种情况
        if (peekToken(TokenType.PUNCTUATION, "*") || peekToken(TokenType.OPERATOR, "*")) {
            consume();
            stmt.columnNames.add("*");
            stmt.columnAliases.add(null);  // * 没有别名
        } else {
            while (true) {
                String colName;
                String colAlias = null;
                SelectStatement subqueryColumn = null;
                
                // 检查是否是子查询作为列（标量子查询）：(SELECT ...)
                if (peekPunctuation("(") && peekToken(1, TokenType.KEYWORD, "SELECT")) {
                    consume();  // 消费左括号
                    consume();  // 消费 SELECT 关键字（因为 parseSelect 期望它已经被消费）
                    // 解析子查询
                    subqueryColumn = parseSelect();
                    expectPunctuation(")");
                    // 子查询列名使用特殊标记，实际值在执行时计算
                    colName = "__SUBQUERY__";
                } else if (peekKeyword("COUNT") || peekKeyword("SUM") || peekKeyword("AVG") || 
                    peekKeyword("MAX") || peekKeyword("MIN")) {
                    // 检查是否是聚合函数（COUNT, SUM, AVG, MAX, MIN）
                    String funcName = consume().value;  // 消费函数名
                    expectPunctuation("(");
                    
                    // 解析函数参数（可能是 * 或列名）
                    String param;
                    if (peekToken(TokenType.PUNCTUATION, "*") || peekToken(TokenType.OPERATOR, "*")) {
                        consume();
                        param = "*";
                    } else {
                        // 支持 alias.column 格式
                        if (peekToken(TokenType.IDENTIFIER) && peekToken(1, TokenType.PUNCTUATION, ".")) {
                            String alias = expectIdentifier();
                            expectPunctuation(".");
                            String column = expectIdentifier();
                            param = alias + "." + column;
                        } else {
                            param = expectIdentifier();
                        }
                    }
                    
                    expectPunctuation(")");
                    colName = funcName + "(" + param + ")";
                } else if (peekToken(TokenType.IDENTIFIER) && peekToken(1, TokenType.PUNCTUATION, ".")) {
                    // 表别名.列名
                    String alias = expectIdentifier();
                    expectPunctuation(".");
                    String column = expectIdentifier();
                    colName = alias + "." + column;
                } else {
                    // 普通列名
                    colName = expectIdentifier();
                }
                
                stmt.columnNames.add(colName);
                stmt.subqueryColumns.add(subqueryColumn);
                
                // 检查是否有列别名（AS 关键字或直接标识符）
                if (peekKeyword("AS")) {
                    consume();  // 消费 AS
                    colAlias = expectIdentifier();
                } else if (peekToken(TokenType.IDENTIFIER) && !peekKeyword("FROM") && 
                          !peekKeyword("WHERE") && !peekKeyword("GROUP") && !peekPunctuation(",")) {
                    // 没有AS关键字，但下一个标识符可能是别名（排除关键字和逗号）
                    colAlias = expectIdentifier();
                }
                
                stmt.columnAliases.add(colAlias);
                
                if (peekPunctuation(",")) {
                    consume();
                } else {
                    break;
                }
            }
        }
        
        expectKeyword("FROM");
        // 解析第一个表（支持别名）
        String firstTable = expectIdentifier();
        String firstAlias = null;
        if (peekKeyword("AS")) {
            consume();
            firstAlias = expectIdentifier();
        } else if (peekToken(TokenType.IDENTIFIER) && !peekKeyword("JOIN") && !peekKeyword("INNER") && 
                   !peekKeyword("LEFT") && !peekKeyword("RIGHT") && !peekKeyword("WHERE") && 
                   !peekPunctuation(",")) {
            // 没有AS关键字，但下一个标识符可能是别名（排除逗号，因为逗号表示隐式JOIN）
            firstAlias = expectIdentifier();
        }
        stmt.tableNames.add(firstTable);
        if (firstAlias != null) {
            stmt.tableAliases.put(firstAlias, firstTable);
        } else {
            // 如果没有别名，使用表名作为别名
            stmt.tableAliases.put(firstTable, firstTable);
        }
        
        // 解析隐式JOIN（逗号连接的表）
        stmt.joinConditions = new ArrayList<>();
        while (peekPunctuation(",")) {
            consume();  // 消费逗号
            String joinTable = expectIdentifier();
            String joinAlias = null;
            if (peekKeyword("AS")) {
                consume();
                joinAlias = expectIdentifier();
            } else if (peekToken(TokenType.IDENTIFIER) && !peekKeyword("JOIN") && !peekKeyword("INNER") && 
                       !peekKeyword("LEFT") && !peekKeyword("RIGHT") && !peekKeyword("WHERE") && 
                       !peekPunctuation(",")) {
                joinAlias = expectIdentifier();
            }
            stmt.tableNames.add(joinTable);
            if (joinAlias != null) {
                stmt.tableAliases.put(joinAlias, joinTable);
            } else {
                stmt.tableAliases.put(joinTable, joinTable);
            }
        }
        
        // 解析显式JOIN
        while (peekKeyword("JOIN") || peekKeyword("INNER") || peekKeyword("LEFT") || peekKeyword("RIGHT")) {
            // 跳过JOIN类型关键字
            if (peekKeyword("INNER") || peekKeyword("LEFT") || peekKeyword("RIGHT")) {
                consume();
            }
            expectKeyword("JOIN");
            
            // 解析JOIN的表名和别名
            String joinTable = expectIdentifier();
            String joinAlias = null;
            if (peekKeyword("AS")) {
                consume();
                joinAlias = expectIdentifier();
            } else if (peekToken(TokenType.IDENTIFIER) && !peekKeyword("ON") && !peekKeyword("WHERE")) {
                joinAlias = expectIdentifier();
            }
            stmt.tableNames.add(joinTable);
            if (joinAlias != null) {
                stmt.tableAliases.put(joinAlias, joinTable);
            } else {
                stmt.tableAliases.put(joinTable, joinTable);
            }
            
            expectKeyword("ON");
            
            // 解析JOIN条件（支持 alias.column = alias.column）
            String leftTableRef, leftColumn, rightTableRef, rightColumn;
            
            // 左侧：alias.column 或 column
            if (peekToken(TokenType.IDENTIFIER) && peekToken(1, TokenType.PUNCTUATION, ".")) {
                leftTableRef = expectIdentifier();
                expectPunctuation(".");
                leftColumn = expectIdentifier();
            } else {
                // 如果没有表别名，使用第一个表
                leftTableRef = stmt.tableNames.get(0);
                leftColumn = expectIdentifier();
            }
            
            // 操作符（通常是 =）
            String op = expectOperator();
            if (!op.equals("=")) {
                throw new SQLException("JOIN condition only supports = operator");
            }
            
            // 右侧：alias.column 或 column
            if (peekToken(TokenType.IDENTIFIER) && peekToken(1, TokenType.PUNCTUATION, ".")) {
                rightTableRef = expectIdentifier();
                expectPunctuation(".");
                rightColumn = expectIdentifier();
            } else {
                // 如果没有表别名，使用当前JOIN的表
                rightTableRef = joinTable;
                rightColumn = expectIdentifier();
            }
            
            // 将别名转换为真实表名
            String leftTable = stmt.tableAliases.getOrDefault(leftTableRef, leftTableRef);
            String rightTable = stmt.tableAliases.getOrDefault(rightTableRef, rightTableRef);
            
            QueryExecutor.JoinCondition joinCond = new QueryExecutor.JoinCondition(
                leftTable, leftColumn, rightTable, rightColumn
            );
            stmt.joinConditions.add(joinCond);
        }
        
        // 解析WHERE子句（支持 alias.column）
        if (peekKeyword("WHERE")) {
            consume();
            stmt.whereCondition = parseWhereCondition();
        }
        
        // 解析GROUP BY子句
        if (peekKeyword("GROUP")) {
            consume();
            expectKeyword("BY");
            while (true) {
                // 支持 alias.column 格式
                String colName;
                if (peekToken(TokenType.IDENTIFIER) && peekToken(1, TokenType.PUNCTUATION, ".")) {
                    String alias = expectIdentifier();
                    expectPunctuation(".");
                    String column = expectIdentifier();
                    colName = alias + "." + column;
                } else {
                    colName = expectIdentifier();
                }
                stmt.groupByColumns.add(colName);
                
                if (peekPunctuation(",")) {
                    consume();
                } else {
                    break;
                }
            }
        }
        
        // 解析ORDER BY子句
        if (peekKeyword("ORDER")) {
            System.out.println("找到ORDER关键字，开始解析ORDER BY");
            consume();
            expectKeyword("BY");
            while (true) {
                // 支持 alias.column 格式
                String colName;
                if (peekToken(TokenType.IDENTIFIER) && peekToken(1, TokenType.PUNCTUATION, ".")) {
                    String alias = expectIdentifier();
                    expectPunctuation(".");
                    String column = expectIdentifier();
                    colName = alias + "." + column;
                } else {
                    colName = expectIdentifier();
                }
                
                System.out.println("解析到排序列: " + colName);
                
                // 解析排序方向（ASC或DESC，默认为ASC）
                boolean ascending = true;
                if (peekKeyword("ASC")) {
                    consume();
                    ascending = true;
                } else if (peekKeyword("DESC")) {
                    consume();
                    ascending = false;
                }
                
                stmt.orderByColumns.add(new OrderByItem(colName, ascending));
                System.out.println("添加ORDER BY项: " + colName + " " + (ascending ? "ASC" : "DESC") + 
                    ", 当前orderByColumns大小: " + stmt.orderByColumns.size());
                
                if (peekPunctuation(",")) {
                    consume();
                } else {
                    break;
                }
            }
            System.out.println("ORDER BY解析完成，共 " + stmt.orderByColumns.size() + " 个排序列");
        } else {
            System.out.println("未找到ORDER关键字");
        }
        
        return stmt;
    }
    
    /**
     * 解析WHERE条件（支持多个AND/OR条件，支持括号）
     */
    private WhereCondition parseWhereCondition() {
        // 解析第一个条件（可能是括号表达式或单个条件）
        WhereCondition left = parseWhereConditionOrExpression();
        
        // 解析后续的AND/OR条件
        while (peekKeyword("AND") || peekKeyword("OR")) {
            WhereCondition.LogicOp logicOp = peekKeyword("AND") ? WhereCondition.LogicOp.AND : WhereCondition.LogicOp.OR;
            consume();  // 消费AND或OR
            WhereCondition right = parseWhereConditionOrExpression();
            left = new WhereCondition(left, logicOp, right);
        }
        
        return left;
    }
    
    /**
     * 解析WHERE条件或括号表达式
     */
    private WhereCondition parseWhereConditionOrExpression() {
        // 如果遇到左括号，递归解析括号内的条件
        if (peekPunctuation("(")) {
            consume();  // 消费左括号
            WhereCondition condition = parseWhereCondition();  // 递归解析括号内的条件
            expectPunctuation(")");  // 消费右括号
            return condition;
        } else {
            // 否则解析单个条件
            return parseSingleWhereCondition();
        }
    }
    
    /**
     * 解析单个WHERE条件（支持 table.column 或 alias.column 格式）
     */
    private WhereCondition parseSingleWhereCondition() {
        String columnName;
        
        // 支持 table.column 或 alias.column 格式
        if (peekToken(TokenType.IDENTIFIER) && peekToken(1, TokenType.PUNCTUATION, ".")) {
            String tableRef = expectIdentifier();
            expectPunctuation(".");
            String column = expectIdentifier();
            columnName = tableRef + "." + column;
        } else {
            // 普通列名
            columnName = expectIdentifier();
        }
        
        // 解析操作符（支持 LIKE、IN、NOT IN 和 BETWEEN 关键字）
        String operator;
        if (peekKeyword("NOT") && peekToken(1, TokenType.KEYWORD, "IN")) {
            // 处理 NOT IN
            consume();  // 消费 NOT 关键字
            consume();  // 消费 IN 关键字
            operator = "NOT IN";
        } else if (peekKeyword("LIKE")) {
            consume();  // 消费 LIKE 关键字
            operator = "LIKE";
        } else if (peekKeyword("IN")) {
            consume();  // 消费 IN 关键字
            operator = "IN";
        } else if (peekKeyword("BETWEEN")) {
            consume();  // 消费 BETWEEN 关键字
            operator = "BETWEEN";
        } else {
            operator = expectOperator();
        }
        
        // 对于等号操作符，检查右侧是否是列名（table.column格式）而不是值
        Object value = null;
        Object minValue = null;
        Object maxValue = null;
        com.dbms.parser.SQLParser.SelectStatement subquery = null;
        
        // 处理 BETWEEN ... AND ... 语法
        if (operator.equals("BETWEEN")) {
            // 解析第一个值（最小值）
            minValue = parseValue();
            // 期望 AND 关键字
            if (!peekKeyword("AND")) {
                throw new SQLException("Expected AND after BETWEEN value");
            }
            consume();  // 消费 AND 关键字
            // 解析第二个值（最大值）
            maxValue = parseValue();
        } else if ((operator.equals("IN") || operator.equals("NOT IN")) && peekPunctuation("(")) {
            // 检查是否是IN (SELECT ...)或NOT IN (SELECT ...)子查询
            consume();  // 消费左括号
            // 检查是否是SELECT语句
            if (peekKeyword("SELECT")) {
                consume();  // 消费 SELECT 关键字（因为 parseSelect 期望它已经被消费）
                // 解析子查询
                subquery = parseSelect();
                expectPunctuation(")");
            } else {
                // 普通的IN值列表（暂不支持，先抛出异常）
                throw new SQLException("IN clause with value list not supported yet, use IN (SELECT ...)");
            }
        } else if (operator.equals("=") && peekToken(TokenType.IDENTIFIER) && 
            peekToken(1, TokenType.PUNCTUATION, ".")) {
            // 右侧是列名（table.column格式）
            String rightTableRef = expectIdentifier();
            expectPunctuation(".");
            String rightColumn = expectIdentifier();
            value = rightTableRef + "." + rightColumn;
        } else {
            // 右侧是值（字符串、数字、NULL等）
            value = parseValue();
        }
        
        DMLExecutor.QueryCondition condition;
        if (operator.equals("BETWEEN")) {
            condition = new DMLExecutor.QueryCondition(columnName, operator, minValue, maxValue);
        } else if (subquery != null) {
            condition = new DMLExecutor.QueryCondition(columnName, operator, subquery);
        } else {
            condition = new DMLExecutor.QueryCondition(columnName, operator, value);
        }
        return new WhereCondition(condition);
    }
    
    /**
     * 解析单个WHERE条件（用于UPDATE和DELETE，返回DMLExecutor.QueryCondition）
     */
    private DMLExecutor.QueryCondition parseSingleWhereConditionForDML() {
        String columnName;
        
        // 支持 table.column 格式（虽然UPDATE/DELETE通常是单表，但为了兼容性支持）
        if (peekToken(TokenType.IDENTIFIER) && peekToken(1, TokenType.PUNCTUATION, ".")) {
            String tableRef = expectIdentifier();
            expectPunctuation(".");
            String column = expectIdentifier();
            columnName = tableRef + "." + column;
        } else {
            // 普通列名
            columnName = expectIdentifier();
        }
        
        // 解析操作符（支持 LIKE 和 BETWEEN 关键字）
        String operator;
        if (peekKeyword("LIKE")) {
            consume();  // 消费 LIKE 关键字
            operator = "LIKE";
        } else if (peekKeyword("BETWEEN")) {
            consume();  // 消费 BETWEEN 关键字
            operator = "BETWEEN";
        } else {
            operator = expectOperator();
        }
        
        // 处理 BETWEEN ... AND ... 语法
        if (operator.equals("BETWEEN")) {
            Object minValue = parseValue();
            if (!peekKeyword("AND")) {
                throw new SQLException("Expected AND after BETWEEN value");
            }
            consume();  // 消费 AND 关键字
            Object maxValue = parseValue();
            return new DMLExecutor.QueryCondition(columnName, operator, minValue, maxValue);
        } else {
            Object value = parseValue();
            return new DMLExecutor.QueryCondition(columnName, operator, value);
        }
    }
    
    /**
     * 解析UPDATE表达式（支持 column + number, column - number 等）
     */
    private UpdateExpression parseUpdateExpression() {
        // 检查是否是表达式：column operator number
        if (peekToken(TokenType.IDENTIFIER)) {
            Token nextToken = peekToken(1);
            
            // 如果下一个token是操作符（+, -, *, /），且再下一个是数字，则是表达式
            if (nextToken != null && 
                (nextToken.type == TokenType.OPERATOR || nextToken.type == TokenType.PUNCTUATION) &&
                (nextToken.value.equals("+") || nextToken.value.equals("-") || 
                 nextToken.value.equals("*") || nextToken.value.equals("/"))) {
                
                String columnName = expectIdentifier();
                
                // 解析操作符（应该是OPERATOR类型）
                String operator = expectOperator();
                
                // 检查操作符是否是 +, -, *, /
                if (!operator.equals("+") && !operator.equals("-") && 
                    !operator.equals("*") && !operator.equals("/")) {
                    throw new SQLException("Unsupported operator in expression: " + operator);
                }
                
                // 解析数字值
                Object value = parseValue();
                return new UpdateExpression(columnName, operator, value);
            }
        }
        
        // 否则是简单值
        Object value = parseValue();
        return new UpdateExpression(value);
    }
    
    /**
     * 解析值（字符串、数字、NULL）
     */
    private Object parseValue() {
        if (peekToken(TokenType.STRING)) {
            return expectToken(TokenType.STRING).value;
        } else if (peekToken(TokenType.NUMBER)) {
            String numStr = expectToken(TokenType.NUMBER).value;
            if (numStr.contains(".")) {
                return Double.parseDouble(numStr);
            } else {
                return Integer.parseInt(numStr);
            }
        } else if (peekKeyword("NULL")) {
            consume();
            return null;
        } else {
            throw new SQLException("Unexpected token in value: " + currentToken());
        }
    }
    
    // 辅助方法
    private Token currentToken() {
        if (currentPos >= tokens.size()) {
            return tokens.get(tokens.size() - 1); // EOF token
        }
        return tokens.get(currentPos);
    }
    
    private Token consume() {
        if (currentPos < tokens.size()) {
            return tokens.get(currentPos++);
        }
        return tokens.get(tokens.size() - 1);
    }
    
    private Token expectToken(TokenType type) {
        Token token = currentToken();
        if (token.type != type) {
            throw new SQLException("Expected " + type + ", got " + token.type + " at " + token);
        }
        return consume();
    }
    
    private String expectIdentifier() {
        return expectToken(TokenType.IDENTIFIER).value;
    }
    
    private String expectKeyword(String keyword) {
        Token token = expectToken(TokenType.KEYWORD);
        if (!token.value.equals(keyword)) {
            throw new SQLException("Expected keyword " + keyword + ", got " + token.value);
        }
        return token.value;
    }
    
    private String expectOperator() {
        return expectToken(TokenType.OPERATOR).value;
    }
    
    private String expectOperator(String op) {
        Token token = expectToken(TokenType.OPERATOR);
        if (!token.value.equals(op)) {
            throw new SQLException("Expected operator " + op + ", got " + token.value);
        }
        return token.value;
    }
    
    private void expectPunctuation(String punct) {
        Token token = expectToken(TokenType.PUNCTUATION);
        if (!token.value.equals(punct)) {
            throw new SQLException("Expected " + punct + ", got " + token.value);
        }
    }
    
    private boolean peekToken(TokenType type) {
        return currentToken().type == type;
    }
    
    private boolean peekToken(TokenType type, String value) {
        Token token = currentToken();
        return token.type == type && token.value.equals(value);
    }
    
    private boolean peekToken(int offset, TokenType type) {
        int pos = currentPos + offset;
        if (pos >= tokens.size()) {
            return false;
        }
        return tokens.get(pos).type == type;
    }
    
    private boolean peekToken(int offset, TokenType type, String value) {
        int pos = currentPos + offset;
        if (pos >= tokens.size()) {
            return false;
        }
        Token token = tokens.get(pos);
        return token.type == type && token.value.equals(value);
    }
    
    /**
     * 查看指定偏移量的token（返回Token对象）
     */
    private Token peekToken(int offset) {
        int pos = currentPos + offset;
        if (pos >= tokens.size()) {
            return tokens.get(tokens.size() - 1); // EOF token
        }
        return tokens.get(pos);
    }
    
    private boolean peekKeyword(String keyword) {
        return peekToken(TokenType.KEYWORD, keyword);
    }
    
    private boolean peekPunctuation(String punct) {
        return peekToken(TokenType.PUNCTUATION, punct);
    }
}

