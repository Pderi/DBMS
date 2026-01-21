package com.dbms.util;

import com.dbms.model.Record;
import com.dbms.model.Table;
import com.dbms.storage.DATFileManager;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 事务管理器
 */
public class TransactionManager {
    private Map<String, Transaction> activeTransactions;
    private Transaction currentTransaction;
    private com.dbms.engine.DDLExecutor ddlExecutor; // 用于回滚时获取表结构
    
    public TransactionManager() {
        this.activeTransactions = new HashMap<>();
    }
    
    public TransactionManager(com.dbms.engine.DDLExecutor ddlExecutor) {
        this();
        this.ddlExecutor = ddlExecutor;
    }
    
    /**
     * 绑定DDL执行器（用于回滚时获取表结构）
     */
    public void setDDLExecutor(com.dbms.engine.DDLExecutor ddlExecutor) {
        this.ddlExecutor = ddlExecutor;
    }
    
    /**
     * 开始事务
     */
    public Transaction beginTransaction() {
        String transactionId = UUID.randomUUID().toString();
        Transaction transaction = new Transaction(transactionId);
        activeTransactions.put(transactionId, transaction);
        currentTransaction = transaction;
        return transaction;
    }
    
    /**
     * 提交事务
     */
    public void commit(Transaction transaction) throws IOException {
        if (transaction == null) {
            throw new DBMSException("No active transaction");
        }
        
        if (transaction.getStatus() != Transaction.TransactionStatus.ACTIVE) {
            throw new DBMSException("Transaction is not active");
        }
        
        // 操作已经在执行时完成，这里只需要标记为已提交
        
        transaction.setStatus(Transaction.TransactionStatus.COMMITTED);
        activeTransactions.remove(transaction.getTransactionId());
        
        if (currentTransaction == transaction) {
            currentTransaction = null;
        }
    }
    
    /**
     * 回滚事务
     */
    public void rollback(Transaction transaction) throws IOException {
        if (transaction == null) {
            throw new DBMSException("No active transaction");
        }
        
        if (transaction.getStatus() != Transaction.TransactionStatus.ACTIVE) {
            throw new DBMSException("Transaction is not active");
        }
        
        // 反向执行所有操作
        List<Transaction.TransactionOperation> operations = transaction.getOperations();
        for (int i = operations.size() - 1; i >= 0; i--) {
            Transaction.TransactionOperation op = operations.get(i);
            rollbackOperation(op);
        }
        
        transaction.setStatus(Transaction.TransactionStatus.ROLLED_BACK);
        activeTransactions.remove(transaction.getTransactionId());
        
        if (currentTransaction == transaction) {
            currentTransaction = null;
        }
    }
    
    /**
     * 回滚单个操作
     */
    private void rollbackOperation(Transaction.TransactionOperation op) throws IOException {
        // 回滚需要表结构（UPDATE/DELETE场景）
        Table table = null;
        if (ddlExecutor != null && op.tableName != null) {
            table = ddlExecutor.getTable(op.tableName);
        }
        if ((op.type == Transaction.TransactionOperation.OperationType.UPDATE ||
             op.type == Transaction.TransactionOperation.OperationType.DELETE) &&
            table == null) {
            throw new DBMSException("Cannot rollback " + op.type + ": table metadata not available for " + op.tableName);
        }
        
        String dataFilePath = op.dataFilePath;
        if (dataFilePath == null || dataFilePath.isEmpty()) {
            throw new DBMSException("Cannot rollback " + op.type + ": dataFilePath is missing for table " + op.tableName);
        }
        
        switch (op.type) {
            case INSERT:
                // 删除插入的记录（逻辑删除）
                DATFileManager.deleteRecord(dataFilePath, op.recordPosition);
                break;
            case UPDATE:
                // 恢复旧值
                if (op.oldValue instanceof Record) {
                    DATFileManager.writeRecordAt(dataFilePath, op.recordPosition,
                        (Record) op.oldValue, table);
                }
                break;
            case DELETE:
                // 恢复删除的记录：将旧记录写回（会写回ACTIVE状态+字段值）
                if (op.oldValue instanceof Record) {
                    DATFileManager.writeRecordAt(dataFilePath, op.recordPosition,
                        (Record) op.oldValue, table);
                } else {
                    // 最低限度：取消逻辑删除标记
                    try (java.io.RandomAccessFile raf = new java.io.RandomAccessFile(dataFilePath, "rw")) {
                        raf.seek(op.recordPosition);
                        raf.writeInt(com.dbms.storage.FileFormat.RECORD_ACTIVE);
                    }
                }
                break;
            default:
                // CREATE_TABLE, DROP_TABLE, ALTER_TABLE等DDL操作
                // 需要更复杂的回滚逻辑，这里简化处理
                break;
        }
    }
    
    /**
     * 获取当前事务
     */
    public Transaction getCurrentTransaction() {
        return currentTransaction;
    }
    
    /**
     * 检查是否有活动事务
     */
    public boolean hasActiveTransaction() {
        return currentTransaction != null && 
               currentTransaction.getStatus() == Transaction.TransactionStatus.ACTIVE;
    }
    
    /**
     * 记录事务操作（由DML/DDL执行器在执行时调用）
     */
    public void recordOperation(Transaction.TransactionOperation operation) {
        if (!hasActiveTransaction()) {
            return;
        }
        currentTransaction.addOperation(operation);
    }
}

