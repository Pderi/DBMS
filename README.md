# DBMS - 数据库管理系统

一个基于Java实现的简单数据库管理系统，支持自定义文件格式存储表结构和数据，以及基本的SQL操作。

## 功能特性

### 1. 数据库表结构管理
- ✅ 创建表（CREATE TABLE）
- ✅ 查看表结构
- ✅ 修改表结构（ALTER TABLE）
  - 添加字段（ADD COLUMN）
  - 删除字段（DROP COLUMN）
  - 修改字段类型（MODIFY COLUMN）
  - 重命名字段（RENAME COLUMN）
- ✅ 删除表（DROP TABLE）
- ✅ 重命名表（RENAME TABLE）

### 2. 数据操作
- ✅ 插入记录（INSERT）
- ✅ 更新记录（UPDATE）
- ✅ 删除记录（DELETE）
- ✅ 查询记录（SELECT）

### 3. 查询功能
- ✅ 单表查询
- ✅ 多表连接查询（JOIN）
- ✅ WHERE条件过滤
- ✅ 字段投影

### 4. 文件存储
- ✅ 自定义.dbf文件格式存储表结构
- ✅ 自定义.dat文件格式存储数据记录
- ✅ 支持在一个数据库文件中存储多张表

### 5. 用户界面
- ✅ 基于Swing的图形界面
- ✅ SQL编辑器
- ✅ 表结构可视化
- ✅ 数据表格展示
- ✅ 查询结果展示

## 项目结构

```
DBMS/
├── src/main/java/com/dbms/
│   ├── model/              # 数据模型
│   │   ├── Field.java      # 字段定义
│   │   ├── FieldType.java  # 字段类型枚举
│   │   ├── Table.java      # 表结构
│   │   ├── Record.java     # 记录
│   │   └── Database.java   # 数据库
│   ├── storage/            # 存储层
│   │   ├── DBFFileManager.java    # .dbf文件管理
│   │   ├── DATFileManager.java    # .dat文件管理
│   │   ├── BinarySerializer.java  # 二进制序列化
│   │   └── FileFormat.java         # 文件格式定义
│   ├── parser/             # SQL解析
│   │   ├── SQLLexer.java   # 词法分析器
│   │   └── SQLParser.java  # 语法分析器
│   ├── engine/             # 执行引擎
│   │   ├── DDLExecutor.java    # DDL执行器
│   │   ├── DMLExecutor.java    # DML执行器
│   │   ├── QueryExecutor.java  # 查询执行器
│   │   └── SQLExecutor.java    # SQL执行器
│   ├── util/               # 工具类
│   │   ├── TypeConverter.java  # 类型转换
│   │   ├── Validator.java      # 数据验证
│   │   ├── DBMSException.java  # 异常类
│   │   └── SQLException.java   # SQL异常
│   └── ui/                 # 用户界面
│       └── MainFrame.java  # 主窗口
└── README.md
```

## 使用方法

### 编译和运行

1. **编译项目**
   ```bash
   javac -d bin -sourcepath src/main/java src/main/java/com/dbms/**/*.java
   ```

2. **运行程序**
   ```bash
   java -cp bin com.dbms.ui.MainFrame
   ```

### SQL语法示例

#### 创建表
```sql
CREATE TABLE students (
    id INT PRIMARY KEY NOT NULL,
    name VARCHAR(50) NOT NULL,
    age INT,
    email VARCHAR(100)
)
```

#### 插入数据
```sql
INSERT INTO students VALUES (1, 'Alice', 20, 'alice@example.com')
INSERT INTO students (id, name, age) VALUES (2, 'Bob', 21)
```

#### 查询数据
```sql
SELECT * FROM students
SELECT name, age FROM students WHERE age > 20
```

#### 更新数据
```sql
UPDATE students SET age = 22 WHERE id = 1
```

#### 删除数据
```sql
DELETE FROM students WHERE id = 2
```

#### 修改表结构
```sql
ALTER TABLE students ADD COLUMN phone VARCHAR(20)
ALTER TABLE students DROP COLUMN phone
ALTER TABLE students MODIFY COLUMN name VARCHAR(100)
ALTER TABLE students RENAME COLUMN email TO email_address
```

#### 删除表
```sql
DROP TABLE students
```

#### 重命名表
```sql
RENAME TABLE students TO student_info
```

#### 多表连接查询
```sql
SELECT * FROM students 
INNER JOIN courses ON students.id = courses.student_id
WHERE students.age > 20
```

## 文件格式说明

### .dbf文件（表结构文件）
- **文件头**（512字节）：
  - 魔数（4字节）：0x44424D53 ("DBMS")
  - 版本号（4字节）
  - 表数量（4字节）
  - 表索引表（表名 + 偏移量）
- **表结构区**：
  - 表名
  - 字段数量
  - 字段定义数组（字段名、类型、长度、是否主键、是否允许NULL）

### .dat文件（数据文件）
- 每条记录包含：
  - 记录状态（4字节）：0=有效，1=已删除
  - 字段值（根据字段类型存储）

## 支持的数据类型

- **INT**: 整数（4字节）
- **FLOAT**: 浮点数（8字节）
- **VARCHAR**: 变长字符串
- **CHAR**: 定长字符串
- **DATE**: 日期（作为字符串存储）

## 注意事项

1. 表名和字段名只能包含字母、数字和下划线，且不能以数字开头
2. VARCHAR类型必须指定长度
3. 主键字段不能为NULL
4. 多表连接查询目前仅支持两表连接
5. 删除操作是逻辑删除（标记为已删除），不会物理删除记录

## 开发环境

- Java 8+
- Swing GUI框架

## 许可证

本项目为教学实验项目。

