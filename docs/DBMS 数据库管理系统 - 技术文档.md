# DBMS 数据库管理系统 - 技术文档

## 目录

1. [系统概述](#系统概述)
2. [整体架构](#整体架构)
3. [核心模块详解](#核心模块详解)
   - [数据模型层](#数据模型层)
   - [存储层](#存储层)
   - [SQL解析层](#sql解析层)
   - [执行引擎层](#执行引擎层)
   - [用户界面层](#用户界面层)
4. [存储与文件格式](#存储层)
5. [SQL语法与执行流程](#sql语法支持)
6. [实现细节与关键算法](#实现细节)
7. [代码示例](#代码示例)
8. [性能分析与设计决策](#性能分析)
9. [使用示例与故障排查](#使用示例)
10. [性能优化与安全性](#性能优化建议)
11. [扩展功能与详细实现](#扩展功能实现指南)
12. [更多实际使用场景](#更多实际使用场景)
13. [与其他数据库系统对比](#与其他数据库系统对比)
14. [总结与选择建议](#总结对比)

---

## 系统概述

本系统是一个基于Java实现的轻量级数据库管理系统（DBMS），采用自定义文件格式存储表结构和数据，支持基本的SQL操作。

### 主要特性

- **自定义文件格式**：使用`.dbf`文件存储表结构，`.dat`文件存储数据记录
- **SQL支持**：支持DDL（数据定义语言）、DML（数据操纵语言）、DCL（数据控制语言）和事务控制
- **多表管理**：在一个数据库文件中管理多张表
- **图形界面**：基于Swing的现代化用户界面
- **用户权限管理**：支持用户创建、删除和权限分配
- **数据备份恢复**：支持数据库备份和恢复功能
- **事务支持**：支持事务的开启、提交和回滚

### 技术栈

- **开发语言**：Java 8+
- **GUI框架**：Java Swing
- **文件操作**：RandomAccessFile、NIO
- **设计模式**：分层架构、策略模式

---

## 整体架构

系统采用分层架构设计，各层职责清晰，便于维护和扩展。

```
┌─────────────────────────────────────────┐
│         用户界面层 (UI Layer)            │
│      MainFrame, 各种Panel组件           │
└─────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────┐
│        SQL执行层 (SQL Executor)         │
│      SQLExecutor - 统一入口              │
└─────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────┐
│       执行引擎层 (Engine Layer)          │
│  DDLExecutor | DMLExecutor | QueryExecutor│
└─────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────┐
│        SQL解析层 (Parser Layer)          │
│      SQLLexer | SQLParser                │
└─────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────┐
│        存储层 (Storage Layer)            │
│  DBFFileManager | DATFileManager         │
└─────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────┐
│        数据模型层 (Model Layer)           │
│  Database | Table | Field | Record       │
└─────────────────────────────────────────┘
```

### 数据流向

1. **用户输入SQL** → UI层接收
2. **SQL解析** → Parser层进行词法和语法分析
3. **生成执行计划** → Executor层根据SQL类型选择执行器
4. **执行操作** → Engine层执行具体的数据库操作
5. **文件读写** → Storage层进行二进制文件操作
6. **返回结果** → 逐层返回，最终在UI层显示

---

## 核心模块详解

### 数据模型层

数据模型层定义了系统的核心数据结构，是其他所有层的基础。

#### 1. FieldType（字段类型枚举）

```java
public enum FieldType {
    INT(4, "INT"),           // 整数类型，4字节
    VARCHAR(-1, "VARCHAR"),  // 变长字符串，-1表示变长
    CHAR(1, "CHAR"),         // 定长字符串
    DATE(8, "DATE"),         // 日期类型，8字节
    FLOAT(8, "FLOAT"),       // 浮点数，8字节
    DOUBLE(8, "DOUBLE");     // 双精度浮点数，8字节
}
```

**设计原理**：
- 使用枚举类型确保类型安全
- 每个类型包含默认长度和SQL名称
- 支持从字符串转换为枚举类型

#### 2. Field（字段定义）

字段定义包含字段的所有元数据信息。

**核心属性**：
- `name`: 字段名
- `type`: 字段类型（FieldType枚举）
- `length`: 字段长度
- `isKey`: 是否为主键
- `nullable`: 是否允许为NULL
- `defaultValue`: 默认值（预留）

**关键方法**：
- `getStorageSize()`: 计算字段在文件中的存储大小
  - INT: 固定4字节
  - FLOAT: 固定8字节
  - DOUBLE: 固定8字节
  - VARCHAR: 4字节长度前缀 + 实际数据
  - CHAR: 固定长度
  - DATE: 变长字符串存储

#### 3. Table（表结构）

表结构管理一张表的所有字段定义。

**核心功能**：
- 字段管理：添加、删除、查找字段
- 主键管理：获取所有主键字段
- 记录大小计算：根据字段定义计算每条记录的大小

**设计要点**：
- 使用`List<Field>`存储字段，保持字段顺序
- 提供按名称和索引查找字段的方法
- 支持字段验证（如检查字段名是否重复）

#### 4. Record（记录）

记录表示表中的一行数据。

**数据结构**：
- `values`: `List<Object>` - 存储各字段的值
- `deleted`: boolean - 逻辑删除标志
- `recordId`: long - 记录ID（预留）

**设计原理**：
- 使用逻辑删除而非物理删除，便于数据恢复
- 值列表与字段列表一一对应
- 支持根据字段名或索引访问值

#### 5. Database（数据库）

数据库类管理整个数据库，包含多张表。

**核心功能**：
- 表管理：添加、删除、查找表
- 文件路径管理：管理.dbf和.dat文件路径
- 表名映射：使用`Map<String, Table>`快速查找表

---

## 存储层

本节从“实现视角”介绍系统的持久化方案，即如何把内存中的表结构与记录转换成二进制形式写入磁盘。  
在这里会给出一个**概要版**的说明，后续的「存储与文件格式」章节会在此基础上展开更多细节。

### 文件格式设计

#### .dbf文件（表结构文件）

**文件结构**：

```
┌─────────────────────────────────────┐
│         文件头 (512字节)             │
├─────────────────────────────────────┤
│  魔数 (4字节): 0x44424D53 ("DBMS")  │
│  版本号 (4字节): 1                  │
│  表数量 (4字节): N                  │
│  表索引表 (变长)                    │
│    - 表1: 表名长度(4) + 表名 + 偏移量(8)│
│    - 表2: ...                       │
│  预留空间 (剩余字节)                 │
└─────────────────────────────────────┘
┌─────────────────────────────────────┐
│         表结构区 (变长)               │
├─────────────────────────────────────┤
│  表1结构:                            │
│    - 表名长度(4) + 表名(UTF-8)       │
│    - 字段数量(4)                     │
│    - 字段数组:                       │
│      * 字段名长度(4) + 字段名        │
│      * 字段类型(1字节枚举)           │
│      * 字段长度(4)                   │
│      * 是否主键(1字节布尔)           │
│      * 是否可空(1字节布尔)           │
│      * 默认值(变长字符串)            │
│    - 记录数量(4)                     │
│    - 最后修改时间(8)                 │
│  表2结构: ...                       │
└─────────────────────────────────────┘
```

**设计原理**：
- **固定文件头**：便于快速读取元数据
- **表索引表**：在文件头存储每张表的偏移量，实现O(1)查找
- **变长存储**：表结构使用变长格式，节省空间
- **UTF-8编码**：所有字符串使用UTF-8编码，支持中文

#### .dat文件（数据文件）

**记录存储格式**：

```
每条记录的结构：
┌─────────────────────────────────────┐
│  记录状态 (4字节)                    │
│    0 = 有效记录                      │
│    1 = 已删除记录（逻辑删除）         │
├─────────────────────────────────────┤
│  字段1值 (根据字段类型)              │
│    INT: 4字节整数                    │
│    FLOAT: 8字节浮点数                │
│    CHAR: 固定长度字符串              │
│    VARCHAR: 长度(4) + 数据(变长)     │
│    DATE: 长度(4) + 数据(变长)         │
├─────────────────────────────────────┤
│  字段2值 ...                        │
│  ...                                │
└─────────────────────────────────────┘
```

**设计原理**：
- **逻辑删除**：使用状态标志而非物理删除，便于恢复
- **变长字段处理**：VARCHAR和DATE使用长度前缀
- **顺序存储**：记录按插入顺序存储，便于追加
- **NULL值处理**：根据字段类型写入默认值（0、空字符串等）

### BinarySerializer（二进制序列化工具）

负责Java对象与二进制数据之间的转换。

**核心方法**：
- `writeInt/readInt`: 整数序列化（4字节，大端序）
- `writeLong/readLong`: 长整数序列化（8字节）
- `writeString/readString`: 字符串序列化（UTF-8，长度前缀）
- `writeField/readField`: 字段定义序列化
- `writeTable/readTable`: 表结构序列化

**设计要点**：
- 使用大端序（网络字节序），保证跨平台兼容性
- 所有变长数据使用长度前缀
- 字符串统一使用UTF-8编码

### DBFFileManager（表结构文件管理器）

**核心功能**：
- `createDatabaseFile()`: 创建新的数据库文件
- `readDatabaseFile()`: 读取数据库文件
- `addTableToFile()`: 添加新表
- `removeTableFromFile()`: 删除表
- `updateTableInFile()`: 更新表结构

**实现原理**：
- 使用`RandomAccessFile`进行随机访问
- 先读取文件头获取表索引
- 根据索引定位到表结构位置
- 更新操作需要重写整个文件（简化实现）

### DATFileManager（数据文件管理器）

**核心功能**：
- `writeRecord()`: 写入记录到文件
- `readRecord()`: 从文件读取记录
- `appendRecord()`: 追加记录到文件末尾
- `readAllRecords()`: 读取所有有效记录
- `deleteRecord()`: 逻辑删除记录

**实现原理**：
- 使用`RandomAccessFile`进行随机访问
- 记录按顺序存储，通过偏移量定位
- 读取时跳过已删除的记录
- 支持在指定位置更新记录

---

## SQL解析层

在了解了数据如何落盘之后，下一步是回答：“SQL 文本是如何被理解的？”  
本节围绕词法分析（Lexer）和语法分析（Parser），说明从字符串到抽象语法树（AST）的转换过程，是整个系统的**前端**。

### 词法分析（SQLLexer）

词法分析器将SQL文本分解为Token序列。

**Token类型**：
- `KEYWORD`: SQL关键字（SELECT, FROM, WHERE等）
- `IDENTIFIER`: 标识符（表名、字段名）
- `STRING`: 字符串字面量
- `NUMBER`: 数字字面量
- `OPERATOR`: 操作符（=, <, >, *, +等）
- `PUNCTUATION`: 标点符号（(, ), ,, ;, .）
- `EOF`: 文件结束

**实现原理**：
- 使用状态机逐个字符扫描
- 识别关键字、标识符、字符串、数字等
- 处理转义字符和字符串边界
- 支持单行注释（--）

**关键算法**：
```java
while (pos < sql.length()) {
    char ch = sql.charAt(pos);
    if (Character.isWhitespace(ch)) {
        // 跳过空白字符
    } else if (ch == '\'' || ch == '"') {
        // 处理字符串字面量
    } else if (Character.isDigit(ch)) {
        // 处理数字
    } else if (isOperator(ch)) {
        // 处理操作符
    } else if (Character.isLetter(ch)) {
        // 处理标识符或关键字
    }
}
```

### 语法分析（SQLParser）

语法分析器将Token序列转换为抽象语法树（AST）。

**支持的SQL语句类型**：
- **DDL（数据定义语言）**：
  - `CREATE TABLE`: 创建表
  - `CREATE INDEX`: 创建索引
  - `CREATE UNIQUE INDEX`: 创建唯一索引
  - `ALTER TABLE`: 修改表结构
  - `DROP TABLE`: 删除表
  - `DROP USER`: 删除用户
  - `RENAME TABLE`: 重命名表
- **DML（数据操纵语言）**：
  - `INSERT`: 插入数据
  - `UPDATE`: 更新数据
  - `DELETE`: 删除数据
  - `SELECT`: 查询数据
- **DCL（数据控制语言）**：
  - `CREATE USER`: 创建用户
  - `DROP USER`: 删除用户
  - `GRANT`: 分配权限
  - `REVOKE`: 回收权限
- **事务控制**：
  - `BEGIN TRANSACTION`: 开始事务
  - `COMMIT`: 提交事务
  - `ROLLBACK`: 回滚事务

**解析方法**：
- 递归下降解析器
- 每个SQL语句类型对应一个解析方法
- 使用`expectToken()`、`peekToken()`等辅助方法

**解析示例 - CREATE TABLE**：
```java
private CreateTableStatement parseCreateTable() {
    expectKeyword("TABLE");
    CreateTableStatement stmt = new CreateTableStatement();
    stmt.tableName = expectIdentifier();
    expectPunctuation("(");
    
    while (!peekPunctuation(")")) {
        FieldDefinition fieldDef = parseFieldDefinition();
        stmt.fields.add(fieldDef);
        if (peekPunctuation(",")) {
            consume();
        }
    }
    expectPunctuation(")");
    return stmt;
}
```

**错误处理**：
- 使用`expectToken()`方法，期望的Token不匹配时抛出异常
- 提供详细的错误信息（位置、期望类型、实际类型）

---

## 执行引擎层

解析只是“看懂 SQL”，真正让数据发生变化的是执行引擎。  
本节从 DDL / DML / 查询三条路线，介绍解析后的语法树是如何被翻译成对文件的实际操作的。

### DDLExecutor（数据定义语言执行器）

负责表结构的创建、修改和删除。

**核心方法**：
- `createTable()`: 创建表
  - 验证表名和字段名合法性
  - 检查表是否已存在
  - 创建表结构并保存到文件
  
- `createIndex()`: 创建索引
  - 验证索引名、表名和列名
  - 检查索引是否已存在
  - 创建索引并保存到表结构
  
- `dropTable()`: 删除表
  - 从数据库中移除表
  - 更新数据库文件
  
- `alterTable()`: 修改表结构
  - `ADD COLUMN`: 添加字段
  - `DROP COLUMN`: 删除字段
  - `MODIFY COLUMN`: 修改字段类型
  - `RENAME COLUMN`: 重命名字段

**实现原理**：
- 所有DDL操作都会更新.dbf文件
- 使用`DBFFileManager`进行文件操作
- 操作前进行验证（表是否存在、字段是否重复等）

### DMLExecutor（数据操纵语言执行器）

负责数据的插入、更新和删除。

**核心方法**：
- `insert()`: 插入记录
  - 验证记录是否符合表结构
  - 类型转换和验证
  - 追加记录到.dat文件
  
- `update()`: 更新记录
  - 读取所有记录
  - 根据WHERE条件过滤
  - 更新匹配的记录
  - 写回文件
  
- `delete()`: 删除记录
  - 逻辑删除（标记为已删除）
  - 支持WHERE条件过滤

**WHERE条件处理**：
- 支持操作符：=, !=, <, >, <=, >=, LIKE
- 支持NULL值检查
- 类型比较和转换

### QueryExecutor（查询执行器）

负责SELECT查询的执行。

**核心方法**：
- `select()`: 单表查询
  - 读取所有记录
  - WHERE条件过滤
  - 字段投影（选择指定列）
  - ORDER BY排序（支持多列、ASC/DESC）
  - 返回查询结果
  
- `join()`: 多表连接
  - 嵌套循环连接（Nested Loop Join）
  - 支持等值连接
  - 支持WHERE条件
  - 支持ORDER BY排序

- `executeSubquery()`: 执行嵌套查询
  - 支持IN (SELECT ...)子查询
  - 递归执行子查询
  - 返回子查询结果集

**查询结果**：
- `QueryResult`类包含列名和数据
- 数据以`List<List<Object>>`形式存储
- 支持在UI中显示

### SQLExecutor（SQL执行器）

统一入口，根据SQL语句类型分发到对应的执行器。

**执行流程**：
1. 解析SQL语句
2. 根据语句类型选择执行器
3. 执行操作
4. 返回结果

**新增功能**：
- **用户管理**：通过`UserManager`管理用户账户和权限
- **事务管理**：通过`TransactionManager`管理事务的开启、提交和回滚
- **DCL执行**：支持GRANT和REVOKE语句执行
- **用户创建/删除**：支持CREATE USER和DROP USER语句执行

### UserManager（用户管理器）

负责用户账户和权限的管理。

**核心功能**：
- `createUser()`: 创建新用户
- `deleteUser()`: 删除用户
- `login()`: 用户登录验证
- `grantPermission()`: 分配权限
- `revokePermission()`: 回收权限
- `getAllUsers()`: 获取所有用户列表

**权限管理**：
- 支持权限：SELECT, INSERT, UPDATE, DELETE, CREATE_TABLE, ALL等
- 权限存储在`users.dat`文件中
- 默认创建admin用户，拥有ALL权限

**实现原理**：
- 使用`HashMap<String, User>`存储用户
- 用户数据持久化到`users.dat`文件
- 使用Java序列化保存用户对象

### TransactionManager（事务管理器）

负责事务的管理和控制。

**核心功能**：
- `beginTransaction()`: 开始事务
- `commit()`: 提交事务
- `rollback()`: 回滚事务
- `getCurrentTransaction()`: 获取当前事务

**实现原理**：
- 使用`ThreadLocal`存储当前事务
- 记录事务中的操作（INSERT、UPDATE、DELETE）
- 回滚时撤销所有操作（简化实现）

**注意事项**：
- 当前为简化实现，主要用于演示
- 完整的事务需要WAL日志和MVCC机制

### BackupManager（备份管理器）

负责数据库的备份和恢复。

**核心功能**：
- `backupDatabase()`: 备份数据库
  - 备份所有.dbf文件
  - 备份所有.dat文件
  - 创建带时间戳的备份目录
  
- `restoreDatabase()`: 恢复数据库
  - 从备份目录恢复.dbf文件
  - 从备份目录恢复.dat文件
  - 覆盖当前数据库文件

**实现原理**：
- 使用`Files.copy()`复制文件
- 备份目录命名格式：`db_backup_yyyyMMdd_HHmmss`
- 支持选择备份位置和恢复源

---

## 用户界面层

有了“能执行 SQL 的内核”之后，还需要一个**对人友好**的交互界面。  
本节介绍基于 Swing 的主界面如何组织布局，以及它与 SQL 执行层之间的调用关系。

### 界面布局

**三栏布局**：
- **左侧**：数据库表列表
- **中间**：SQL编辑器和执行结果
- **右侧**：表结构和表数据

**组件设计**：
- 使用`JSplitPane`实现可调整大小的面板
- 每个功能区域独立的面板
- 统一的标题栏样式（蓝色背景，白色文字）

### 核心组件

**MainFrame（主窗口）**：
- 管理所有UI组件
- 协调各模块的交互
- 处理用户操作事件

**SQL编辑器**：
- `JTextArea`实现多行文本编辑
- 支持语法高亮（可扩展）
- F5快捷键执行SQL

**结果展示**：
- `JTextArea`显示文本结果
- `JTable`显示表格数据
- 支持滚动查看

**用户管理界面**：
- 用户列表对话框
- 显示所有用户及其权限
- 支持刷新用户列表

**备份恢复功能**：
- 通过菜单栏"文件"->"备份数据库"进行备份
- 通过菜单栏"文件"->"恢复数据库"进行恢复
- 备份包含所有.dbf和.dat文件

### 事件处理

**SQL执行流程**：
1. 用户点击执行按钮或按F5
2. 获取SQL编辑器内容
3. 过滤注释行
4. 按分号分割多条语句
5. 依次执行每条语句
6. 显示执行结果
7. 刷新表格显示

**表选择事件**：
- 选择表时自动显示表结构
- 自动加载表数据
- 更新右侧面板

---

## 存储与文件格式（详细展开）

在前面的「存储层」中，我们从宏观角度介绍了持久化思路。  
本节是对该部分的**深入展开**：详细描述 `.dbf` / `.dat` 文件的二进制布局，方便调试、扩展或在课上讲解。

### .dbf文件格式详解

**文件头结构（512字节）**：

| 偏移量 | 大小 | 说明 |
|--------|------|------|
| 0x00 | 4字节 | 魔数：0x44424D53 ("DBMS") |
| 0x04 | 4字节 | 版本号：1 |
| 0x08 | 4字节 | 表数量：N |
| 0x0C | 变长 | 表索引表 |
| ... | ... | 预留空间（填充到512字节） |

**表索引表结构**：
```
对于每张表：
- 表名长度（4字节）
- 表名（UTF-8编码，变长）
- 表结构偏移量（8字节，从文件开始的位置）
```

**表结构块结构**：
```
- 表名长度（4字节）
- 表名（UTF-8编码，变长）
- 字段数量（4字节）
- 字段数组：
  - 字段名长度（4字节）
  - 字段名（UTF-8编码，变长）
  - 字段类型（1字节，FieldType枚举的ordinal值）
  - 字段长度（4字节）
  - 是否主键（1字节，0=false, 1=true）
  - 是否可空（1字节，0=false, 1=true）
  - 默认值长度（4字节）
  - 默认值（UTF-8编码，变长，可为空）
- 记录数量（4字节，预留）
- 最后修改时间（8字节，时间戳）
```

### .dat文件格式详解

**记录存储格式**：

每条记录由以下部分组成：

1. **记录头（4字节）**：
   - 0x00000000：有效记录
   - 0x00000001：已删除记录

2. **字段值（变长）**：
   - **INT类型**：4字节，大端序整数
   - **FLOAT类型**：8字节，IEEE 754双精度浮点数
   - **CHAR类型**：固定长度字节数组，UTF-8编码，不足部分填充0
   - **VARCHAR类型**：
     - 长度前缀（4字节）：实际数据长度
     - 数据内容（变长）：UTF-8编码的字符串
     - 如果长度为0，表示NULL值
   - **DATE类型**：与VARCHAR相同，存储为字符串

**记录定位**：
- 记录按插入顺序连续存储
- 通过计算偏移量定位：`offset = recordIndex * recordSize`
- 读取时需要跳过已删除的记录

---

## SQL语法支持

在理解了解析器与执行引擎的实现之后，本节从“系统能懂哪些 SQL 语句”的角度，总结当前支持的语法子集，并通过简洁示例进行说明。

### 支持的SQL语句

#### DDL语句

**CREATE TABLE**：
```sql
CREATE TABLE table_name (
    column_name data_type(length) [PRIMARY KEY] [NOT NULL],
    ...
);
```

**ALTER TABLE**：
```sql
ALTER TABLE table_name ADD COLUMN column_name data_type(length);
ALTER TABLE table_name DROP COLUMN column_name;
ALTER TABLE table_name MODIFY COLUMN column_name new_type(length);
ALTER TABLE table_name RENAME COLUMN old_name TO new_name;
```

**DROP TABLE**：
```sql
DROP TABLE table_name;
```

**RENAME TABLE**：
```sql
RENAME TABLE old_name TO new_name;
```

**CREATE INDEX**：
```sql
CREATE INDEX index_name ON table_name(column_name);
CREATE UNIQUE INDEX index_name ON table_name(column_name);
```

#### DCL语句

**CREATE USER**：
```sql
CREATE USER username IDENTIFIED BY 'password';
```

**DROP USER**：
```sql
DROP USER username;
```

**GRANT**：
```sql
GRANT permission1, permission2 TO username;
GRANT ALL TO username;
```

**REVOKE**：
```sql
REVOKE permission FROM username;
REVOKE permission1, permission2 FROM username;
```

#### 事务控制语句

**BEGIN TRANSACTION**：
```sql
BEGIN TRANSACTION;
```

**COMMIT**：
```sql
COMMIT;
```

**ROLLBACK**：
```sql
ROLLBACK;
```

#### DML语句

**INSERT**：
```sql
INSERT INTO table_name VALUES (value1, value2, ...);
INSERT INTO table_name (col1, col2) VALUES (val1, val2);
```

**UPDATE**：
```sql
UPDATE table_name SET col1=val1, col2=val2 [WHERE condition];
UPDATE table_name SET col1=col1+1, col2=col2*2 [WHERE condition];  -- 支持表达式
```

**DELETE**：
```sql
DELETE FROM table_name [WHERE condition];
```

**SELECT**：
```sql
SELECT * FROM table_name [WHERE condition];
SELECT col1, col2 FROM table_name [WHERE condition];
SELECT col1 AS alias_name FROM table_name;  -- 支持列别名
SELECT * FROM table1 JOIN table2 ON table1.id = table2.id [WHERE condition];
SELECT COUNT(*) FROM table_name [GROUP BY col1];  -- 支持聚合函数和分组
SELECT col1, COUNT(*) as count FROM table_name GROUP BY col1;  -- 聚合函数与分组
SELECT * FROM table_name ORDER BY col1 ASC, col2 DESC;  -- 支持排序
SELECT * FROM table_name WHERE col1 IN (SELECT col2 FROM table2);  -- 支持嵌套查询
```

### WHERE条件支持

**比较操作符**：
- `=`: 等于
- `!=` 或 `<>`: 不等于
- `<`: 小于
- `>`: 大于
- `<=`: 小于等于
- `>=`: 大于等于
- `LIKE`: 模式匹配（支持%和_通配符）

**逻辑操作符**：
- `AND`: 逻辑与
- `OR`: 逻辑或
- `NOT`: 逻辑非（预留）
- 支持括号嵌套：`(condition1 AND condition2) OR condition3`

**IN操作符**：
- `IN (value1, value2, ...)`: 值列表匹配
- `IN (SELECT ...)`: 嵌套查询匹配
- `NOT IN (...)`: 反向匹配

**示例**：
```sql
SELECT * FROM students WHERE age > 20;
SELECT * FROM students WHERE name LIKE 'A%';  -- 匹配以A开头的名字
SELECT * FROM students WHERE name LIKE '%li%';  -- 匹配包含'li'的名字
SELECT * FROM students WHERE id = 1 AND age > 18;
SELECT * FROM students WHERE (age > 20 AND gender = 'Female') OR id = 1;  -- 括号支持
SELECT * FROM students WHERE id IN (1, 2, 3);  -- IN值列表
SELECT * FROM students WHERE id IN (SELECT student_id FROM enrollments WHERE grade > 90);  -- IN嵌套查询
```

### ORDER BY排序支持

**语法**：
```sql
SELECT * FROM table_name ORDER BY column1 [ASC|DESC], column2 [ASC|DESC];
```

**特性**：
- 支持单列和多列排序
- 支持升序（ASC）和降序（DESC），默认为ASC
- 支持NULL值处理（NULL值排在最后）

**示例**：
```sql
SELECT * FROM students ORDER BY age ASC;
SELECT * FROM students ORDER BY age DESC, name ASC;  -- 先按年龄降序，再按姓名升序
```

---

## 实现细节

本节从“工程实现”的角度，补充若干在前文提到但未详细展开的细节：类型转换、数据验证、异常体系以及与性能相关的一些取舍。

### 类型转换

**TypeConverter工具类**：
- 将字符串转换为对应的Java类型
- 验证值是否符合字段类型要求
- 处理NULL值

**转换规则**：
- `INT`: `Integer.parseInt()`
- `FLOAT`: `Double.parseDouble()`
- `VARCHAR/CHAR`: 直接使用字符串
- `DATE`: 作为字符串处理（可扩展为Date对象）

### 数据验证

**Validator工具类**：
- 验证字段名和表名的合法性（字母、数字、下划线，不能以数字开头）
- 验证记录是否符合表结构要求
- 检查NULL约束
- 检查类型和长度约束

### 错误处理

**异常体系**：
- `DBMSException`: 基础异常类
- `SQLException`: SQL执行异常

**错误信息**：
- 提供详细的错误信息
- 包含错误位置（行号、列号）
- 提示期望的类型和实际类型

### 性能优化

**文件操作优化**：
- 使用`RandomAccessFile`进行随机访问
- 批量读取记录
- 逻辑删除避免频繁的文件重写

**查询优化**（可扩展）：
- 简单的嵌套循环连接
- 可扩展为哈希连接、排序合并连接
- 可添加索引机制

---

## 扩展方向

到这里，系统已经具备一个迷你关系型数据库的大部分核心能力。本节不再给出完整实现，而是以**“设计备忘录”**的形式，列出可以继续扩展的方向，便于后续课程或个人研究使用。

### 功能扩展

1. **索引支持**：✅ **已实现**
   - ✅ CREATE INDEX：创建普通索引
   - ✅ CREATE UNIQUE INDEX：创建唯一索引
   - ⏳ B+树索引（当前为简化实现）
   - ⏳ 哈希索引
   - ⏳ 主键自动索引

2. **事务支持**：✅ **已实现**
   - ✅ BEGIN TRANSACTION：开始事务
   - ✅ COMMIT：提交事务
   - ✅ ROLLBACK：回滚事务
   - ⏳ 完整ACID特性（当前为简化实现）
   - ⏳ WAL日志记录
   - ⏳ 完整回滚机制

3. **约束支持**：
   - 外键约束
   - 唯一约束
   - 检查约束

4. **高级查询**：
   - ✅ 聚合函数（COUNT, SUM, AVG, MAX, MIN） - **已实现**
   - ✅ GROUP BY - **已实现**
   - ✅ ORDER BY - **已实现**（支持多列排序、ASC/DESC）
   - ✅ 嵌套查询（IN (SELECT ...)） - **已实现**
   - ⏳ HAVING - 待实现
   - ⏳ EXISTS子查询 - 待实现
   - ⏳ 相关子查询 - 待实现

### 性能优化

1. **查询优化器**：
   - 查询计划生成
   - 成本估算
   - 执行计划选择

2. **缓存机制**：
   - 表结构缓存
   - 查询结果缓存
   - 文件缓冲区

3. **并发控制**：
   - 读写锁
   - 事务隔离级别
   - 死锁检测

---

## 阶段小结（一）

本节之前的内容，主要从**整体架构**、**核心模块**、**存储设计**、**SQL解析与执行引擎**以及**用户界面**几个角度，完整介绍了系统的基础设计与运行原理。

这一阶段可以理解为“从零设计一个迷你关系型数据库”的骨架部分：  
- 通过分层架构，让 UI、SQL 层、执行引擎、存储层和模型层之间的依赖关系清晰、单向；  
- 通过自定义 `.dbf` / `.dat` 文件格式，实现了可控且便于教学的持久化方案；  
- 通过 SQL 解析器与执行引擎，打通了“SQL 文本 → 实际读写文件”的完整闭环。

在此基础之上，后续章节会进一步深入到**具体算法实现、性能分析、典型使用场景**以及**与其他数据库系统的对比**，让整篇文档从“原理”自然过渡到“实践”和“对比分析”。

---

## 算法详解

本节将前面零散出现的关键算法（词法分析、语法分析、查询执行、文件读写）集中在一起，从更“理论化”的视角回顾一次，便于课程讲解或自学复盘。

### 词法分析算法

词法分析器使用有限状态自动机（FSM）将SQL文本转换为Token序列。

**状态转换图**：

```
初始状态
    ↓
空白字符 → 跳过 → 初始状态
    ↓
引号('或") → 字符串状态 → 读取字符 → 遇到匹配引号 → 初始状态
    ↓
数字 → 数字状态 → 读取数字/小数点 → 遇到非数字 → 初始状态
    ↓
操作符 → 操作符状态 → 检查多字符操作符 → 初始状态
    ↓
字母/下划线 → 标识符状态 → 读取字母数字下划线 → 遇到非标识符字符 → 初始状态
    ↓
标点符号 → 直接输出Token → 初始状态
```

**关键实现**：

```java
// 字符串字面量处理
if (ch == '\'' || ch == '"') {
    char stringChar = ch;
    pos++;
    StringBuilder sb = new StringBuilder();
    boolean escaped = false;
    
    while (pos < sql.length()) {
        char c = sql.charAt(pos);
        if (escaped) {
            sb.append(c);
            escaped = false;
        } else if (c == '\\') {
            escaped = true;
        } else if (c == stringChar) {
            pos++;
            break; // 字符串结束
        } else {
            sb.append(c);
        }
        pos++;
    }
    tokens.add(new Token(TokenType.STRING, sb.toString(), line, column));
}
```

### 语法分析算法

使用递归下降解析器（Recursive Descent Parser）实现。

**解析树结构**：

```
SQLStatement
├── DDLStatement
│   ├── CreateTableStatement
│   ├── AlterTableStatement
│   ├── DropTableStatement
│   └── RenameTableStatement
├── DMLStatement
│   ├── InsertStatement
│   ├── UpdateStatement
│   └── DeleteStatement
└── QueryStatement
    └── SelectStatement
```

**解析流程**：

1. **预测分析**：根据第一个Token确定语句类型
2. **递归解析**：对每个语法结构调用对应的解析方法
3. **错误恢复**：遇到错误时抛出异常，提供详细错误信息

**示例 - SELECT语句解析**：

```java
private SelectStatement parseSelect() {
    SelectStatement stmt = new SelectStatement();
    
    // 解析列列表
    if (peekToken(TokenType.OPERATOR, "*")) {
        consume();
        stmt.columnNames.add("*");
    } else {
        // 解析列名列表
        do {
            stmt.columnNames.add(expectIdentifier());
        } while (peekPunctuation(",") && consume());
    }
    
    // 解析FROM子句
    expectKeyword("FROM");
    stmt.tableNames.add(expectIdentifier());
    
    // 解析JOIN子句（可选）
    if (peekKeyword("JOIN")) {
        parseJoinClause(stmt);
    }
    
    // 解析WHERE子句（可选）
    if (peekKeyword("WHERE")) {
        consume();
        stmt.whereCondition = parseWhereCondition();
    }
    
    return stmt;
}
```

### 查询执行算法

#### 单表查询算法

**时间复杂度**：O(n)，其中n是记录数

**算法步骤**：

1. 读取所有记录：O(n)
2. WHERE条件过滤：O(n)
3. 字段投影：O(n × m)，m是字段数
4. 返回结果：O(k)，k是结果记录数

**伪代码**：

```
function SELECT(table, columns, condition):
    records = readAllRecords(table)
    result = []
    
    for each record in records:
        if condition == null or condition.matches(record):
            row = []
            for each column in columns:
                if column == "*":
                    row.addAll(record.values)
                else:
                    row.add(record.getValue(column))
            result.add(row)
    
    return result
```

#### 多表连接算法

**嵌套循环连接（Nested Loop Join）**：

**时间复杂度**：O(n × m)，其中n和m是两个表的记录数

**算法步骤**：

1. 读取两个表的所有记录
2. 外层循环遍历第一个表
3. 内层循环遍历第二个表
4. 检查连接条件
5. 如果匹配，组合记录

**伪代码**：

```
function JOIN(table1, table2, joinCondition, whereCondition):
    records1 = readAllRecords(table1)
    records2 = readAllRecords(table2)
    result = []
    
    for each r1 in records1:
        for each r2 in records2:
            if joinCondition.matches(r1, r2):
                combinedRow = combine(r1, r2)
                if whereCondition == null or whereCondition.matches(combinedRow):
                    result.add(combinedRow)
    
    return result
```

**优化方向**：
- **哈希连接**：对较小的表建立哈希表，时间复杂度可降至O(n + m)
- **排序合并连接**：先排序再合并，适合已排序的数据
- **索引连接**：使用索引加速连接条件匹配

### 文件读写算法

#### 记录定位算法

**定长记录定位**：
```
recordOffset = recordIndex × recordSize
```

**变长记录定位**：
- 需要顺序扫描或维护索引
- 当前实现使用顺序扫描

#### 记录读取算法

**顺序读取**：

```java
public static List<Record> readAllRecords(String filePath, Table table) {
    List<Record> records = new ArrayList<>();
    RandomAccessFile raf = new RandomAccessFile(filePath, "r");
    
    while (raf.getFilePointer() < raf.length()) {
        Record record = readRecord(raf, table);
        if (!record.isDeleted()) {
            records.add(record);
        }
    }
    
    return records;
}
```

**随机读取**（通过偏移量）：

```java
public static Record readRecordAt(String filePath, long position, Table table) {
    RandomAccessFile raf = new RandomAccessFile(filePath, "r");
    raf.seek(position);
    return readRecord(raf, table);
}
```

---

## 代码示例

为了把前面的原理与实现串起来，本节给出几段“从 SQL 到代码”的完整示例 —— 既包括 SQL 语句本身，也包括解析后的结构和最终调用的 Java 代码。

### 创建表示例

**SQL语句**：
```sql
CREATE TABLE students (
    id INT PRIMARY KEY NOT NULL,
    name VARCHAR(50) NOT NULL,
    age INT
);
```

**执行流程**：

1. **词法分析**：
   ```
   CREATE (KEYWORD)
   TABLE (KEYWORD)
   students (IDENTIFIER)
   ( (PUNCTUATION)
   id (IDENTIFIER)
   INT (KEYWORD)
   PRIMARY (KEYWORD)
   KEY (KEYWORD)
   NOT (KEYWORD)
   NULL (KEYWORD)
   ...
   ```

2. **语法分析**：
   ```java
   CreateTableStatement {
       tableName: "students"
       fields: [
           FieldDefinition {
               name: "id"
               type: INT
               length: 4
               isKey: true
               nullable: false
           },
           FieldDefinition {
               name: "name"
               type: VARCHAR
               length: 50
               isKey: false
               nullable: false
           },
           FieldDefinition {
               name: "age"
               type: INT
               length: 4
               isKey: false
               nullable: true
           }
       ]
   }
   ```

3. **执行**：
   ```java
   // 创建Table对象
   Table table = new Table("students");
   table.addField(new Field("id", FieldType.INT, 4, true, false));
   table.addField(new Field("name", FieldType.VARCHAR, 50, false, false));
   table.addField(new Field("age", FieldType.INT, 4, false, true));
   
   // 添加到数据库
   database.addTable(table);
   
   // 保存到文件
   DBFFileManager.createDatabaseFile(dbFilePath, database);
   ```

### 插入数据示例

**SQL语句**：
```sql
INSERT INTO students VALUES (1, 'Alice', 20);
```

**执行流程**：

1. **解析**：
   ```java
   InsertStatement {
       tableName: "students"
       columnNames: []  // 空表示按顺序插入
       values: [1, "Alice", 20]
   }
   ```

2. **类型转换**：
   ```java
   // 根据字段类型转换值
   Field idField = table.getFieldByIndex(0);  // INT
   Object idValue = TypeConverter.convertValue("1", FieldType.INT);  // 1
   
   Field nameField = table.getFieldByIndex(1);  // VARCHAR
   Object nameValue = TypeConverter.convertValue("Alice", FieldType.VARCHAR);  // "Alice"
   
   Field ageField = table.getFieldByIndex(2);  // INT
   Object ageValue = TypeConverter.convertValue("20", FieldType.INT);  // 20
   ```

3. **验证**：
   ```java
   Record record = new Record(3);
   record.setValue(0, 1);
   record.setValue(1, "Alice");
   record.setValue(2, 20);
   
   Validator.validateRecord(record, table);
   ```

4. **写入文件**：
   ```java
   // 追加到文件末尾
   DATFileManager.appendRecord(datFilePath, record, table);
   ```

### 查询数据示例

**SQL语句**：
```sql
SELECT * FROM students WHERE age > 18;
```

**执行流程**：

1. **解析**：
   ```java
   SelectStatement {
       columnNames: ["*"]
       tableNames: ["students"]
       whereCondition: QueryCondition {
           columnName: "age"
           operator: ">"
           value: 18
       }
   }
   ```

2. **读取记录**：
   ```java
   List<Record> allRecords = DATFileManager.readAllRecords(datFilePath, table);
   // 假设有3条记录：
   // Record1: [1, "Alice", 20]
   // Record2: [2, "Bob", 17]
   // Record3: [3, "Charlie", 22]
   ```

3. **过滤**：
   ```java
   List<Record> filteredRecords = new ArrayList<>();
   for (Record record : allRecords) {
       Object ageValue = record.getValue(table, "age");
       if (ageValue != null && ((Integer) ageValue) > 18) {
           filteredRecords.add(record);
       }
   }
   // 结果：Record1 (age=20), Record3 (age=22)
   ```

4. **投影**：
   ```java
   List<List<Object>> resultData = new ArrayList<>();
   for (Record record : filteredRecords) {
       List<Object> row = new ArrayList<>();
       // SELECT * 表示选择所有字段
       for (Field field : table.getFields()) {
           row.add(record.getValue(table, field.getName()));
       }
       resultData.add(row);
   }
   // 结果：
   // [[1, "Alice", 20], [3, "Charlie", 22]]
   ```

---

## 性能分析

在掌握了实现后，理解其性能特征同样重要。本节从时间复杂度、空间复杂度和主要瓶颈三个维度，给出一个定性+定量的评估，为后续优化和对比打基础。

### 时间复杂度分析

| 操作 | 时间复杂度 | 说明 |
|------|-----------|------|
| CREATE TABLE | O(1) | 追加到文件末尾 |
| DROP TABLE | O(n) | 需要重写整个.dbf文件，n是表数量 |
| ALTER TABLE | O(n) | 需要重写整个.dbf文件 |
| INSERT | O(1) | 追加到文件末尾 |
| UPDATE | O(n) | 需要读取所有记录，n是记录数 |
| DELETE | O(n) | 需要读取所有记录 |
| SELECT (无WHERE) | O(n) | 读取所有记录 |
| SELECT (有WHERE) | O(n) | 读取并过滤所有记录 |
| JOIN | O(n × m) | 嵌套循环，n和m是两个表的记录数 |

### 空间复杂度分析

| 数据结构 | 空间复杂度 | 说明 |
|---------|-----------|------|
| 表结构 | O(f) | f是字段数 |
| 单条记录 | O(f) | f是字段数 |
| 查询结果 | O(k × f) | k是结果记录数，f是字段数 |
| 文件存储 | O(n × f) | n是记录数，f是字段数 |

### 性能瓶颈

1. **全表扫描**：
   - 所有查询都需要读取所有记录
   - 没有索引机制
   - **优化方案**：实现B+树索引

2. **文件重写**：
   - ALTER TABLE和DROP TABLE需要重写整个文件
   - **优化方案**：使用日志文件记录变更，定期合并

3. **连接操作**：
   - 嵌套循环连接效率低
   - **优化方案**：实现哈希连接或排序合并连接

4. **内存占用**：
   - 查询时需要将所有记录加载到内存
   - **优化方案**：实现流式处理，分批读取

---

## 设计决策说明

很多实现细节背后都隐含着权衡：为什么这样做，而不是那样做？  
本节以问答的形式，对一些关键设计选择给出理由，帮助读者从“照着写”上升到“理解并能改”。

### 为什么使用自定义文件格式？

**优点**：
- 完全控制存储格式
- 可以针对特定需求优化
- 不依赖外部库
- 便于理解底层实现

**缺点**：
- 需要自己实现序列化/反序列化
- 兼容性问题（不同版本之间）
- 缺少标准工具支持

**替代方案**：
- JSON/XML：可读性好，但体积大、解析慢
- SQLite格式：标准格式，但实现复杂
- 关系数据库：功能完整，但不符合实验要求

### 为什么使用逻辑删除？

**优点**：
- 删除操作快速（只需修改标志位）
- 可以恢复误删的数据
- 避免文件碎片

**缺点**：
- 占用存储空间
- 需要定期清理
- 查询时需要过滤已删除记录

**实现**：
```java
// 删除时只修改标志位
raf.seek(position);
raf.writeInt(RECORD_DELETED);  // 0x00000001

// 读取时跳过已删除记录
if (status == RECORD_DELETED) {
    continue;  // 跳过
}
```

### 为什么使用UTF-8编码？

**原因**：
- 支持所有Unicode字符（包括中文）
- 向后兼容ASCII
- Java默认使用UTF-8
- 跨平台兼容性好

**实现**：
```java
// 写入字符串
byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
raf.writeInt(bytes.length);
raf.write(bytes);

// 读取字符串
int length = raf.readInt();
byte[] bytes = new byte[length];
raf.readFully(bytes);
String str = new String(bytes, StandardCharsets.UTF_8);
```

### 为什么使用大端序？

**原因**：
- 网络字节序标准
- 跨平台兼容性
- Java默认使用大端序（Big-Endian）

**实现**：
```java
// RandomAccessFile默认使用大端序
raf.writeInt(value);  // 大端序
int value = raf.readInt();  // 大端序
```

---

## 使用示例

本节从一个**完整的学生管理场景**出发，把表结构设计、数据插入、查询与更新串联起来，作为“如何使用这套 DBMS”的参考脚本。

### 完整示例：学生管理系统

#### 1. 创建数据库和表

```sql
-- 创建学生表
CREATE TABLE students (
    id INT PRIMARY KEY NOT NULL,
    name VARCHAR(50) NOT NULL,
    age INT,
    email VARCHAR(100)
);

-- 创建课程表
CREATE TABLE courses (
    course_id INT PRIMARY KEY NOT NULL,
    course_name VARCHAR(100) NOT NULL,
    credits INT
);

-- 创建选课表
CREATE TABLE enrollments (
    student_id INT NOT NULL,
    course_id INT NOT NULL,
    grade FLOAT
);
```

#### 2. 插入数据

```sql
-- 插入学生数据
INSERT INTO students VALUES (1, 'Alice', 20, 'alice@example.com');
INSERT INTO students VALUES (2, 'Bob', 21, 'bob@example.com');
INSERT INTO students VALUES (3, 'Charlie', 19, 'charlie@example.com');

-- 插入课程数据
INSERT INTO courses VALUES (101, 'Database Systems', 3);
INSERT INTO courses VALUES (102, 'Operating Systems', 4);
INSERT INTO courses VALUES (103, 'Computer Networks', 3);

-- 插入选课数据
INSERT INTO enrollments VALUES (1, 101, 85.5);
INSERT INTO enrollments VALUES (1, 102, 90.0);
INSERT INTO enrollments VALUES (2, 101, 78.5);
INSERT INTO enrollments VALUES (3, 103, 92.0);
```

#### 3. 查询数据

```sql
-- 查询所有学生
SELECT * FROM students;

-- 查询年龄大于20的学生
SELECT name, age FROM students WHERE age > 20;

-- 查询特定学生的信息
SELECT * FROM students WHERE name = 'Alice';

-- 多表连接查询：查询学生选课信息
SELECT s.name, c.course_name, e.grade
FROM students s
JOIN enrollments e ON s.id = e.student_id
JOIN courses c ON e.course_id = c.course_id;
```

#### 4. 更新数据

```sql
-- 更新学生年龄
UPDATE students SET age = 22 WHERE id = 1;

-- 更新成绩
UPDATE enrollments SET grade = 88.0 
WHERE student_id = 1 AND course_id = 101;
```

#### 5. 删除数据

```sql
-- 删除选课记录
DELETE FROM enrollments WHERE student_id = 2;

-- 删除学生
DELETE FROM students WHERE id = 3;
```

#### 6. 修改表结构

```sql
-- 添加字段
ALTER TABLE students ADD COLUMN phone VARCHAR(20);

-- 修改字段类型
ALTER TABLE students MODIFY COLUMN email VARCHAR(150);

-- 重命名字段
ALTER TABLE students RENAME COLUMN phone TO phone_number;

-- 删除字段
ALTER TABLE students DROP COLUMN phone_number;
```

---

## 故障排查

任何系统在实际使用中都会遇到问题。本节整理了若干典型错误信息及成因，并给出排查建议，可作为实验或作业时的“FAQ”。

### 常见问题及解决方案

#### 1. 文件读取错误

**问题**：`Invalid database file format`

**原因**：
- 文件损坏
- 文件格式不匹配
- 文件被其他程序修改

**解决方案**：
- 检查文件是否完整
- 确认文件版本号
- 使用备份文件恢复

#### 2. 类型转换错误

**问题**：`Cannot convert 'abc' to type INT`

**原因**：
- 字符串无法转换为数字
- 数据类型不匹配

**解决方案**：
- 检查SQL语句中的值类型
- 确保字符串值用引号括起来
- 检查字段类型定义

#### 3. 字段不存在错误

**问题**：`Column 'xxx' does not exist`

**原因**：
- 字段名拼写错误
- 表结构已更改

**解决方案**：
- 检查字段名是否正确
- 使用`SELECT *`查看表结构
- 确认表名是否正确

#### 4. 主键冲突

**问题**：`Primary key violation`

**原因**：
- 插入的主键值已存在
- 主键约束未正确实现

**解决方案**：
- 检查主键值是否重复
- 使用不同的主键值
- 实现主键唯一性检查（当前版本未实现）

#### 5. NULL约束违反

**问题**：`Field 'xxx' cannot be NULL`

**原因**：
- 字段定义为NOT NULL
- 插入时未提供值

**解决方案**：
- 为NOT NULL字段提供值
- 或修改字段定义允许NULL

#### 6. 文件锁定问题

**问题**：`File is locked by another process`

**原因**：
- 文件被其他程序打开
- 文件句柄未正确关闭

**解决方案**：
- 关闭其他打开该文件的程序
- 使用try-with-resources确保文件正确关闭
- 检查是否有未关闭的RandomAccessFile

---

## 测试用例

为了保证系统稳定性，本节给出了一些具有代表性的单元测试和集成测试示例，帮助读者理解如何为自己的 DBMS 编写测试用例。

### 单元测试示例

#### 测试字段类型转换

```java
@Test
public void testTypeConverter() {
    // 测试INT类型
    Object intValue = TypeConverter.convertValue("123", FieldType.INT);
    assertEquals(123, intValue);
    
    // 测试FLOAT类型
    Object floatValue = TypeConverter.convertValue("3.14", FieldType.FLOAT);
    assertEquals(3.14, (Double) floatValue, 0.001);
    
    // 测试VARCHAR类型
    Object strValue = TypeConverter.convertValue("test", FieldType.VARCHAR);
    assertEquals("test", strValue);
    
    // 测试NULL值
    Object nullValue = TypeConverter.convertValue("NULL", FieldType.INT);
    assertNull(nullValue);
}
```

#### 测试记录验证

```java
@Test
public void testRecordValidation() {
    Table table = new Table("test");
    table.addField(new Field("id", FieldType.INT, 4, true, false));
    table.addField(new Field("name", FieldType.VARCHAR, 50, false, false));
    
    // 有效记录
    Record validRecord = new Record(2);
    validRecord.setValue(0, 1);
    validRecord.setValue(1, "test");
    assertDoesNotThrow(() -> Validator.validateRecord(validRecord, table));
    
    // NULL值违反约束
    Record invalidRecord = new Record(2);
    invalidRecord.setValue(0, null);  // NOT NULL字段
    invalidRecord.setValue(1, "test");
    assertThrows(IllegalArgumentException.class, 
        () -> Validator.validateRecord(invalidRecord, table));
}
```

#### 测试SQL解析

```java
@Test
public void testSQLParser() {
    SQLParser parser = new SQLParser();
    
    // 测试CREATE TABLE
    String sql = "CREATE TABLE test (id INT, name VARCHAR(50));";
    SQLStatement stmt = parser.parse(sql);
    assertTrue(stmt instanceof CreateTableStatement);
    CreateTableStatement createStmt = (CreateTableStatement) stmt;
    assertEquals("test", createStmt.tableName);
    assertEquals(2, createStmt.fields.size());
    
    // 测试SELECT
    sql = "SELECT * FROM test WHERE id > 10;";
    stmt = parser.parse(sql);
    assertTrue(stmt instanceof SelectStatement);
    SelectStatement selectStmt = (SelectStatement) stmt;
    assertEquals("test", selectStmt.tableNames.get(0));
    assertNotNull(selectStmt.whereCondition);
}
```

### 集成测试示例

#### 测试完整流程

```java
@Test
public void testCompleteWorkflow() throws Exception {
    // 1. 创建数据库
    Database db = new Database("TestDB");
    DBFFileManager.createDatabaseFile("test.dbf", db);
    
    // 2. 创建表
    Table table = new Table("students");
    table.addField(new Field("id", FieldType.INT, 4, true, false));
    table.addField(new Field("name", FieldType.VARCHAR, 50, false, false));
    db.addTable(table);
    DBFFileManager.createDatabaseFile("test.dbf", db);
    
    // 3. 插入数据
    Record record = new Record(2);
    record.setValue(0, 1);
    record.setValue(1, "Alice");
    DATFileManager.appendRecord("test.dat", record, table);
    
    // 4. 查询数据
    List<Record> records = DATFileManager.readAllRecords("test.dat", table);
    assertEquals(1, records.size());
    assertEquals(1, records.get(0).getValue(0));
    assertEquals("Alice", records.get(0).getValue(1));
    
    // 5. 清理
    new File("test.dbf").delete();
    new File("test.dat").delete();
}
```

---

## 性能优化建议

如果希望把该系统从“教学原型”进一步演化为“更高效的实验平台”，本节提供了几条可操作的优化路径，并配以代码骨架示例。

### 1. 索引实现

**B+树索引**：

```java
public class BPlusTreeIndex {
    private BPlusTreeNode root;
    private int order;  // B+树的阶
    
    // 插入索引
    public void insert(Object key, long recordOffset) {
        // B+树插入算法
    }
    
    // 查找索引
    public List<Long> search(Object key) {
        // B+树查找算法
        return offsets;
    }
}
```

**使用索引加速查询**：
- 主键查询：O(log n)
- 范围查询：O(log n + k)，k是结果数
- WHERE条件：如果字段有索引，可以使用索引

### 2. 查询优化

**查询计划优化**：

```java
public class QueryOptimizer {
    // 选择最优连接顺序
    public List<Table> optimizeJoinOrder(List<Table> tables) {
        // 使用动态规划选择最优顺序
        // 考虑表的大小、索引等因素
    }
    
    // 选择最优连接算法
    public JoinAlgorithm selectJoinAlgorithm(Table t1, Table t2) {
        if (t1.size() < t2.size()) {
            return JoinAlgorithm.HASH_JOIN;  // 小表建立哈希表
        } else {
            return JoinAlgorithm.SORT_MERGE_JOIN;  // 排序合并
        }
    }
}
```

### 3. 缓存机制

**表结构缓存**：

```java
public class TableCache {
    private Map<String, Table> cache = new HashMap<>();
    
    public Table getTable(String tableName) {
        if (!cache.containsKey(tableName)) {
            // 从文件加载
            Table table = loadFromFile(tableName);
            cache.put(tableName, table);
        }
        return cache.get(tableName);
    }
    
    public void invalidate(String tableName) {
        cache.remove(tableName);
    }
}
```

### 4. 批量操作

**批量插入**：

```java
public void batchInsert(String tableName, List<Record> records) {
    // 批量写入，减少文件I/O次数
    try (RandomAccessFile raf = new RandomAccessFile(datFilePath, "rw")) {
        raf.seek(raf.length());  // 定位到文件末尾
        for (Record record : records) {
            writeRecord(raf, record, table);
        }
    }
}
```

---

## 安全性考虑

尽管本系统主要用于教学与实验，但在设计时仍然可以借鉴工业级数据库的一些安全理念。本节简要讨论 SQL 注入、防止误删和文件访问控制等问题。

### 1. SQL注入防护

**当前实现**：
- 使用预编译的解析器
- 参数化查询（可扩展）

**建议改进**：
```java
// 参数化查询示例
String sql = "SELECT * FROM users WHERE name = ? AND age = ?";
PreparedStatement stmt = parser.prepare(sql);
stmt.setString(1, userName);
stmt.setInt(2, userAge);
```

### 2. 文件访问控制

**建议实现**：
- 文件权限检查
- 只读模式支持
- 并发访问控制

### 3. 数据验证

**当前实现**：
- 类型验证
- NULL约束检查
- 长度限制

**建议扩展**：
- 正则表达式验证
- 范围检查
- 自定义验证规则

---

## 扩展功能实现指南

如果你希望把本系统当作“课程大作业框架”或“个人练手项目”，本节给出了若干扩展功能的实现步骤，包括：索引、事务和聚合函数等。

### 实现索引

**步骤**：

1. **定义索引结构**：
```java
public class Index {
    private String indexName;
    private String tableName;
    private String columnName;
    private IndexType type;  // B_TREE, HASH
    private BPlusTree tree;  // 或HashTable
}
```

2. **创建索引**：
```sql
CREATE INDEX idx_name ON students(name);
```

3. **使用索引**：
- 查询时检查是否有可用索引
- 使用索引加速查找
- 维护索引的一致性

### 实现事务

**步骤**：

1. **事务管理器**：
```java
public class TransactionManager {
    private List<Transaction> activeTransactions;
    
    public Transaction beginTransaction() {
        Transaction txn = new Transaction();
        activeTransactions.add(txn);
        return txn;
    }
    
    public void commit(Transaction txn) {
        // 应用所有变更
        // 写入日志
        activeTransactions.remove(txn);
    }
    
    public void rollback(Transaction txn) {
        // 撤销所有变更
        activeTransactions.remove(txn);
    }
}
```

2. **日志记录**：
```java
public class WriteAheadLog {
    public void logInsert(String table, Record record) {
        // 记录插入操作
    }
    
    public void replay() {
        // 重放日志，恢复数据
    }
}
```

### 已实现的聚合函数功能 ✅

**支持的聚合函数**：
- `COUNT(*)`: 计算行数
- `COUNT(column)`: 计算非NULL值的数量
- `SUM(column)`: 求和
- `AVG(column)`: 平均值
- `MAX(column)`: 最大值
- `MIN(column)`: 最小值

**GROUP BY支持**：
- 支持按单列或多列分组
- 支持与聚合函数结合使用
- 支持表别名（如 `s.name`）

**列别名支持**：
- 使用 `AS` 关键字：`SELECT COUNT(*) AS total`
- 或直接使用标识符：`SELECT COUNT(*) total`

**示例**：
```sql
-- 统计每个学生的选课数量
SELECT s.name, COUNT(*) as course_count
FROM students s, enrollments e
WHERE s.id = e.student_id
GROUP BY s.name;

-- 计算每门课程的平均分
SELECT course_id, AVG(grade) as avg_grade
FROM enrollments
GROUP BY course_id;
```

---

## 阶段小结（二）

在前面的章节中，我们已经从**实现细节、算法分析、性能与设计决策、使用示例、故障排查与测试**等角度，对系统做了较为完整的工程化说明。

可以把这一部分看作是对“骨架系统”的一次**工程实践梳理**：不仅关注“能跑起来”，还关注**可靠性、可维护性和可优化空间**，为后续扩展和对比分析打下基础。

### 核心亮点

1. **自定义文件格式**：完全控制存储格式，便于优化
2. **SQL解析**：完整的词法和语法分析
3. **执行引擎**：支持DDL和DML操作
4. **图形界面**：友好的用户交互体验

### 学习价值

- 理解数据库系统的基本原理
- 掌握文件I/O和序列化技术
- 学习SQL解析和执行
- 实践软件工程和架构设计

### 改进方向

- 添加索引机制提高查询性能
- 实现事务支持保证数据一致性
- 支持更多SQL特性（聚合、分组、排序等）
- 优化连接算法提高多表查询效率

---

## 详细代码实现说明

### 二进制序列化实现

#### 整数序列化

```java
// 写入整数（大端序）
public static void writeInt(DataOutputStream dos, int value) throws IOException {
    dos.writeInt(value);  // Java默认使用大端序
}

// 读取整数
public static int readInt(DataInputStream dis) throws IOException {
    return dis.readInt();
}
```

**实现细节**：
- Java的`DataOutputStream.writeInt()`默认使用大端序（Big-Endian）
- 4字节固定长度
- 支持范围：-2,147,483,648 到 2,147,483,647

#### 字符串序列化

```java
// 写入字符串（UTF-8编码，长度前缀）
public static void writeString(DataOutputStream dos, String str) throws IOException {
    if (str == null) {
        writeInt(dos, 0);  // 长度为0表示NULL
        return;
    }
    byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
    writeInt(dos, bytes.length);  // 先写长度
    dos.write(bytes);              // 再写内容
}

// 读取字符串
public static String readString(DataInputStream dis) throws IOException {
    int length = readInt(dis);
    if (length == 0) {
        return null;  // 长度为0表示NULL
    }
    byte[] bytes = new byte[length];
    dis.readFully(bytes);  // 读取完整字节数组
    return new String(bytes, StandardCharsets.UTF_8);
}
```

**实现细节**：
- 使用UTF-8编码支持所有Unicode字符
- 长度前缀（4字节）允许变长字符串
- NULL值用长度为0表示
- `readFully()`确保读取完整数据

#### 表结构序列化

```java
// 写入表结构
public static void writeTable(DataOutputStream dos, Table table) throws IOException {
    // 1. 写入表名
    writeString(dos, table.getName());
    
    // 2. 写入字段数量
    writeInt(dos, table.getFieldCount());
    
    // 3. 写入每个字段
    for (Field field : table.getFields()) {
        writeField(dos, field);
    }
    
    // 4. 写入元数据
    writeInt(dos, table.getRecordCount());
    writeLong(dos, table.getLastModified());
}

// 读取表结构
public static Table readTable(DataInputStream dis) throws IOException {
    Table table = new Table();
    
    // 1. 读取表名
    table.setName(readString(dis));
    
    // 2. 读取字段数量
    int fieldCount = readInt(dis);
    
    // 3. 读取每个字段
    List<Field> fields = new ArrayList<>();
    for (int i = 0; i < fieldCount; i++) {
        fields.add(readField(dis));
    }
    table.setFields(fields);
    
    // 4. 读取元数据
    table.setRecordCount(readInt(dis));
    table.setLastModified(readLong(dis));
    
    return table;
}
```

**设计要点**：
- 顺序写入，顺序读取
- 字段数量前置，便于预分配数组
- 元数据放在最后，便于扩展

### 文件管理器实现

#### 数据库文件创建

```java
public static void createDatabaseFile(String filePath, Database database) throws IOException {
    try (RandomAccessFile raf = new RandomAccessFile(filePath, "rw")) {
        // 1. 写入文件头
        writeHeader(raf, database);
        
        // 2. 写入表结构
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
        
        // 3. 更新文件头中的表索引
        updateTableIndex(raf, indexEntries);
    }
}
```

**实现细节**：
- 使用`RandomAccessFile`支持随机访问
- 先写入表结构，再更新索引
- 索引存储在文件头，便于快速查找

#### 记录读写实现

```java
// 写入记录
public static void writeRecord(RandomAccessFile raf, Record record, Table table) throws IOException {
    // 1. 写入记录状态
    raf.writeInt(record.isDeleted() ? FileFormat.RECORD_DELETED : FileFormat.RECORD_ACTIVE);
    
    // 2. 写入每个字段的值
    for (int i = 0; i < table.getFieldCount(); i++) {
        Field field = table.getFieldByIndex(i);
        Object value = record.getValue(i);
        writeFieldValue(raf, value, field);
    }
}

// 读取记录
public static Record readRecord(RandomAccessFile raf, Table table) throws IOException {
    Record record = new Record(table.getFieldCount());
    
    // 1. 读取记录状态
    int status = raf.readInt();
    record.setDeleted(status == FileFormat.RECORD_DELETED);
    
    // 2. 读取每个字段的值
    for (int i = 0; i < table.getFieldCount(); i++) {
        Field field = table.getFieldByIndex(i);
        Object value = readFieldValue(raf, field);
        record.setValue(i, value);
    }
    
    return record;
}
```

**字段值读写**：

```java
// 写入VARCHAR字段值
case VARCHAR:
    String str = value.toString();
    byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
    raf.writeInt(bytes.length);  // 长度前缀
    raf.write(bytes);           // 数据内容
    break;

// 读取VARCHAR字段值
case VARCHAR:
    int length = raf.readInt();
    if (length == 0) {
        return null;  // NULL值
    }
    byte[] bytes = new byte[length];
    raf.readFully(bytes);
    return new String(bytes, StandardCharsets.UTF_8);
```

### SQL解析器实现

#### 词法分析器状态机

```java
public List<Token> tokenize(String sql) {
    List<Token> tokens = new ArrayList<>();
    int pos = 0;
    int line = 1;
    int column = 1;
    
    while (pos < sql.length()) {
        char ch = sql.charAt(pos);
        
        // 状态1: 空白字符处理
        if (Character.isWhitespace(ch)) {
            if (ch == '\n') {
                line++;
                column = 1;
            } else {
                column++;
            }
            pos++;
            continue;
        }
        
        // 状态2: 字符串字面量
        if (ch == '\'' || ch == '"') {
            Token token = parseStringLiteral(sql, pos, line, column);
            tokens.add(token);
            pos = token.endPos;
            continue;
        }
        
        // 状态3: 数字字面量
        if (Character.isDigit(ch) || ch == '-' || ch == '+') {
            Token token = parseNumber(sql, pos, line, column);
            tokens.add(token);
            pos = token.endPos;
            continue;
        }
        
        // 状态4: 操作符
        if (isOperator(ch)) {
            Token token = parseOperator(sql, pos, line, column);
            tokens.add(token);
            pos = token.endPos;
            continue;
        }
        
        // 状态5: 标识符或关键字
        if (Character.isLetter(ch) || ch == '_') {
            Token token = parseIdentifierOrKeyword(sql, pos, line, column);
            tokens.add(token);
            pos = token.endPos;
            continue;
        }
        
        // 状态6: 标点符号
        if (isPunctuation(ch)) {
            tokens.add(new Token(TokenType.PUNCTUATION, String.valueOf(ch), line, column));
            pos++;
            column++;
            continue;
        }
        
        // 未知字符
        throw new RuntimeException("Unexpected character: " + ch);
    }
    
    tokens.add(new Token(TokenType.EOF, "", line, column));
    return tokens;
}
```

#### 语法分析器实现

**递归下降解析**：

```java
// 解析字段定义
private FieldDefinition parseFieldDefinition() {
    FieldDefinition fieldDef = new FieldDefinition();
    
    // 1. 解析字段名
    fieldDef.name = expectIdentifier();
    
    // 2. 解析类型（可能是关键字，如INT、VARCHAR）
    String typeName;
    Token typeToken = currentToken();
    if (typeToken.type == TokenType.KEYWORD) {
        typeName = consume().value;
    } else if (typeToken.type == TokenType.IDENTIFIER) {
        typeName = expectIdentifier();
    } else {
        throw new SQLException("Expected type name");
    }
    fieldDef.type = FieldType.fromString(typeName);
    
    // 3. 解析长度（如果有括号）
    if (peekPunctuation("(")) {
        consume();
        fieldDef.length = Integer.parseInt(expectToken(TokenType.NUMBER).value);
        expectPunctuation(")");
    } else {
        fieldDef.length = fieldDef.type.getDefaultLength();
    }
    
    // 4. 解析约束（PRIMARY KEY, NOT NULL等）
    fieldDef.isKey = false;
    fieldDef.nullable = true;
    
    while (peekKeyword("PRIMARY") || peekKeyword("NOT")) {
        if (peekKeyword("PRIMARY")) {
            consume();
            expectKeyword("KEY");
            fieldDef.isKey = true;
        } else if (peekKeyword("NOT")) {
            consume();
            expectKeyword("NULL");
            fieldDef.nullable = false;
        }
    }
    
    return fieldDef;
}
```

**错误处理机制**：

```java
private Token expectToken(TokenType type) {
    Token token = currentToken();
    if (token.type != type) {
        throw new SQLException(
            String.format("Expected %s, got %s at line %d, column %d",
                type, token.type, token.line, token.column));
    }
    return consume();
}
```

### 执行引擎实现

#### INSERT执行流程

```java
public void insert(String tableName, List<Object> values) {
    // 1. 获取表结构
    Table table = ddlExecutor.getTable(tableName);
    if (table == null) {
        throw new DBMSException("Table " + tableName + " does not exist");
    }
    
    // 2. 验证值数量
    if (values.size() != table.getFieldCount()) {
        throw new DBMSException("Value count mismatch");
    }
    
    // 3. 创建记录对象
    Record record = new Record(table.getFieldCount());
    
    // 4. 类型转换和赋值
    for (int i = 0; i < values.size(); i++) {
        Field field = table.getFieldByIndex(i);
        Object value = values.get(i);
        
        // 类型转换
        if (value instanceof String) {
            value = TypeConverter.convertValue((String) value, field.getType());
        }
        
        record.setValue(i, value);
    }
    
    // 5. 验证记录
    Validator.validateRecord(record, table);
    
    // 6. 写入文件
    try {
        DATFileManager.appendRecord(datFilePath, record, table);
        table.setRecordCount(table.getRecordCount() + 1);
    } catch (IOException e) {
        throw new DBMSException("Failed to insert record", e);
    }
}
```

#### SELECT执行流程

```java
public QueryResult select(String tableName, List<String> columnNames, 
                         QueryCondition condition) {
    // 1. 获取表结构
    Table table = ddlExecutor.getTable(tableName);
    
    // 2. 确定要选择的列
    if (columnNames == null || columnNames.contains("*")) {
        columnNames = table.getFields().stream()
            .map(Field::getName)
            .collect(Collectors.toList());
    }
    
    // 3. 读取所有记录
    List<Record> allRecords = DATFileManager.readAllRecords(datFilePath, table);
    
    // 4. WHERE条件过滤
    List<Record> filteredRecords = new ArrayList<>();
    for (Record record : allRecords) {
        if (condition == null || condition.matches(record, table)) {
            filteredRecords.add(record);
        }
    }
    
    // 5. 字段投影
    List<List<Object>> resultData = new ArrayList<>();
    for (Record record : filteredRecords) {
        List<Object> row = new ArrayList<>();
        for (String colName : columnNames) {
            row.add(record.getValue(table, colName));
        }
        resultData.add(row);
    }
    
    return new QueryResult(columnNames, resultData);
}
```

#### WHERE条件匹配

```java
public boolean matches(Record record, Table table) {
    Field field = table.getFieldByName(columnName);
    if (field == null) {
        return false;
    }
    
    Object recordValue = record.getValue(table, columnName);
    
    // NULL值处理
    if (recordValue == null || value == null) {
        return operator.equals("=") && recordValue == value;
    }
    
    // 比较操作
    switch (operator) {
        case "=":
            return recordValue.equals(value);
        case "!=":
        case "<>":
            return !recordValue.equals(value);
        case "<":
            return compare(recordValue, value) < 0;
        case ">":
            return compare(recordValue, value) > 0;
        case "<=":
            return compare(recordValue, value) <= 0;
        case ">=":
            return compare(recordValue, value) >= 0;
        case "LIKE":
            // 模式匹配：%表示任意字符，_表示单个字符
            String pattern = ((String) value)
                .replace("%", ".*")
                .replace("_", ".");
            return ((String) recordValue).matches(pattern);
        default:
            return false;
    }
}
```

---

## 更多实际使用场景

在前面的章节中，我们主要从“系统内部实现”和“单条 SQL 示例”的角度说明了本 DBMS 的工作方式。  
本章节开始，将站在**应用者的视角**，给出若干完整的小型业务场景，展示如何用本系统搭建实际的业务数据库结构，并编写典型查询语句。

### 场景1：图书管理系统

#### 数据库设计

```sql
-- 创建图书表
CREATE TABLE books (
    book_id INT PRIMARY KEY NOT NULL,
    title VARCHAR(200) NOT NULL,
    author VARCHAR(100) NOT NULL,
    isbn VARCHAR(20),
    price FLOAT,
    stock INT NOT NULL
);

-- 创建借阅记录表
CREATE TABLE borrowings (
    borrowing_id INT PRIMARY KEY NOT NULL,
    book_id INT NOT NULL,
    borrower_name VARCHAR(50) NOT NULL,
    borrow_date VARCHAR(20) NOT NULL,
    return_date VARCHAR(20)
);

-- 创建分类表
CREATE TABLE categories (
    category_id INT PRIMARY KEY NOT NULL,
    category_name VARCHAR(50) NOT NULL
);
```

#### 常用操作

```sql
-- 添加图书
INSERT INTO books VALUES (1, '数据库系统概念', 'Abraham Silberschatz', '978-7-111-43177-4', 89.00, 10);
INSERT INTO books VALUES (2, '算法导论', 'Thomas H. Cormen', '978-7-111-40701-0', 128.00, 5);
INSERT INTO books VALUES (3, '操作系统概念', 'Abraham Silberschatz', '978-7-111-43178-1', 79.00, 8);

-- 查询所有图书
SELECT * FROM books;

-- 查询特定作者的图书
SELECT title, price FROM books WHERE author = 'Abraham Silberschatz';

-- 查询库存不足的图书
SELECT * FROM books WHERE stock < 5;

-- 借阅图书
INSERT INTO borrowings VALUES (1, 1, '张三', '2024-01-01', NULL);

-- 归还图书
UPDATE borrowings SET return_date = '2024-01-15' WHERE borrowing_id = 1;

-- 查询未归还的图书
SELECT b.title, br.borrower_name, br.borrow_date
FROM books b
JOIN borrowings br ON b.book_id = br.book_id
WHERE br.return_date IS NULL;
```

### 场景2：员工管理系统

#### 数据库设计

```sql
-- 部门表
CREATE TABLE departments (
    dept_id INT PRIMARY KEY NOT NULL,
    dept_name VARCHAR(50) NOT NULL,
    location VARCHAR(100)
);

-- 员工表
CREATE TABLE employees (
    emp_id INT PRIMARY KEY NOT NULL,
    name VARCHAR(50) NOT NULL,
    dept_id INT,
    position VARCHAR(50),
    salary FLOAT,
    hire_date VARCHAR(20)
);

-- 项目表
CREATE TABLE projects (
    project_id INT PRIMARY KEY NOT NULL,
    project_name VARCHAR(100) NOT NULL,
    start_date VARCHAR(20),
    end_date VARCHAR(20)
);

-- 员工项目关联表
CREATE TABLE employee_projects (
    emp_id INT NOT NULL,
    project_id INT NOT NULL,
    role VARCHAR(50)
);
```

#### 常用查询

```sql
-- 查询各部门员工数量
SELECT d.dept_name, COUNT(e.emp_id) as employee_count
FROM departments d
LEFT JOIN employees e ON d.dept_id = e.dept_id
GROUP BY d.dept_name;

-- 查询高薪员工（假设需要手动计算）
SELECT name, salary FROM employees WHERE salary > 10000;

-- 查询参与多个项目的员工
SELECT e.name, COUNT(ep.project_id) as project_count
FROM employees e
JOIN employee_projects ep ON e.emp_id = ep.emp_id
GROUP BY e.emp_id, e.name
HAVING COUNT(ep.project_id) > 1;
```

### 场景3：学生成绩管理系统

#### 数据库设计

```sql
-- 学生表
CREATE TABLE students (
    student_id INT PRIMARY KEY NOT NULL,
    name VARCHAR(50) NOT NULL,
    class VARCHAR(20),
    gender VARCHAR(10)
);

-- 课程表
CREATE TABLE courses (
    course_id INT PRIMARY KEY NOT NULL,
    course_name VARCHAR(100) NOT NULL,
    teacher VARCHAR(50),
    credits INT
);

-- 成绩表
CREATE TABLE grades (
    student_id INT NOT NULL,
    course_id INT NOT NULL,
    score FLOAT,
    exam_date VARCHAR(20)
);
```

#### 复杂查询示例

```sql
-- 查询每个学生的平均成绩
SELECT s.name, AVG(g.score) as avg_score
FROM students s
JOIN grades g ON s.student_id = g.student_id
GROUP BY s.student_id, s.name;

-- 查询不及格的学生
SELECT s.name, c.course_name, g.score
FROM students s
JOIN grades g ON s.student_id = g.student_id
JOIN courses c ON g.course_id = c.course_id
WHERE g.score < 60;

-- 查询每门课程的最高分
SELECT c.course_name, MAX(g.score) as max_score
FROM courses c
JOIN grades g ON c.course_id = g.course_id
GROUP BY c.course_id, c.course_name;
```

### 场景4：电商订单系统

#### 数据库设计

```sql
-- 商品表
CREATE TABLE products (
    product_id INT PRIMARY KEY NOT NULL,
    product_name VARCHAR(200) NOT NULL,
    price FLOAT NOT NULL,
    stock INT NOT NULL,
    category VARCHAR(50)
);

-- 订单表
CREATE TABLE orders (
    order_id INT PRIMARY KEY NOT NULL,
    customer_name VARCHAR(50) NOT NULL,
    order_date VARCHAR(20) NOT NULL,
    total FLOAT NOT NULL,
    status VARCHAR(20)
);

-- 订单明细表
CREATE TABLE order_items (
    order_id INT NOT NULL,
    product_id INT NOT NULL,
    quantity INT NOT NULL,
    price FLOAT NOT NULL
);
```

#### 业务查询

```sql
-- 查询热销商品（订单数量最多的商品）
SELECT p.product_name, SUM(oi.quantity) as total_sold
FROM products p
JOIN order_items oi ON p.product_id = oi.product_id
GROUP BY p.product_id, p.product_name
ORDER BY total_sold DESC;

-- 查询待发货订单
SELECT * FROM orders WHERE status = '待发货';

-- 查询某个客户的订单历史
SELECT o.order_id, o.order_date, o.total, o.status
FROM orders o
WHERE o.customer_name = '张三'
ORDER BY o.order_date DESC;
```

---

## 与其他数据库系统对比

在了解了系统的内部实现和典型使用场景之后，本章节从更高的视角，将本系统与 SQLite、MySQL、PostgreSQL 等主流数据库进行对比。  
目标不是“谁更强”，而是**明确本系统的教学定位**、优缺点以及在实际项目中如何合理选型。

### 功能对比表

| 特性 | 本系统 | SQLite | MySQL | PostgreSQL |
|------|--------|--------|-------|------------|
| **存储方式** | 自定义二进制文件 | SQLite数据库文件 | 服务器+文件系统 | 服务器+文件系统 |
| **SQL支持** | 基础DDL/DML | 完整SQL | 完整SQL | 完整SQL |
| **数据类型** | 5种基础类型 | 丰富的数据类型 | 丰富的数据类型 | 丰富的数据类型 |
| **索引** | ✅ 基础支持 | B树索引 | B+树索引 | B树/GiST索引 |
| **事务** | ✅ 基础支持 | ACID事务 | ACID事务 | ACID事务 |
| **并发控制** | 不支持 | 文件锁 | 行级锁 | MVCC |
| **外键约束** | 不支持 | 支持 | 支持 | 支持 |
| **触发器** | 不支持 | 支持 | 支持 | 支持 |
| **存储过程** | 不支持 | 不支持 | 支持 | 支持 |
| **视图** | 不支持 | 支持 | 支持 | 支持 |
| **用户权限** | ✅ 基础支持 | 不支持 | 支持 | 支持 |
| **复制/集群** | 不支持 | 不支持 | 支持 | 支持 |
| **文件大小** | 无限制 | 281TB | 取决于文件系统 | 取决于文件系统 |
| **跨平台** | 是（Java） | 是 | 是 | 是 |
| **依赖** | 仅需JRE | 无依赖 | 需要服务器 | 需要服务器 |
| **学习价值** | 高（可理解原理） | 中 | 低（黑盒） | 低（黑盒） |

### 详细对比分析

#### 1. 存储架构对比

**本系统**：
- 文件格式：自定义的.dbf和.dat文件
- 优点：完全控制，便于理解
- 缺点：需要自己实现所有功能

**SQLite**：
- 文件格式：单个SQLite数据库文件
- 优点：标准格式，工具支持丰富
- 缺点：格式复杂，难以理解

**MySQL/PostgreSQL**：
- 存储：服务器进程 + 文件系统
- 优点：功能完整，性能优秀
- 缺点：需要服务器，配置复杂

#### 2. SQL支持对比

**本系统支持的SQL**：
```sql
-- DDL
CREATE TABLE, ALTER TABLE, DROP TABLE, RENAME TABLE
CREATE INDEX, CREATE UNIQUE INDEX

-- DML
INSERT, UPDATE, DELETE, SELECT

-- DCL
CREATE USER, DROP USER
GRANT, REVOKE

-- 事务控制
BEGIN TRANSACTION, COMMIT, ROLLBACK

-- 查询特性
WHERE条件（=, !=, <, >, <=, >=, LIKE, IN）
WHERE条件支持AND/OR逻辑和括号嵌套
IN (SELECT ...) 嵌套查询
JOIN（等值连接，支持隐式和显式JOIN）
聚合函数：COUNT, SUM, AVG, MAX, MIN
GROUP BY分组
ORDER BY排序（支持多列、ASC/DESC）
列别名（AS关键字）
UPDATE表达式（如 age = age + 1）
表别名（在JOIN查询中）
```

**SQLite/MySQL/PostgreSQL支持的SQL**：
```sql
-- 所有本系统支持的SQL，plus:
-- 分组过滤：HAVING
-- 限制：LIMIT, OFFSET
-- 更多子查询类型：EXISTS, ANY, ALL
-- 联合：UNION, INTERSECT, EXCEPT
-- 窗口函数（PostgreSQL）
-- CTE（Common Table Expressions）
-- 更多数据类型和约束
-- 存储过程和触发器
-- 视图（VIEW）
```

#### 3. 性能对比

**插入性能**：

| 系统 | 插入1000条记录 | 说明 |
|------|---------------|------|
| 本系统 | ~100ms | 顺序追加，无索引维护 |
| SQLite | ~50ms | 有索引维护，但优化良好 |
| MySQL | ~30ms | 批量插入优化 |
| PostgreSQL | ~40ms | 写入优化 |

**查询性能**：

| 系统 | 全表扫描1000条 | 索引查询 | 说明 |
|------|---------------|---------|------|
| 本系统 | ~50ms | N/A | 无索引，全表扫描 |
| SQLite | ~20ms | ~1ms | B树索引 |
| MySQL | ~15ms | ~0.5ms | B+树索引 |
| PostgreSQL | ~18ms | ~0.8ms | B树/GiST索引 |

**连接查询性能**：

| 系统 | 两表连接（各1000条） | 说明 |
|------|---------------------|------|
| 本系统 | ~5000ms | 嵌套循环，O(n×m) |
| SQLite | ~200ms | 查询优化器选择最优算法 |
| MySQL | ~150ms | 查询优化器 + 索引 |
| PostgreSQL | ~180ms | 查询优化器 + 多种连接算法 |

#### 4. 适用场景对比

**本系统适用场景**：
- ✅ 教学和学习数据库原理
- ✅ 小型单用户应用
- ✅ 原型开发
- ✅ 理解数据库底层实现
- ❌ 生产环境（功能不完整）
- ❌ 高并发应用（无并发控制）
- ❌ 大数据量（无索引优化）

**SQLite适用场景**：
- ✅ 嵌入式应用
- ✅ 移动应用
- ✅ 小型Web应用
- ✅ 桌面应用
- ✅ 单用户应用
- ❌ 高并发写入
- ❌ 需要网络访问

**MySQL适用场景**：
- ✅ Web应用
- ✅ 中小型企业应用
- ✅ 高并发读取
- ✅ 需要网络访问
- ✅ 需要用户权限管理
- ❌ 复杂分析查询（不如PostgreSQL）

**PostgreSQL适用场景**：
- ✅ 复杂查询和分析
- ✅ 大数据应用
- ✅ 需要高级特性（JSON、数组、全文搜索）
- ✅ 企业级应用
- ✅ 需要ACID严格保证
- ❌ 简单应用（过于复杂）

### 代码复杂度对比

#### 本系统代码量

```
核心模块：
- 数据模型：~500行
- 存储层：~600行
- SQL解析：~800行
- 执行引擎：~700行
- 用户界面：~900行
总计：~3500行
```

#### SQLite代码量

```
SQLite核心：~150,000行
- 解析器：~30,000行
- 执行引擎：~40,000行
- B树实现：~20,000行
- 其他：~60,000行
```

**对比**：
- 本系统代码量约为SQLite的2.3%
- 本系统功能约为SQLite的10-15%
- 本系统更适合学习和理解

### 学习曲线对比

**本系统**：
- 学习难度：⭐⭐（简单）
- 需要理解：文件I/O、序列化、基本算法
- 适合：初学者理解数据库原理

**SQLite**：
- 学习难度：⭐⭐⭐（中等）
- 需要理解：SQL语法、数据库设计
- 适合：有一定基础的开发者

**MySQL/PostgreSQL**：
- 学习难度：⭐⭐⭐⭐（较难）
- 需要理解：SQL、数据库设计、性能优化、服务器管理
- 适合：专业数据库管理员

### 扩展性对比

**本系统扩展方向**：
1. 添加索引（B+树）：~2000行代码
2. 实现事务：~1500行代码
3. 添加聚合函数：~500行代码
4. 优化查询：~1000行代码

**SQLite扩展**：
- 通过C扩展接口
- 需要理解SQLite内部结构
- 相对复杂

**MySQL/PostgreSQL扩展**：
- 通过插件机制
- 需要深入理解数据库内核
- 非常复杂

### 实际应用建议

#### 何时使用本系统

1. **教学场景**：
   - 数据库课程实验
   - 理解数据库原理
   - 学习文件存储和序列化

2. **原型开发**：
   - 快速验证想法
   - 不需要复杂功能
   - 单用户使用

3. **学习项目**：
   - 理解SQL解析
   - 学习执行引擎
   - 实践软件工程

#### 何时使用SQLite

1. **嵌入式应用**：
   - 移动应用
   - 桌面应用
   - 不需要网络访问

2. **小型Web应用**：
   - 个人博客
   - 小型CMS
   - 单用户应用

#### 何时使用MySQL

1. **Web应用**：
   - 中小型网站
   - 需要网络访问
   - 需要用户权限

2. **企业应用**：
   - 需要高可用
   - 需要复制
   - 需要集群

#### 何时使用PostgreSQL

1. **复杂应用**：
   - 需要复杂查询
   - 需要高级数据类型
   - 需要严格ACID

2. **数据分析**：
   - 数据仓库
   - 商业智能
   - 复杂报表

---

## 性能优化实践

### 当前实现的性能特点

#### 优势

1. **简单高效**：
   - 无索引维护开销
   - 无事务日志开销
   - 无锁竞争

2. **内存占用小**：
   - 按需读取记录
   - 不缓存所有数据
   - 适合内存受限环境

#### 劣势

1. **查询性能**：
   - 全表扫描：O(n)
   - 无索引加速
   - 连接操作效率低

2. **更新操作**：
   - 需要读取所有记录
   - 文件重写开销大

### 优化建议

#### 1. 实现简单索引

```java
// 哈希索引实现
public class HashIndex {
    private Map<Object, List<Long>> index = new HashMap<>();
    
    public void buildIndex(Table table, String columnName) {
        List<Record> records = readAllRecords(table);
        for (int i = 0; i < records.size(); i++) {
            Record record = records.get(i);
            Object key = record.getValue(table, columnName);
            long offset = calculateOffset(i, table);
            index.computeIfAbsent(key, k -> new ArrayList<>()).add(offset);
        }
    }
    
    public List<Long> lookup(Object key) {
        return index.getOrDefault(key, Collections.emptyList());
    }
}
```

**性能提升**：
- 等值查询：O(1) → O(log n)
- 范围查询：仍需要全表扫描

#### 2. 批量操作优化

```java
// 批量插入
public void batchInsert(String tableName, List<Record> records) {
    Table table = getTable(tableName);
    try (RandomAccessFile raf = new RandomAccessFile(datFilePath, "rw")) {
        raf.seek(raf.length());  // 定位到文件末尾
        for (Record record : records) {
            writeRecord(raf, record, table);
        }
    }
}
```

**性能提升**：
- 减少文件打开/关闭次数
- 减少系统调用
- 提升约30-50%的插入性能

#### 3. 查询结果缓存

```java
public class QueryCache {
    private Map<String, QueryResult> cache = new LRUCache<>(100);
    
    public QueryResult getCached(String sql, Table table) {
        String key = sql + table.getName() + table.getLastModified();
        return cache.get(key);
    }
    
    public void putCache(String sql, Table table, QueryResult result) {
        String key = sql + table.getName() + table.getLastModified();
        cache.put(key, result);
    }
}
```

**性能提升**：
- 重复查询：O(1)
- 适合读多写少的场景

---

## 总结对比

### 本系统的定位

**核心价值**：
- **教育价值**：帮助理解数据库系统原理
- **实践价值**：完整的实现示例
- **学习价值**：从零开始构建数据库系统

**适用人群**：
- 数据库课程学生
- 想理解数据库原理的开发者
- 需要简单存储方案的原型项目

**不适合的场景**：
- 生产环境应用
- 高并发系统
- 大数据量处理
- 需要复杂SQL功能

### 选择建议

| 需求 | 推荐系统 |
|------|---------|
| 学习数据库原理 | 本系统 |
| 小型单用户应用 | SQLite |
| Web应用（中小型） | MySQL |
| 复杂查询和分析 | PostgreSQL |
| 嵌入式应用 | SQLite |
| 企业级应用 | MySQL/PostgreSQL |

---

*文档版本：1.4*  
*最后更新：2024年*

## 更新日志

### v1.4 (2024)
- ✅ 新增CREATE INDEX和CREATE UNIQUE INDEX支持
- ✅ 新增CREATE USER和DROP USER支持（DCL）
- ✅ 新增GRANT和REVOKE权限管理支持
- ✅ 新增BEGIN TRANSACTION、COMMIT、ROLLBACK事务支持
- ✅ 新增ORDER BY排序支持（多列、ASC/DESC）
- ✅ 新增IN (SELECT ...)嵌套查询支持
- ✅ 新增用户管理界面（查看用户列表和权限）
- ✅ 新增数据备份和恢复功能
- ✅ 改进WHERE条件处理（支持IN操作符和嵌套查询）

### v1.3 (2024)
- ✅ 新增DOUBLE数据类型支持
- ✅ 新增LIKE操作符支持（模式匹配）
- ✅ 新增聚合函数支持（COUNT, SUM, AVG, MAX, MIN）
- ✅ 新增GROUP BY子句支持
- ✅ 新增列别名（AS）支持
- ✅ 新增WHERE条件括号嵌套支持
- ✅ 新增UPDATE表达式支持（如 `age = age + 1`）
- ✅ 改进表别名处理（在JOIN查询中）

### v1.2 (2024)
- 初始版本发布
- 基础DDL和DML支持
- 单表和多表查询
- 自定义文件格式

