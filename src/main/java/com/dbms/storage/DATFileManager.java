package com.dbms.storage;

import com.dbms.model.Field;
import com.dbms.model.Record;
import com.dbms.model.Table;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * .dat文件管理器 - 负责记录的存储和读取
 * 每个表使用独立的数据文件：table_name.dat
 */
public class DATFileManager {
    
    /**
     * 获取表的数据文件路径
     */
    public static String getTableDataFilePath(String basePath, String tableName) {
        // 如果basePath是完整路径，提取目录
        String dir = basePath;
        String baseName = "database";
        if (basePath.contains(File.separator)) {
            int lastSep = basePath.lastIndexOf(File.separator);
            dir = basePath.substring(0, lastSep + 1);
            String fileName = basePath.substring(lastSep + 1);
            if (fileName.endsWith(".dbf")) {
                baseName = fileName.substring(0, fileName.length() - 4);
            } else if (fileName.endsWith(".dat")) {
                baseName = fileName.substring(0, fileName.length() - 4);
            } else {
                baseName = fileName;
            }
        } else if (basePath.endsWith(".dbf")) {
            baseName = basePath.substring(0, basePath.length() - 4);
            dir = "";
        } else if (basePath.endsWith(".dat")) {
            baseName = basePath.substring(0, basePath.length() - 4);
            dir = "";
        }
        
        // 生成表特定的数据文件路径
        return dir + baseName + "_" + tableName + ".dat";
    }
    
    /**
     * 写入记录到数据文件
     */
    public static void writeRecord(RandomAccessFile raf, Record record, Table table) throws IOException {
        // 写入记录状态（4字节）
        int status = record.isDeleted() ? FileFormat.RECORD_DELETED : FileFormat.RECORD_ACTIVE;
        System.out.println("writeRecord: isDeleted=" + record.isDeleted() + ", status=" + status + 
            " (RECORD_ACTIVE=" + FileFormat.RECORD_ACTIVE + ", RECORD_DELETED=" + FileFormat.RECORD_DELETED + ")");
        raf.writeInt(status);
        
        // 写入每个字段的值
        for (int i = 0; i < table.getFieldCount(); i++) {
            Field field = table.getFieldByIndex(i);
            Object value = record.getValue(i);
            writeFieldValue(raf, value, field);
        }
    }
    
    /**
     * 从数据文件读取记录
     */
    public static Record readRecord(RandomAccessFile raf, Table table) throws IOException {
        Record record = new Record(table.getFieldCount());
        
        // 读取记录状态
        int status = raf.readInt();
        boolean isDeleted = (status == FileFormat.RECORD_DELETED);
        record.setDeleted(isDeleted);
        System.out.println("readRecord: status=" + status + ", isDeleted=" + isDeleted + 
            " (RECORD_ACTIVE=" + FileFormat.RECORD_ACTIVE + ", RECORD_DELETED=" + FileFormat.RECORD_DELETED + ")");
        
        // 读取每个字段的值
        for (int i = 0; i < table.getFieldCount(); i++) {
            Field field = table.getFieldByIndex(i);
            Object value = readFieldValue(raf, field);
            record.setValue(i, value);
        }
        
        return record;
    }
    
    /**
     * 写入字段值
     */
    private static void writeFieldValue(RandomAccessFile raf, Object value, Field field) throws IOException {
        if (value == null) {
            // NULL值处理：根据字段类型写入默认值
            writeNullValue(raf, field);
            return;
        }
        
        switch (field.getType()) {
            case INT:
                raf.writeInt((Integer) value);
                break;
            case FLOAT:
            case DOUBLE:
                raf.writeDouble((Double) value);
                break;
            case CHAR:
                BinarySerializer.writeFixedString(
                    new DataOutputStream(new RAFOutputStream(raf)), 
                    value.toString(), 
                    field.getLength());
                break;
            case VARCHAR:
                // 变长字段：先写长度，再写内容
                String str = value.toString();
                byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
                raf.writeInt(bytes.length);
                raf.write(bytes);
                break;
            case DATE:
                // 日期作为字符串存储
                String dateStr = value.toString();
                byte[] dateBytes = dateStr.getBytes(StandardCharsets.UTF_8);
                raf.writeInt(dateBytes.length);
                raf.write(dateBytes);
                break;
        }
    }
    
    /**
     * 读取字段值
     */
    private static Object readFieldValue(RandomAccessFile raf, Field field) throws IOException {
        switch (field.getType()) {
            case INT:
                return raf.readInt();
            case FLOAT:
            case DOUBLE:
                return raf.readDouble();
            case CHAR:
                return BinarySerializer.readFixedString(
                    new DataInputStream(new RAFInputStream(raf)), 
                    field.getLength());
            case VARCHAR:
                // 变长字段：先读长度，再读内容
                int length = raf.readInt();
                if (length == 0) {
                    return null;
                }
                byte[] bytes = new byte[length];
                raf.readFully(bytes);
                return new String(bytes, StandardCharsets.UTF_8);
            case DATE:
                int dateLength = raf.readInt();
                if (dateLength == 0) {
                    return null;
                }
                byte[] dateBytes = new byte[dateLength];
                raf.readFully(dateBytes);
                return new String(dateBytes, StandardCharsets.UTF_8);
            default:
                return null;
        }
    }
    
