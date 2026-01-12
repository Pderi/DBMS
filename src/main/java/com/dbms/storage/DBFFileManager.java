package com.dbms.storage;

import com.dbms.model.Database;
import com.dbms.model.Table;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * .dbf文件管理器 - 负责表结构的存储和读取
 */
public class DBFFileManager {
    
    /**
     * 创建新的数据库文件
     */
    public static void createDatabaseFile(String filePath, Database database) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(filePath, "rw")) {
            // 写入文件头
            writeHeader(raf, database);
            
            // 写入表结构
            long currentOffset = FileFormat.DBF_HEADER_SIZE;
            List<TableIndexEntry> indexEntries = new ArrayList<>();
            
            for (Table table : database.getTables().values()) {
                long tableOffset = currentOffset;
                writeTable(raf, table, tableOffset);
                
                // 记录索引
                indexEntries.add(new TableIndexEntry(table.getName(), tableOffset));
                
                // 计算下一个表的偏移量
                currentOffset = raf.getFilePointer();
            }
            
            // 更新文件头中的表索引
            updateTableIndex(raf, indexEntries);
        }
    }
    
    /**
     * 读取数据库文件
     */
    public static Database readDatabaseFile(String filePath) throws IOException {
        Database database = new Database();
        database.setDbFilePath(filePath);
        
        try (RandomAccessFile raf = new RandomAccessFile(filePath, "r")) {
            // 读取文件头
            readHeader(raf);
            
            // 读取表索引
            List<TableIndexEntry> indexEntries = readTableIndex(raf);
            
            // 读取每个表的结构
            for (TableIndexEntry entry : indexEntries) {
                raf.seek(entry.offset);
                Table table = readTable(raf);
                database.addTable(table);
            }
        }
        
        return database;
    }
    
    /**
     * 写入文件头
     */
    private static void writeHeader(RandomAccessFile raf, Database database) throws IOException {
        raf.seek(0);
        raf.writeInt(FileFormat.DBF_MAGIC_NUMBER);  // 魔数
        raf.writeInt(FileFormat.FILE_VERSION);       // 版本号
        raf.writeInt(database.getTableCount());      // 表数量
        // 预留空间
        raf.seek(FileFormat.DBF_HEADER_SIZE);
    }
    
    /**
     * 读取文件头
     */
    private static void readHeader(RandomAccessFile raf) throws IOException {
        raf.seek(0);
        int magic = raf.readInt();
        if (magic != FileFormat.DBF_MAGIC_NUMBER) {
            throw new IOException("Invalid database file format");
        }
        int version = raf.readInt();
        if (version != FileFormat.FILE_VERSION) {
            throw new IOException("Unsupported file version: " + version);
        }
    }
    
    /**
     * 更新表索引（写入文件头后的索引区）
     */
    private static void updateTableIndex(RandomAccessFile raf, List<TableIndexEntry> entries) throws IOException {
        raf.seek(12); // 跳过魔数(4) + 版本号(4) + 表数量(4)
        
        for (TableIndexEntry entry : entries) {
            // 写入表名长度和表名
            byte[] nameBytes = entry.tableName.getBytes("UTF-8");
            raf.writeInt(nameBytes.length);
            raf.write(nameBytes);
            // 写入偏移量
            raf.writeLong(entry.offset);
        }
    }
    
    /**
     * 读取表索引
     */
    private static List<TableIndexEntry> readTableIndex(RandomAccessFile raf) throws IOException {
        raf.seek(0);
        raf.readInt(); // 跳过魔数
        raf.readInt(); // 跳过版本号
        int tableCount = raf.readInt();
        
        List<TableIndexEntry> entries = new ArrayList<>();
        for (int i = 0; i < tableCount; i++) {
            int nameLength = raf.readInt();
            byte[] nameBytes = new byte[nameLength];
            raf.readFully(nameBytes);
            String tableName = new String(nameBytes, "UTF-8");
            long offset = raf.readLong();
            entries.add(new TableIndexEntry(tableName, offset));
        }
        
        return entries;
    }
    
    /**
     * 写入表结构
     */
    private static void writeTable(RandomAccessFile raf, Table table, long offset) throws IOException {
        raf.seek(offset);
        
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(baos)) {
            
            BinarySerializer.writeTable(dos, table);
            dos.flush();
            byte[] data = baos.toByteArray();
            raf.write(data);
        }
    }
    
    /**
     * 读取表结构
     */
    private static Table readTable(RandomAccessFile raf) throws IOException {
        try (DataInputStream dis = new DataInputStream(
                new BufferedInputStream(new RAFInputStream(raf)))) {
            return BinarySerializer.readTable(dis);
        }
    }
    
    /**
     * 添加新表到数据库文件
     */
    public static void addTableToFile(String filePath, Table table) throws IOException {
        Database database = readDatabaseFile(filePath);
        database.addTable(table);
        createDatabaseFile(filePath, database); // 重新写入整个文件
    }
    
    /**
     * 从数据库文件删除表
     */
    public static void removeTableFromFile(String filePath, String tableName) throws IOException {
        Database database = readDatabaseFile(filePath);
        database.removeTable(tableName);
        createDatabaseFile(filePath, database);
    }
    
    /**
     * 更新数据库文件中的表结构
     */
    public static void updateTableInFile(String filePath, Table table) throws IOException {
        Database database = readDatabaseFile(filePath);
        database.removeTable(table.getName());
        database.addTable(table);
        createDatabaseFile(filePath, database);
    }
    
    /**
     * 表索引项内部类
     */
    private static class TableIndexEntry {
        String tableName;
        long offset;
        
        TableIndexEntry(String tableName, long offset) {
            this.tableName = tableName;
            this.offset = offset;
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
}

