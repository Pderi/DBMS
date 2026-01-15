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
    
    public TransactionManager() {
        this.activeTransactions = new HashMap<>();
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
        
        // 执行所有操作
        for (Transaction.TransactionOperation op : transaction.getOperations()) {
            // 操作已经在执行时记录，这里只需要标记为已提交
            // 实际的数据修改已经在执行时完成
        }
        
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
        switch (op.type) {
            case INSERT:
                // 删除插入的记录（逻辑删除）
                String insertDatFile = com.dbms.storage.DATFileManager.getTableDataFilePath(
                    "database.dat", op.tableName);
                DATFileManager.deleteRecord(insertDatFile, op.recordPosition);
                break;
            case UPDATE:
                // 恢复旧值
                String updateDatFile = com.dbms.storage.DATFileManager.getTableDataFilePath(
                    "database.dat", op.tableName);
                if (op.oldValue instanceof Record) {
                    Table table = null; // 需要从数据库获取表结构
                    DATFileManager.writeRecordAt(updateDatFile, op.recordPosition, 
                        (Record) op.oldValue, table);
                }
                break;
            case DELETE:
                // 恢复删除的记录（标记为未删除）
                String deleteDatFile = com.dbms.storage.DATFileManager.getTableDataFilePath(
                    "database.dat", op.tableName);
                // 需要读取记录并恢复
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
}

