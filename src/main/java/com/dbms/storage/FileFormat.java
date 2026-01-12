package com.dbms.storage;

/**
 * 文件格式常量定义
 */
public class FileFormat {
    // .dbf文件魔数（标识文件类型）
    public static final int DBF_MAGIC_NUMBER = 0x44424D53; // "DBMS"
    
    // 文件版本号
    public static final int FILE_VERSION = 1;
    
    // 文件头大小（字节）
    public static final int DBF_HEADER_SIZE = 512;
    
    // 表索引项大小（表名长度4字节 + 偏移量8字节）
    public static final int TABLE_INDEX_ENTRY_SIZE = 12;
    
    // 最大表数量（预留）
    public static final int MAX_TABLE_COUNT = 100;
    
    // 记录状态标志
    public static final int RECORD_ACTIVE = 0;
    public static final int RECORD_DELETED = 1;
}

