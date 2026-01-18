package com.dbms.util;

import com.dbms.model.Database;
import com.dbms.storage.DBFFileManager;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 数据备份和恢复管理器
 */
public class BackupManager {
    
    /**
     * 备份数据库
     * @param dbFilePath 数据库文件路径（.dbf）
     * @param backupDir 备份目录
     * @return 备份文件路径
     */
    public static String backupDatabase(String dbFilePath, String backupDir) throws IOException {
        File dbFile = new File(dbFilePath);
        if (!dbFile.exists()) {
            throw new IOException("Database file does not exist: " + dbFilePath);
        }
        
        // 创建备份目录（如果不存在）
        File backupDirectory = new File(backupDir);
        if (!backupDirectory.exists()) {
            backupDirectory.mkdirs();
        }
        
        // 生成备份文件名（包含时间戳）
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String timestamp = sdf.format(new Date());
        String dbFileName = new File(dbFilePath).getName();
        String backupFileName = dbFileName.replace(".dbf", "_backup_" + timestamp + ".dbf");
        String backupFilePath = new File(backupDir, backupFileName).getAbsolutePath();
        
        // 复制.dbf文件
        Files.copy(dbFile.toPath(), Paths.get(backupFilePath), StandardCopyOption.REPLACE_EXISTING);
        
        // 读取数据库，获取所有表的.dat文件路径
        Database database = DBFFileManager.readDatabaseFile(dbFilePath);
        
        // 备份所有表的.dat文件
        // 如果datFilePath为null，使用dbFilePath的目录
        String baseDatPath = database.getDatFilePath();
        if (baseDatPath == null || baseDatPath.isEmpty()) {
            // 从dbFilePath推断dat文件路径
            File dbFileObj = new File(dbFilePath);
            String dbDir = dbFileObj.getParent();
            if (dbDir == null) {
                dbDir = ".";
            }
            String dbBaseName = dbFileObj.getName().replace(".dbf", "");
            baseDatPath = new File(dbDir, dbBaseName + ".dat").getAbsolutePath();
        }
        
        for (String tableName : database.getTableNames()) {
            String datFilePath = com.dbms.storage.DATFileManager.getTableDataFilePath(
                baseDatPath, tableName);
            File datFile = new File(datFilePath);
            
            if (datFile.exists()) {
                String datBackupFileName = datFile.getName().replace(".dat", "_backup_" + timestamp + ".dat");
                String datBackupFilePath = new File(backupDir, datBackupFileName).getAbsolutePath();
                Files.copy(datFile.toPath(), Paths.get(datBackupFilePath), StandardCopyOption.REPLACE_EXISTING);
            }
        }
        
        return backupFilePath;
    }
    
    /**
     * 恢复数据库
     * @param backupFilePath 备份文件路径（.dbf）
     * @param targetDbFilePath 目标数据库文件路径
     */
    public static void restoreDatabase(String backupFilePath, String targetDbFilePath) throws IOException {
        File backupFile = new File(backupFilePath);
        if (!backupFile.exists()) {
            throw new IOException("Backup file does not exist: " + backupFilePath);
        }
        
        // 读取备份数据库
        Database backupDatabase = DBFFileManager.readDatabaseFile(backupFilePath);
        
        // 确定备份目录
        String backupDir = backupFile.getParent();
        
        // 提取时间戳（从文件名中）
        String backupFileName = backupFile.getName();
        String timestamp = extractTimestamp(backupFileName);
        
        // 恢复.dbf文件
        Files.copy(backupFile.toPath(), Paths.get(targetDbFilePath), StandardCopyOption.REPLACE_EXISTING);
        
        // 确定目标数据库的.dat文件目录
        File targetDbFile = new File(targetDbFilePath);
        String targetDatDir = targetDbFile.getParent();
        String baseDatFileName = targetDbFile.getName().replace(".dbf", "");
        
        // 恢复所有表的.dat文件
        for (String tableName : backupDatabase.getTableNames()) {
            String datBackupFileName = baseDatFileName + "_" + tableName + "_backup_" + timestamp + ".dat";
            String datBackupFilePath = new File(backupDir, datBackupFileName).getAbsolutePath();
            File datBackupFile = new File(datBackupFilePath);
            
            if (datBackupFile.exists()) {
                String targetDatFilePath = com.dbms.storage.DATFileManager.getTableDataFilePath(
                    new File(targetDatDir, baseDatFileName + ".dat").getAbsolutePath(), tableName);
                Files.copy(datBackupFile.toPath(), Paths.get(targetDatFilePath), StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }
    
    /**
     * 从备份文件名中提取时间戳
     */
    private static String extractTimestamp(String fileName) {
        // 格式：xxx_backup_yyyyMMdd_HHmmss.dbf
        int backupIndex = fileName.indexOf("_backup_");
        if (backupIndex >= 0) {
            String afterBackup = fileName.substring(backupIndex + 8);
            int dotIndex = afterBackup.lastIndexOf(".");
            if (dotIndex > 0) {
                return afterBackup.substring(0, dotIndex);
            }
        }
        return "";
    }
    
    /**
     * 列出所有备份文件
     * @param backupDir 备份目录
     * @return 备份文件列表
     */
    public static File[] listBackups(String backupDir) {
        File backupDirectory = new File(backupDir);
        if (!backupDirectory.exists()) {
            return new File[0];
        }
        
        return backupDirectory.listFiles((dir, name) -> name.endsWith(".dbf") && name.contains("_backup_"));
    }
}

