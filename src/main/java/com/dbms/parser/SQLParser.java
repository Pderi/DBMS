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
        CREATE_TABLE, ALTER_TABLE, DROP_TABLE, RENAME_TABLE,
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
    
    // UPDATE语句
    public static class UpdateStatement extends SQLStatement {
        public String tableName;
        public List<String> columnNames;
        public List<Object> values;
        public DMLExecutor.QueryCondition whereCondition;
        
        public UpdateStatement() {
            this.type = StatementType.UPDATE;
            this.columnNames = new ArrayList<>();
            this.values = new ArrayList<>();
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
    
    // SELECT语句
    public static class SelectStatement extends SQLStatement {
        public List<String> columnNames;
        public List<String> tableNames;
        public java.util.Map<String, String> tableAliases; // 别名 -> 真实表名
        public List<QueryExecutor.JoinCondition> joinConditions;
        public DMLExecutor.QueryCondition whereCondition;
        
        public SelectStatement() {
            this.type = StatementType.SELECT;
            this.columnNames = new ArrayList<>();
            this.tableNames = new ArrayList<>();
            this.tableAliases = new java.util.HashMap<>();
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
                return parseCreateTable();
            case "ALTER":
                return parseAlterTable();
            case "DROP":
                return parseDropTable();
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
        
        String alterType = expectIdentifier().toUpperCase();
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
     * 解析DROP TABLE语句
     */
    private DropTableStatement parseDropTable() {
        expectKeyword("TABLE");
        DropTableStatement stmt = new DropTableStatement();
        stmt.tableName = expectIdentifier();
        return stmt;
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
            Object value = parseValue();
            stmt.columnNames.add(columnName);
            stmt.values.add(value);
            
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
            stmt.whereCondition = parseWhereCondition();
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
            stmt.whereCondition = parseWhereCondition();
        }
        
        return stmt;
    }
    
    /**
     * 解析SELECT语句
     */
    private SelectStatement parseSelect() {
        SelectStatement stmt = new SelectStatement();
        
        // 解析列名（支持 alias.column 格式）
        // *可能被识别为OPERATOR或PUNCTUATION，需要检查两种情况
        if (peekToken(TokenType.PUNCTUATION, "*") || peekToken(TokenType.OPERATOR, "*")) {
            consume();
            stmt.columnNames.add("*");
        } else {
            while (true) {
                // 支持 alias.column 格式
                String colName;
                if (peekToken(TokenType.IDENTIFIER) && peekToken(1, TokenType.PUNCTUATION, ".")) {
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
                   !peekKeyword("LEFT") && !peekKeyword("RIGHT") && !peekKeyword("WHERE")) {
            // 没有AS关键字，但下一个标识符可能是别名
            firstAlias = expectIdentifier();
        }
        stmt.tableNames.add(firstTable);
        if (firstAlias != null) {
            stmt.tableAliases.put(firstAlias, firstTable);
        } else {
            // 如果没有别名，使用表名作为别名
            stmt.tableAliases.put(firstTable, firstTable);
        }
        
        // 解析多个JOIN
        stmt.joinConditions = new ArrayList<>();
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
        
        return stmt;
    }
    
    /**
     * 解析WHERE条件
     */
    private DMLExecutor.QueryCondition parseWhereCondition() {
        String columnName = expectIdentifier();
        String operator = expectOperator();
        Object value = parseValue();
        return new DMLExecutor.QueryCondition(columnName, operator, value);
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
    
    private boolean peekKeyword(String keyword) {
        return peekToken(TokenType.KEYWORD, keyword);
    }
    
    private boolean peekPunctuation(String punct) {
        return peekToken(TokenType.PUNCTUATION, punct);
    }
}

