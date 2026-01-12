package com.dbms.util;

/**
 * DBMS自定义异常类
 */
public class DBMSException extends RuntimeException {
    
    public DBMSException(String message) {
        super(message);
    }
    
    public DBMSException(String message, Throwable cause) {
        super(message, cause);
    }
}

