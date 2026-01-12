package com.dbms.util;

/**
 * SQL执行异常类
 */
public class SQLException extends DBMSException {
    
    public SQLException(String message) {
        super(message);
    }
    
    public SQLException(String message, Throwable cause) {
        super(message, cause);
    }
}

