package com.dbms.util;

import java.util.ArrayList;
import java.util.List;

/**
 * 事务类
 */
public class Transaction {
    private String transactionId;
    private TransactionStatus status;
    private List<TransactionOperation> operations;
    private long startTime;
    
    public enum TransactionStatus {
        ACTIVE, COMMITTED, ROLLED_BACK
    }
    
    public Transaction(String transactionId) {
        this.transactionId = transactionId;
        this.status = TransactionStatus.ACTIVE;
        this.operations = new ArrayList<>();
        this.startTime = System.currentTimeMillis();
    }
    
    public String getTransactionId() {
        return transactionId;
    }
    
    public TransactionStatus getStatus() {
        return status;
    }
    
    public void setStatus(TransactionStatus status) {
        this.status = status;
    }
    
    public List<TransactionOperation> getOperations() {
        return operations;
    }
    
    public void addOperation(TransactionOperation operation) {
        operations.add(operation);
    }
    
    public long getStartTime() {
        return startTime;
    }
    
    /**
     * 事务操作记录
     */
    public static class TransactionOperation {
        public enum OperationType {
            INSERT, UPDATE, DELETE, CREATE_TABLE, DROP_TABLE, ALTER_TABLE
        }
        
        public OperationType type;
        public String tableName;
        public Object oldValue;  // 用于回滚
        public Object newValue;  // 新值
        public long recordPosition;  // 记录位置（用于UPDATE/DELETE）
        
        public TransactionOperation(OperationType type, String tableName, Object oldValue, Object newValue, long recordPosition) {
            this.type = type;
            this.tableName = tableName;
            this.oldValue = oldValue;
            this.newValue = newValue;
            this.recordPosition = recordPosition;
        }
    }
}