    /**
     * 写入NULL值
     */
    private static void writeNullValue(RandomAccessFile raf, Field field) throws IOException {
        switch (field.getType()) {
            case INT:
                raf.writeInt(0);
                break;
            case FLOAT:
            case DOUBLE:
                raf.writeDouble(0.0);
                break;
            case CHAR:
                BinarySerializer.writeFixedString(
                    new DataOutputStream(new RAFOutputStream(raf)), 
                    null, 
                    field.getLength());
                break;
            case VARCHAR:
            case DATE:
                raf.writeInt(0); // 长度为0表示NULL
                break;
        }
    }
    
    /**
     * 获取记录在文件中的大小（字节）
     */
    public static int getRecordSize(Table table) {
        return table.getRecordSize();
    }
    
    /**
     * 追加记录到文件末尾
     */
    public static long appendRecord(String filePath, Record record, Table table) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(filePath, "rw")) {
            long position = raf.length();
            raf.seek(position);
            writeRecord(raf, record, table);
            return position;
        }
    }
    
    /**
     * 在指定位置写入记录
     */
    public static void writeRecordAt(String filePath, long position, Record record, Table table) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(filePath, "rw")) {
            raf.seek(position);
            writeRecord(raf, record, table);
        }
    }
    
    /**
     * 从指定位置读取记录
     */
    public static Record readRecordAt(String filePath, long position, Table table) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(filePath, "r")) {
            raf.seek(position);
            return readRecord(raf, table);
        }
    }
    
    /**
     * 读取所有记录
     */
    public static List<Record> readAllRecords(String filePath, Table table) throws IOException {
        List<Record> records = new ArrayList<>();
        
        // 如果文件不存在，返回空列表（新表还没有数据文件）
        File file = new File(filePath);
        if (!file.exists() || file.length() == 0) {
            System.out.println("readAllRecords: 文件不存在或为空: " + filePath);
            return records;
        }
        
        System.out.println("readAllRecords: 文件大小: " + file.length() + " 字节, 表字段数: " + table.getFieldCount());
        
        try (RandomAccessFile raf = new RandomAccessFile(filePath, "r")) {
            long fileLength = raf.length();
            int recordCount = 0;
            int deletedCount = 0;
            int errorCount = 0;
            
            while (raf.getFilePointer() < fileLength) {
                try {
                    Record record = readRecord(raf, table);
                    recordCount++;
                    if (!record.isDeleted()) {
                        records.add(record);
                    } else {
                        deletedCount++;
                    }
                } catch (EOFException e) {
                    System.out.println("readAllRecords: 遇到文件结束，当前位置: " + raf.getFilePointer() + ", 文件长度: " + fileLength);
                    break;
                } catch (Exception e) {
                    errorCount++;
                    System.err.println("readAllRecords: 读取记录失败，位置: " + raf.getFilePointer() + ", 错误: " + e.getMessage());
                    e.printStackTrace();
                    // 如果读取失败，尝试跳过当前记录（但这可能导致后续记录也读错）
                    // 为了安全，我们停止读取
                    break;
                }
            }
            
            System.out.println("readAllRecords: 总共读取 " + recordCount + " 条记录, 有效记录: " + records.size() + 
                ", 已删除: " + deletedCount + ", 错误: " + errorCount);
        }
        
        return records;
    }
    
    /**
     * 逻辑删除记录（标记为已删除）
     */
    public static void deleteRecord(String filePath, long position) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(filePath, "rw")) {
            raf.seek(position);
            raf.writeInt(FileFormat.RECORD_DELETED);
        }
    }
    
    /**
     * RandomAccessFile的InputStream适配器
     */
    private static class RAFInputStream extends InputStream {
        private final RandomAccessFile raf;
        
        public RAFInputStream(RandomAccessFile raf) {
            this.raf = raf;
        }
        
        @Override
        public int read() throws IOException {
            return raf.read();
        }
        
        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            return raf.read(b, off, len);
        }
    }
    
    /**
     * RandomAccessFile的OutputStream适配器
     */
    private static class RAFOutputStream extends OutputStream {
        private final RandomAccessFile raf;
        
        public RAFOutputStream(RandomAccessFile raf) {
            this.raf = raf;
        }
        
        @Override
        public void write(int b) throws IOException {
            raf.write(b);
        }
        
        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            raf.write(b, off, len);
        }
    }
}

