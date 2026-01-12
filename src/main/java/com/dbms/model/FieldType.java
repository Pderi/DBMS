package com.dbms.model;

/**
 * 字段类型枚举
 */
public enum FieldType {
    INT(4, "INT"),
    VARCHAR(-1, "VARCHAR"),  // -1表示变长
    CHAR(1, "CHAR"),
    DATE(8, "DATE"),
    FLOAT(8, "FLOAT");
    
    private final int defaultLength;
    private final String sqlName;
    
    FieldType(int defaultLength, String sqlName) {
        this.defaultLength = defaultLength;
        this.sqlName = sqlName;
    }
    
    public int getDefaultLength() {
        return defaultLength;
    }
    
    public String getSqlName() {
        return sqlName;
    }
    
    public static FieldType fromString(String typeName) {
        typeName = typeName.toUpperCase().trim();
        for (FieldType type : values()) {
            if (type.sqlName.equals(typeName) || type.name().equals(typeName)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown field type: " + typeName);
    }
}

