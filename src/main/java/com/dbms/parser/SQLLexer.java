package com.dbms.parser;

import java.util.ArrayList;
import java.util.List;

/**
 * SQL词法分析器
 */
public class SQLLexer {
    
    // SQL关键字
    private static final String[] KEYWORDS = {
        "SELECT", "FROM", "WHERE", "INSERT", "INTO", "VALUES", "UPDATE", "SET",
        "DELETE", "CREATE", "TABLE", "ALTER", "DROP", "ADD", "COLUMN", "MODIFY",
        "RENAME", "TO", "AS", "AND", "OR", "NOT", "NULL", "PRIMARY", "KEY",
        "INT", "VARCHAR", "CHAR", "DATE", "FLOAT", "DOUBLE", "JOIN", "ON", "INNER", "LEFT", "RIGHT",
        "LIKE",  // LIKE 操作符
        "COUNT", "SUM", "AVG", "MAX", "MIN",  // 聚合函数
        "GROUP", "BY"  // GROUP BY 子句
    };
    
    // Token类型
    public enum TokenType {
        KEYWORD, IDENTIFIER, STRING, NUMBER, OPERATOR, PUNCTUATION, EOF
    }
    
    // Token类
    public static class Token {
        public TokenType type;
        public String value;
        public int line;
        public int column;
        
        public Token(TokenType type, String value, int line, int column) {
            this.type = type;
            this.value = value;
            this.line = line;
            this.column = column;
        }
        
        @Override
        public String toString() {
            return String.format("Token(%s, '%s', %d:%d)", type, value, line, column);
        }
    }
    
    /**
     * 词法分析
     */
    public List<Token> tokenize(String sql) {
        List<Token> tokens = new ArrayList<>();
        sql = sql.trim();
        
        int pos = 0;
        int line = 1;
        int column = 1;
        
        while (pos < sql.length()) {
            char ch = sql.charAt(pos);
            
            // 跳过空白字符
            if (Character.isWhitespace(ch)) {
                if (ch == '\n') {
                    line++;
                    column = 1;
                } else {
                    column++;
                }
                pos++;
                continue;
            }
            
            // 字符串字面量
            if (ch == '\'' || ch == '"') {
                pos++;
                column++;
                StringBuilder sb = new StringBuilder();
                boolean escaped = false;
                
                while (pos < sql.length()) {
                    char c = sql.charAt(pos);
                    if (escaped) {
                        sb.append(c);
                        escaped = false;
                    } else if (c == '\\') {
                        escaped = true;
                    } else if (c == ch) {
                        pos++;
                        column++;
                        break;
                    } else {
                        sb.append(c);
                    }
                    pos++;
                    column++;
                }
                
                tokens.add(new Token(TokenType.STRING, sb.toString(), line, column));
                continue;
            }
            
            // 操作符（必须在数字之前检查，避免将 + 和 - 识别为数字的一部分）
            if (isOperator(ch)) {
                StringBuilder op = new StringBuilder();
                op.append(ch);
                pos++;
                column++;
                
                // 处理多字符操作符 (<=, >=, !=, <>)
                if (pos < sql.length()) {
                    char next = sql.charAt(pos);
                    if ((ch == '<' && (next == '=' || next == '>')) ||
                        (ch == '>' && next == '=') ||
                        (ch == '!' && next == '=')) {
                        op.append(next);
                        pos++;
                        column++;
                    }
                }
                
                tokens.add(new Token(TokenType.OPERATOR, op.toString(), line, column));
                continue;
            }
            
            // 数字（在操作符之后检查，避免将 + 和 - 识别为数字的一部分）
            if (Character.isDigit(ch)) {
                int start = pos;
                pos++;
                column++;
                
                while (pos < sql.length() && 
                       (Character.isDigit(sql.charAt(pos)) || sql.charAt(pos) == '.')) {
                    pos++;
                    column++;
                }
                
                String numStr = sql.substring(start, pos);
                tokens.add(new Token(TokenType.NUMBER, numStr, line, column));
                continue;
            }
            
            // 标点符号
            if (ch == '(' || ch == ')' || ch == ',' || ch == ';' || ch == '.') {
                tokens.add(new Token(TokenType.PUNCTUATION, String.valueOf(ch), line, column));
                pos++;
                column++;
                continue;
            }
            
            // 标识符和关键字
            if (Character.isLetter(ch) || ch == '_') {
                int start = pos;
                while (pos < sql.length() && 
                       (Character.isLetterOrDigit(sql.charAt(pos)) || sql.charAt(pos) == '_')) {
                    pos++;
                    column++;
                }
                
                String word = sql.substring(start, pos).toUpperCase();
                
                // 检查是否是关键字
                boolean isKeyword = false;
                for (String keyword : KEYWORDS) {
                    if (keyword.equals(word)) {
                        tokens.add(new Token(TokenType.KEYWORD, word, line, column));
                        isKeyword = true;
                        break;
                    }
                }
                
                if (!isKeyword) {
                    tokens.add(new Token(TokenType.IDENTIFIER, sql.substring(start, pos), line, column));
                }
                continue;
            }
            
            // 未知字符
            throw new RuntimeException("Unexpected character: " + ch + " at line " + line + ", column " + column);
        }
        
        tokens.add(new Token(TokenType.EOF, "", line, column));
        return tokens;
    }
    
    private boolean isOperator(char ch) {
        return ch == '=' || ch == '<' || ch == '>' || ch == '!' || ch == '+' || ch == '-' || 
               ch == '*' || ch == '/';
    }
}

