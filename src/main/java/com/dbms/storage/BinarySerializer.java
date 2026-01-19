package com.dbms.storage;

import com.dbms.model.Field;
import com.dbms.model.FieldType;
import com.dbms.model.Index;
import com.dbms.model.Table;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 二进制序列化工具类
 */
public class BinarySerializer {
    
    /**
     * 写入整数（4字节）
     */
    public static void writeInt(DataOutputStream dos, int value) throws IOException {
        dos.writeInt(value);
    }
    
    /**
     * 读取整数（4字节）
     */
    public static int readInt(DataInputStream dis) throws IOException {
        return dis.readInt();
    }
    
    /**
     * 写入长整数（8字节）
     */
    public static void writeLong(DataOutputStream dos, long value) throws IOException {
        dos.writeLong(value);
    }
    
    /**
     * 读取长整数（8字节）
     */
    public static long readLong(DataInputStream dis) throws IOException {
        return dis.readLong();
    }
    
    /**
     * 写入字符串（UTF-8编码，先写长度再写内容）
     */
    public static void writeString(DataOutputStream dos, String str) throws IOException {
        if (str == null) {
            writeInt(dos, 0);
            return;
        }
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        writeInt(dos, bytes.length);
        dos.write(bytes);
    }
    
    /**
     * 读取字符串
     */
    public static String readString(DataInputStream dis) throws IOException {
        int length = readInt(dis);
        if (length == 0) {
            return null;
        }
        byte[] bytes = new byte[length];
        dis.readFully(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }
    
    /**
     * 写入字段类型
     */
    public static void writeFieldType(DataOutputStream dos, FieldType type) throws IOException {
        dos.writeByte(type.ordinal());
    }
    
    /**
     * 读取字段类型
     */
    public static FieldType readFieldType(DataInputStream dis) throws IOException {
        int ordinal = dis.readByte() & 0xFF;
        return FieldType.values()[ordinal];
    }
    
    /**
     * 写入字段定义
     */
    public static void writeField(DataOutputStream dos, Field field) throws IOException {
        writeString(dos, field.getName());
        writeFieldType(dos, field.getType());
        writeInt(dos, field.getLength());
        dos.writeBoolean(field.isKey());
        dos.writeBoolean(field.isNullable());
        writeString(dos, field.getDefaultValue()); // 预留字段
    }
    
    /**
     * 读取字段定义
     */
    public static Field readField(DataInputStream dis) throws IOException {
        Field field = new Field();
        field.setName(readString(dis));
        field.setType(readFieldType(dis));
        field.setLength(readInt(dis));
        field.setKey(dis.readBoolean());
        field.setNullable(dis.readBoolean());
        field.setDefaultValue(readString(dis));
        return field;
    }
    
    /**
     * 写入表结构
     */
    public static void writeTable(DataOutputStream dos, Table table) throws IOException {
        writeString(dos, table.getName());
        writeInt(dos, table.getFieldCount());
        for (Field field : table.getFields()) {
            writeField(dos, field);
        }
        writeInt(dos, table.getRecordCount());
        writeLong(dos, table.getLastModified());
        
        // 写入索引信息
        if (table.getIndexes() != null) {
            writeInt(dos, table.getIndexes().size());
            for (Index index : table.getIndexes().values()) {
                writeIndex(dos, index);
            }
        } else {
            writeInt(dos, 0);
        }
    }
    
    /**
     * 读取表结构
     */
    public static Table readTable(DataInputStream dis) throws IOException {
        Table table = new Table();
        table.setName(readString(dis));
        int fieldCount = readInt(dis);
        List<Field> fields = new ArrayList<>();
        for (int i = 0; i < fieldCount; i++) {
            fields.add(readField(dis));
        }
        table.setFields(fields);
        table.setRecordCount(readInt(dis));
        table.setLastModified(readLong(dis));
        
        // 读取索引信息（向后兼容：旧文件可能没有索引信息）
        try {
            int indexCount = readInt(dis);
            for (int i = 0; i < indexCount; i++) {
                Index index = readIndex(dis);
                table.addIndex(index);
            }
        } catch (java.io.EOFException e) {
            // 旧文件格式没有索引信息，跳过
            // 索引列表初始化为空即可
        }
        
        return table;
    }
    
    /**
     * 写入索引信息
     */
    public static void writeIndex(DataOutputStream dos, Index index) throws IOException {
        writeString(dos, index.getIndexName());
        writeString(dos, index.getTableName());
        writeString(dos, index.getColumnName());
        dos.writeBoolean(index.isUnique());
        // 注意：indexMap 不需要持久化，因为可以在需要时重新构建
    }
    
    /**
     * 读取索引信息
     */
    public static Index readIndex(DataInputStream dis) throws IOException {
        String indexName = readString(dis);
        String tableName = readString(dis);
        String columnName = readString(dis);
        boolean unique = dis.readBoolean();
        return new Index(indexName, tableName, columnName, unique);
    }
    
    /**
     * 写入固定长度的字符串（用于CHAR类型）
     */
    public static void writeFixedString(DataOutputStream dos, String str, int length) throws IOException {
        byte[] bytes = new byte[length];
        if (str != null) {
            byte[] strBytes = str.getBytes(StandardCharsets.UTF_8);
            int copyLength = Math.min(strBytes.length, length);
            System.arraycopy(strBytes, 0, bytes, 0, copyLength);
        }
        dos.write(bytes);
    }
    
    /**
     * 读取固定长度的字符串
     */
    public static String readFixedString(DataInputStream dis, int length) throws IOException {
        byte[] bytes = new byte[length];
        dis.readFully(bytes);
        // 找到第一个null字节的位置
        int nullIndex = length;
        for (int i = 0; i < length; i++) {
            if (bytes[i] == 0) {
                nullIndex = i;
                break;
            }
        }
        if (nullIndex == 0) {
            return null;
        }
        return new String(bytes, 0, nullIndex, StandardCharsets.UTF_8);
    }
}

